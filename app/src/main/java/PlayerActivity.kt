package com.example.musicapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.musicapp.databinding.ActivityPlayerBinding
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Transaction
import com.google.firebase.database.MutableData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError


class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        player = ExoPlayer.Builder(this).build()
        // Nhận dữ liệu từ Intent
        val audioUrl = intent.getStringExtra("audioUrl")
        val title = intent.getStringExtra("title")
        val artist = intent.getStringExtra("artist")
        val cover = intent.getStringExtra("cover")
        val songId = intent.getStringExtra("songId")

        if (!songId.isNullOrEmpty()) {
            increaseViewCount(songId)
        }


        // Kiểm tra dữ liệu
        if (audioUrl.isNullOrEmpty() || title.isNullOrEmpty() || artist.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "Không có dữ liệu bài hát", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("PlayerActivity", "Audio URL: $audioUrl")

        // Set thông tin bài hát lên UI
        binding.tvTitle.text = title
        binding.tvArtist.text = artist
        Glide.with(this).load(cover).into(binding.ivCover)

        // Khởi tạo ExoPlayer
        player = ExoPlayer.Builder(this).build().apply {
            binding.playerView.player = this
            val mediaItem = MediaItem.fromUri(audioUrl)
            setMediaItem(mediaItem)
            playWhenReady = true
            prepare()

            // Bắt lỗi phát nhạc
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(this@PlayerActivity, "Lỗi phát nhạc: ${error.message}", Toast.LENGTH_LONG).show()
                    Log.e("PlayerActivity", "Playback error: ${error.message}")
                }
            })
        }
    }

    private fun increaseViewCount(songId: String) {
        val songRef = FirebaseDatabase.getInstance()
            .getReference("Songs")
            .child(songId)
            .child("views")

        songRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentViews = currentData.getValue(Long::class.java) ?: 0
                currentData.value = currentViews + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("PlayerActivity", "Tăng view lỗi: ${error.message}")
                }
            }
        })
    }


    override fun onStart() {
        super.onStart()
        player?.playWhenReady = true
    }

    override fun onResume() {
        super.onResume()
        // Không cần gọi lại playWhenReady ở đây nếu đã gọi trong onStart
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

}