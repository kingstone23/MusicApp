package com.example.musicapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.example.musicapp.databinding.ActivityPlayerBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false

    // Runnable cập nhật SeekBar an toàn
    private val updateProgressAction = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed || player == null) {
                handler.removeCallbacks(this)
                return
            }

            try {
                if (player!!.isPlaying && !isUserSeeking) {
                    val current = player!!.currentPosition
                    val total = player!!.duration

                    if (total > 0) {
                        binding.seekBar.max = total.toInt()
                        binding.seekBar.progress = current.toInt()
                    }
                    binding.tvCurrentTime.text = formatTime(current)
                    binding.tvTotalTime.text = formatTime(total)
                }
                handler.postDelayed(this, 1000)
            } catch (e: Exception) {
                Log.e("PlayerActivity", "Lỗi SeekBar: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Lấy dữ liệu từ Intent
        val audioUrl = intent.getStringExtra("audioUrl") ?: ""
        val title = intent.getStringExtra("title") ?: "Unknown Song"
        val artist = intent.getStringExtra("artist") ?: "Unknown Artist"
        val cover = intent.getStringExtra("cover")
        val songId = intent.getStringExtra("songId")

        // 2. Gán dữ liệu lên giao diện mới
        binding.tvTitle.text = title
        binding.tvTitle.isSelected = true // [QUAN TRỌNG] Để chữ chạy (Marquee)

        binding.tvArtist.text = artist

        // Load ảnh vào ivDisc (bên trong CardView)
        Glide.with(this)
            .load(cover)
            .placeholder(R.mipmap.ic_launcher)
            .into(binding.ivDisc)

        // 3. Khởi tạo Player
        initializePlayer(audioUrl)
        if (!songId.isNullOrEmpty()) increaseViewCount(songId)

        // 4. Xử lý sự kiện nút bấm
        setupClickListeners()
        setupSeekBar()
    }

    private fun setupClickListeners() {
        // Nút Play/Pause (FloatingActionButton)
        binding.btnPlay.setOnClickListener {
            player?.let { p ->
                if (p.isPlaying) {
                    p.pause()
                } else {
                    p.play()
                }
                updatePlayPauseIcon(p.isPlaying)
            }
        }

        // Nút Prev (Tạm thời là Tua lại 10s)
        binding.btnPrev.setOnClickListener {
            player?.let { p ->
                val newPos = maxOf(p.currentPosition - 10000, 0)
                p.seekTo(newPos)
            }
        }

        // Nút Next (Tạm thời là Tua tới 10s)
        binding.btnNext.setOnClickListener {
            player?.let { p ->
                val newPos = minOf(p.currentPosition + 10000, p.duration)
                p.seekTo(newPos)
            }
        }
    }

    private fun setupSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                player?.seekTo(seekBar?.progress?.toLong() ?: 0)
            }
        })
    }

    private fun initializePlayer(url: String) {
        try {
            // Cấu hình AudioAttributes để tự dừng khi rút tai nghe / có cuộc gọi
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            player = ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true) // handleAudioBecomingNoisy = true
                .build()

            val mediaItem = MediaItem.fromUri(url)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true // Tự động phát khi sẵn sàng

            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && player != null) {
                        binding.tvTotalTime.text = formatTime(player!!.duration)
                        binding.seekBar.max = player!!.duration.toInt()
                        handler.post(updateProgressAction)
                    } else if (playbackState == Player.STATE_ENDED) {
                        binding.btnPlay.setImageResource(android.R.drawable.ic_media_play)
                        handler.removeCallbacks(updateProgressAction)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseIcon(isPlaying)
                    if (isPlaying) {
                        handler.post(updateProgressAction)
                    } else {
                        handler.removeCallbacks(updateProgressAction)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("PlayerError", "Lỗi phát nhạc: ${error.message}")
                    Toast.makeText(this@PlayerActivity, "Lỗi phát nhạc", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khởi tạo player: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        if (isPlaying) {
            binding.btnPlay.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            binding.btnPlay.setImageResource(android.R.drawable.ic_media_play)
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

    override fun onStop() {
        super.onStop()
        // Chỉ Pause chứ không release, để khi quay lại app vẫn phát tiếp được nếu muốn
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
    }
}