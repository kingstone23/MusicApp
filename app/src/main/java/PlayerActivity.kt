package com.example.musicapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.example.musicapp.databinding.ActivityPlayerBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Transaction
import com.google.firebase.database.MutableData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false

    // Runnable cập nhật SeekBar (Phiên bản An toàn)
    private val updateProgressAction = object : Runnable {
        override fun run() {
            // Kiểm tra: Nếu Activity đã đóng hoặc Player null thì DỪNG NGAY
            if (isFinishing || isDestroyed || player == null) {
                handler.removeCallbacks(this)
                return
            }

            try {
                if (player!!.isPlaying && !isUserSeeking) {
                    val currentPosition = player!!.currentPosition
                    val totalDuration = player!!.duration

                    if (totalDuration > 0) {
                        binding.seekBar.max = totalDuration.toInt()
                        binding.seekBar.progress = currentPosition.toInt()
                    }

                    binding.tvCurrentTime.text = formatTime(currentPosition)
                    binding.tvTotalTime.text = formatTime(totalDuration)
                }
                // Lặp lại sau 1 giây
                handler.postDelayed(this, 1000)
            } catch (e: Exception) {
                Log.e("PlayerActivity", "Lỗi cập nhật SeekBar: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val audioUrl = intent.getStringExtra("audioUrl") ?: ""
        val title = intent.getStringExtra("title")
        val artist = intent.getStringExtra("artist")
        val cover = intent.getStringExtra("cover")
        val songId = intent.getStringExtra("songId")

        binding.tvTitle.text = title
        binding.tvArtist.text = artist
        Glide.with(this).load(cover).placeholder(R.mipmap.ic_launcher).into(binding.ivCover)

        initializePlayer(audioUrl)
        if (!songId.isNullOrEmpty()) increaseViewCount(songId)

        // Xử lý nút bấm
        binding.btnPlayPause.setOnClickListener {
            player?.let { p ->
                if (p.isPlaying) {
                    p.pause()
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                    handler.removeCallbacks(updateProgressAction)
                } else {
                    p.play()
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                    handler.post(updateProgressAction)
                }
            }
        }

        binding.btnRewind.setOnClickListener {
            player?.let { p -> p.seekTo(maxOf(p.currentPosition - 5000, 0)) }
        }

        binding.btnForward.setOnClickListener {
            player?.let { p -> p.seekTo(minOf(p.currentPosition + 5000, p.duration)) }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.tvCurrentTime.text = formatTime(progress.toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isUserSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                player?.seekTo(seekBar?.progress?.toLong() ?: 0)
            }
        })
    }

    private fun initializePlayer(url: String) {
        try {
            player = ExoPlayer.Builder(this).build()
            val mediaItem = MediaItem.fromUri(url)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true

            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && player != null) {
                        binding.tvTotalTime.text = formatTime(player!!.duration)
                        binding.seekBar.max = player!!.duration.toInt()
                        handler.post(updateProgressAction)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                        handler.post(updateProgressAction)
                    } else {
                        binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        handler.removeCallbacks(updateProgressAction)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("PlayerError", "Lỗi phát nhạc: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khởi tạo player: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun increaseViewCount(songId: String) {
        val songRef = FirebaseDatabase.getInstance().getReference("Songs").child(songId).child("views")
        songRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentViews = currentData.getValue(Long::class.java) ?: 0
                currentData.value = currentViews + 1
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
        })
    }

    // --- QUAN TRỌNG NHẤT: DỌN DẸP KHI THOÁT APP ---
    override fun onStop() {
        super.onStop()
        // Dừng nhạc khi thoát màn hình (tuỳ chọn)
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 1. Dừng mọi cập nhật giao diện
        handler.removeCallbacksAndMessages(null)

        // 2. Giải phóng Player an toàn
        player?.release()
        player = null
    }
}