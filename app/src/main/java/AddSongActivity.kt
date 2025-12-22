package com.example.musicapp

import com.example.musicapp.Song
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class AddSongActivity : AppCompatActivity() {

    // [QUAN TRỌNG] Thay link này bằng link GitHub Pages thật của bạn
    private val GITHUB_BASE_URL = "https://kingstone23.github.io/mp3/"

    private lateinit var edtTitle: EditText
    private lateinit var edtArtist: EditText
    private lateinit var edtAudioUrl: EditText
    private lateinit var edtCover: EditText
    private lateinit var edtGenre: EditText // 1. Khai báo biến Genre
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_song)

        // Ánh xạ View
        edtTitle = findViewById(R.id.edtTitle)
        edtArtist = findViewById(R.id.edtArtist)
        edtAudioUrl = findViewById(R.id.edtAudioUrl)
        edtCover = findViewById(R.id.edtCover)
        edtGenre = findViewById(R.id.edtGenre) // 2. Ánh xạ ID từ XML (Nhớ thêm vào XML nhé)
        btnSubmit = findViewById(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val artist = edtArtist.text.toString().trim()
            val genre = edtGenre.text.toString().trim() // 3. Lấy dữ liệu Genre

            // ==================================================================
            // XỬ LÝ URL NHẠC (AUDIO) - Logic thông minh
            // ==================================================================
            var audioRaw = edtAudioUrl.text.toString().trim()
            audioRaw = audioRaw.replace("\"", "").replace("'", "") // Xóa ngoặc kép
            if (audioRaw.contains("\\")) audioRaw = audioRaw.substringAfterLast("\\")
            if (audioRaw.contains("/")) audioRaw = audioRaw.substringAfterLast("/")

            // Tự thêm đuôi .mp3
            if (audioRaw.isNotEmpty() && !audioRaw.endsWith(".mp3", ignoreCase = true)) {
                audioRaw += ".mp3"
            }
            val cleanAudioName = audioRaw.replace(" ", "%20")
            val finalAudioUrl = if (cleanAudioName.isEmpty()) "" else GITHUB_BASE_URL + cleanAudioName


            // ==================================================================
            // XỬ LÝ URL ẢNH (COVER)
            // ==================================================================
            var coverRaw = edtCover.text.toString().trim()
            coverRaw = coverRaw.replace("\"", "").replace("'", "")
            if (coverRaw.contains("\\")) coverRaw = coverRaw.substringAfterLast("\\")
            if (coverRaw.contains("/")) coverRaw = coverRaw.substringAfterLast("/")

            // Tự thêm đuôi .jpg
            if (coverRaw.isNotEmpty() && !coverRaw.contains(".")) {
                coverRaw += ".jpg"
            }
            val cleanCoverName = coverRaw.replace(" ", "%20")
            val finalCoverUrl = if (cleanCoverName.isEmpty()) "" else GITHUB_BASE_URL + cleanCoverName


            // ==================================================================
            // KIỂM TRA ĐẦU VÀO (Bao gồm cả Genre)
            // ==================================================================
            if (title.isEmpty() || artist.isEmpty() || finalAudioUrl.isEmpty() || finalCoverUrl.isEmpty() || genre.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin (cả thể loại)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ==================================================================
            // KIỂM TRA LINK & LƯU FIREBASE
            // ==================================================================
            val mediaPlayer = MediaPlayer()
            try {
                mediaPlayer.setDataSource(finalAudioUrl)

                mediaPlayer.setOnPreparedListener {
                    mediaPlayer.release()

                    // 4. Tạo đối tượng Song có chứa Genre
                    // Đảm bảo Data Class Song của bạn đã có trường 'genre'
                    val song = Song(title, artist, finalAudioUrl, finalCoverUrl, genre)

                    val songId = FirebaseDatabase.getInstance().getReference("Songs").push().key

                    if (songId != null) {
                        FirebaseDatabase.getInstance().getReference("Songs").child(songId)
                            .setValue(song)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Đã thêm: $title ($genre)", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Lỗi Firebase: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Không thể tạo ID bài hát", Toast.LENGTH_SHORT).show()
                    }
                }

                mediaPlayer.setOnErrorListener { _, _, _ ->
                    mediaPlayer.release()
                    Toast.makeText(this, "Không tìm thấy file nhạc trên GitHub: $cleanAudioName", Toast.LENGTH_LONG).show()
                    true
                }

                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                mediaPlayer.release()
                Toast.makeText(this, "Lỗi hệ thống: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}