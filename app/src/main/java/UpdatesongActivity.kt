package com.example.musicapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class UpdateSongActivity : AppCompatActivity() {

    // [QUAN TRỌNG] Thay bằng Link GitHub Pages của bạn
    private val GITHUB_BASE_URL = "https://kingstone23.github.io/mp3/"

    private lateinit var etSongTitle: EditText
    private lateinit var etArtistName: EditText
    private lateinit var etGenre: EditText // [MỚI]
    private lateinit var etCoverUrl: EditText
    private lateinit var etAudioUrl: EditText
    private lateinit var btnSaveChanges: Button

    private var songId: String? = null
    private val songRef = FirebaseDatabase.getInstance().getReference("Songs")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_song)

        // 1. Ánh xạ View
        etSongTitle = findViewById(R.id.etSongTitle)
        etArtistName = findViewById(R.id.etArtistName)
        etGenre = findViewById(R.id.etGenre) // [MỚI]
        etCoverUrl = findViewById(R.id.etCoverUrl)
        etAudioUrl = findViewById(R.id.etAudioUrl)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        // 2. Nhận dữ liệu từ Intent (Adapter gửi sang)
        songId = intent.getStringExtra("id")
        val title = intent.getStringExtra("title")
        val artist = intent.getStringExtra("artist")
        val genre = intent.getStringExtra("genre") // [MỚI] Nhận genre cũ
        val cover = intent.getStringExtra("cover")
        val audioUrl = intent.getStringExtra("audioUrl")

        if (songId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID bài hát", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 3. Hiển thị dữ liệu cũ lên màn hình
        etSongTitle.setText(title)
        etArtistName.setText(artist)
        etGenre.setText(genre) // [MỚI]
        etCoverUrl.setText(cover)
        etAudioUrl.setText(audioUrl)

        btnSaveChanges.setOnClickListener {
            updateSong()
        }
    }

    private fun updateSong() {
        val title = etSongTitle.text.toString().trim()
        val artist = etArtistName.text.toString().trim()
        val genre = etGenre.text.toString().trim() // [MỚI]

        // ---------------------------------------------------------
        // XỬ LÝ URL NHẠC (AUDIO) - Logic thông minh
        // ---------------------------------------------------------
        var audioRaw = etAudioUrl.text.toString().trim()

        // Xóa dấu ngoặc kép/đơn
        audioRaw = audioRaw.replace("\"", "").replace("'", "")

        // Lấy tên file (Hoạt động cho cả Link cũ và Path máy tính mới)
        if (audioRaw.contains("\\")) audioRaw = audioRaw.substringAfterLast("\\")
        if (audioRaw.contains("/")) audioRaw = audioRaw.substringAfterLast("/")

        // Tự thêm .mp3
        if (audioRaw.isNotEmpty() && !audioRaw.endsWith(".mp3", ignoreCase = true)) {
            audioRaw += ".mp3"
        }
        val cleanAudioName = audioRaw.replace(" ", "%20")
        val finalAudioUrl = if (cleanAudioName.isEmpty()) "" else GITHUB_BASE_URL + cleanAudioName


        // ---------------------------------------------------------
        // XỬ LÝ URL ẢNH (COVER)
        // ---------------------------------------------------------
        var coverRaw = etCoverUrl.text.toString().trim()

        coverRaw = coverRaw.replace("\"", "").replace("'", "")

        if (coverRaw.contains("\\")) coverRaw = coverRaw.substringAfterLast("\\")
        if (coverRaw.contains("/")) coverRaw = coverRaw.substringAfterLast("/")

        // Tự thêm .jpg
        if (coverRaw.isNotEmpty() && !coverRaw.contains(".")) {
            coverRaw += ".jpg"
        }
        val cleanCoverName = coverRaw.replace(" ", "%20")
        val finalCoverUrl = if (cleanCoverName.isEmpty()) "" else GITHUB_BASE_URL + cleanCoverName


        // ---------------------------------------------------------
        // KIỂM TRA VÀ LƯU
        // ---------------------------------------------------------
        if (title.isEmpty() || artist.isEmpty() || finalCoverUrl.isEmpty() || finalAudioUrl.isEmpty() || genre.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedData = mapOf(
            "title" to title,
            "artist" to artist,
            "genre" to genre,        // [MỚI] Cập nhật genre
            "cover" to finalCoverUrl,
            "audioUrl" to finalAudioUrl
        )

        songRef.child(songId!!).updateChildren(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Đã cập nhật: $title", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Cập nhật thất bại: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}