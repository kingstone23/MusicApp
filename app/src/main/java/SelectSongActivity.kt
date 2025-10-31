package com.example.musicapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class SelectSongActivity : AppCompatActivity() {

    private lateinit var recyclerSelectSong: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private val songList = mutableListOf<Song>()
    private val songRef = FirebaseDatabase.getInstance().getReference("Songs")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_song) // Đảm bảo bạn có file layout này

        recyclerSelectSong = findViewById(R.id.recyclerSelectSong)
        setupRecyclerView()
        loadAllSongs()
    }

    private fun setupRecyclerView() {
        // Adapter ở đây không cần nút, chỉ cần sự kiện click vào item
        songAdapter = SongAdapter(this, songList, object : SongAdapter.OnSongClickListener {
            override fun onAddClick(song: Song) {}
            override fun onRemoveClick(song: Song) {}
            override fun onDelete(song: Song) {}
            override fun onUpdateClick(song: Song) {}

            // <<< LOGIC QUAN TRỌNG NHẤT NẰM Ở ĐÂY >>>
            override fun onSongClick(song: Song) {
                // 1. Tạo một Intent rỗng để chứa kết quả
                val resultIntent = Intent()

                // 2. Đặt dữ liệu cần trả về vào Intent
                resultIntent.putExtra("SELECTED_SONG", song)

                // 3. Đánh dấu kết quả là THÀNH CÔNG (RESULT_OK) và gửi kèm Intent dữ liệu
                setResult(Activity.RESULT_OK, resultIntent)

                // 4. Đóng màn hình này lại để quay về AdminDashboard
                finish()
            }
        }, SongMode.SELECT_ONLY, // Mode không quan trọng ở đây
            isAdmin = false)   // isAdmin = false để không hiện nút Delete

        recyclerSelectSong.layoutManager = LinearLayoutManager(this)
        recyclerSelectSong.adapter = songAdapter
    }

    private fun loadAllSongs() {
        // Copy hàm này từ AdminDashboardActivity sang là được
        songRef.get().addOnSuccessListener { snapshot ->
            val newList = mutableListOf<Song>()
            snapshot.children.forEach {
                val song = it.getValue(Song::class.java)
                song?.let { s ->
                    s.id = it.key ?: ""
                    newList.add(s)
                }
            }
            songList.clear()
            songList.addAll(newList)
            songAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(this, "Lỗi tải bài hát", Toast.LENGTH_SHORT).show()
        }
    }
}