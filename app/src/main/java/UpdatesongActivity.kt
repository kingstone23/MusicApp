package com.example.musicapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class UpdateSongActivity : AppCompatActivity() {

    // BƯỚC 1: KHAI BÁO CÁC BIẾN Ở ĐÂY
    // Dòng này rất có thể là dòng bạn đang bị thiếu.
    private lateinit var etSongTitle: EditText
    private lateinit var etArtistName: EditText
    private lateinit var etCoverUrl: EditText
    private lateinit var etAudioUrl: EditText
    private lateinit var btnSaveChanges: Button

    private var songId: String? = null
    private val songRef = FirebaseDatabase.getInstance().getReference("Songs")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ... (Phần code kiểm tra bảo mật của bạn)
        val isFromAdmin = intent.getBooleanExtra("IS_FROM_ADMIN_DASHBOARD", false)
        if (!isFromAdmin) {
            Toast.makeText(this, "Không có quyền truy cập.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_update_song)

        // BƯỚC 2: ÁNH XẠ (KHỞI TẠO) BIẾN VỚI VIEW TỪ XML
        // Phải đặt sau dòng setContentView()
        etSongTitle = findViewById(R.id.etSongTitle)
        etArtistName = findViewById(R.id.etArtistName)
        etCoverUrl = findViewById(R.id.etCoverUrl)
        etAudioUrl = findViewById(R.id.etAudioUrl)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        // Các đoạn code còn lại sẽ hoạt động bình thường sau khi biến đã được khai báo và ánh xạ
        songId = intent.getStringExtra("id")
        val title = intent.getStringExtra("title")
        val artist = intent.getStringExtra("artist")
        val cover = intent.getStringExtra("cover")
        val audioUrl = intent.getStringExtra("audioUrl")

        if (songId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID bài hát", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etSongTitle.setText(title) // Bây giờ dòng này sẽ không báo lỗi nữa
        etArtistName.setText(artist)
        etCoverUrl.setText(cover)
        etAudioUrl.setText(audioUrl)

        btnSaveChanges.setOnClickListener {
            updateSong()
        }
    }

    private fun updateSong() {
        val title = etSongTitle.text.toString().trim() // Và dòng này cũng vậy
        val artist = etArtistName.text.toString().trim()
        val coverUrl = etCoverUrl.text.toString().trim()
        val audioUrl = etAudioUrl.text.toString().trim()

        if (title.isEmpty() || artist.isEmpty() || coverUrl.isEmpty() || audioUrl.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedData = mapOf(
            "title" to title,
            "artist" to artist,
            "cover" to coverUrl,
            "audioUrl" to audioUrl
        )

        songRef.child(songId!!).updateChildren(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Cập nhật bài hát thành công", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Cập nhật thất bại: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}