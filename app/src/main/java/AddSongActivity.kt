package com.example.musicapp

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class AddSongActivity : AppCompatActivity() {

    private lateinit var edtTitle: EditText
    private lateinit var edtArtist: EditText
    private lateinit var edtAudioUrl: EditText
    private lateinit var edtCover: EditText
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_song)

        edtTitle = findViewById(R.id.edtTitle)
        edtArtist = findViewById(R.id.edtArtist)
        edtAudioUrl = findViewById(R.id.edtAudioUrl)
        edtCover = findViewById(R.id.edtCover)
        btnSubmit = findViewById(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val artist = edtArtist.text.toString().trim()
            val audioUrl = edtAudioUrl.text.toString().trim()
            val cover = edtCover.text.toString().trim()

            if (title.isEmpty() || artist.isEmpty() || audioUrl.isEmpty() || cover.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val mediaPlayer = MediaPlayer()
            try {
                mediaPlayer.setDataSource(audioUrl)

                mediaPlayer.setOnPreparedListener {
                    mediaPlayer.release()

                    val song = Song(title, artist, audioUrl, cover)
                    val songId = FirebaseDatabase.getInstance().getReference("Songs").push().key

                    if (songId != null) {
                        FirebaseDatabase.getInstance().getReference("Songs").child(songId)
                            .setValue(song)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Đã thêm bài hát", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Lỗi khi thêm bài hát: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Không thể tạo ID bài hát", Toast.LENGTH_SHORT).show()
                    }
                }

                mediaPlayer.setOnErrorListener { _, _, _ ->
                    mediaPlayer.release()
                    Toast.makeText(this, "Link audio không hợp lệ hoặc không phát được", Toast.LENGTH_SHORT).show()
                    true
                }

                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                mediaPlayer.release()
                Toast.makeText(this, "Lỗi khi kiểm tra link: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}