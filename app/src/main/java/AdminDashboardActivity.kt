package com.example.musicapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class AdminDashboardActivity : AppCompatActivity() {

    // --- CÁC BIẾN GỐC (ĐÃ BỎ NHỮNG BIẾN KHÔNG CẦN THIẾT) ---
    private lateinit var btnAddSong: Button
    private lateinit var btnDeleteUser: Button
    private lateinit var btnUpdateSong: Button
    private lateinit var recyclerAllSongs: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private val songList = mutableListOf<Song>()
    private val songRef = FirebaseDatabase.getInstance().getReference("Songs")

// --- SỬA LỖI: TÁCH THÀNH 2 LAUNCHER RIÊNG BIỆT ---

    // Launcher 1: Mở màn hình Update và nhận kết quả để REFRESH
    private val updateAndRefreshLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Danh sách đã được cập nhật.", Toast.LENGTH_SHORT).show()
            loadAllSongs()
        }
    }

    // Launcher 2: Mở màn hình Select và nhận kết quả là bài hát đã chọn
    // Launcher 2: Mở màn hình Select và nhận kết quả là bài hát đã chọn
    private val selectSongLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            // <<< DÒNG SỬA Ở ĐÂY >>>
            val selectedSong = result.data?.getParcelableExtra<Song>("SELECTED_SONG")

            if (selectedSong != null) {
                // Khi đã có bài hát, tạo Intent để mở màn hình Update

                // (Bạn có thể kiểm tra ID ở đây)
                // Toast.makeText(this, "Đã nhận ID: ${selectedSong.id}", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, UpdateSongActivity::class.java).apply {
                    putExtra("id", selectedSong.id)
                    putExtra("title", selectedSong.title)
                    putExtra("artist", selectedSong.artist)
                    putExtra("cover", selectedSong.cover)
                    putExtra("audioUrl", selectedSong.audioUrl)
                    putExtra("IS_FROM_ADMIN_DASHBOARD", true)
                }
                // Dùng launcher 1 để mở màn hình update
                updateAndRefreshLauncher.launch(intent)
            } else {
                // Nếu selectedSong là null, nó sẽ báo lỗi ở đây
                Toast.makeText(this, "Lỗi: Không nhận được bài hát", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // --- CODE GỐC CỦA BẠN (GIỮ NGUYÊN) ---
        val username: String = intent.getStringExtra("username") ?: "Admin"
        findViewById<TextView>(R.id.tvWelcome).text = "Xin chào $username"

        btnAddSong = findViewById(R.id.btnAddSong)
        btnDeleteUser = findViewById(R.id.btnDeleteUser)
        recyclerAllSongs = findViewById(R.id.recyclerAllSongs)
        btnUpdateSong = findViewById(R.id.btnUpdateSong)

        btnAddSong.setOnClickListener {
            startActivity(Intent(this, AddSongActivity::class.java))
        }

        btnDeleteUser.setOnClickListener {
            startActivity(Intent(this, DeleteUserActivity::class.java))
        }

        // --- LOGIC NÚT UPDATE ĐÃ ĐƯỢC ĐƠN GIẢN HÓA ---
        btnUpdateSong.setOnClickListener {
            // Luôn mở màn hình chọn bài hát
            val intent = Intent(this, SelectSongActivity::class.java)
            selectSongLauncher.launch(intent) // Dùng launcher 2
        }

        setupRecyclerView()
        loadAllSongs()
    }

    private fun setupRecyclerView() {
        // Hàm này của bạn đã đúng, giữ nguyên
        songAdapter = SongAdapter(
            this, songList, object : SongAdapter.OnSongClickListener {
                override fun onAddClick(song: Song) {}
                override fun onRemoveClick(song: Song) {}
                override fun onUpdateClick(song: Song) {}

                override fun onDelete(song: Song) {
                    AlertDialog.Builder(this@AdminDashboardActivity)
                        .setTitle("Xác nhận xoá vĩnh viễn")
                        .setMessage("Bạn có chắc muốn XOÁ VĨNH VIỄN bài hát '${song.title}'?")
                        .setPositiveButton("Xoá vĩnh viễn") { _, _ ->
                            songRef.child(song.id).removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this@AdminDashboardActivity,
                                        "Đã xoá: ${song.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val currentPosition = songList.indexOf(song)
                                    if (currentPosition != -1) {
                                        songList.removeAt(currentPosition)
                                        songAdapter.notifyItemRemoved(currentPosition)
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this@AdminDashboardActivity,
                                        "Lỗi khi xoá bài hát",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .setNegativeButton("Huỷ", null)
                        .show()
                }

                override fun onSongClick(song: Song) {
                    val intent =
                        Intent(this@AdminDashboardActivity, PlayerActivity::class.java).apply {
                            putExtra("title", song.title)
                            putExtra("artist", song.artist)
                            putExtra("cover", song.cover)
                            putExtra("audioUrl", song.audioUrl)
                        }
                    startActivity(intent)
                }
            }, SongMode.REMOVE_ONLY,
            isAdmin = true
        )

        recyclerAllSongs.layoutManager = LinearLayoutManager(this)
        recyclerAllSongs.adapter = songAdapter
    }

    private fun loadAllSongs() {
        // Hàm này của bạn đã đúng, giữ nguyên
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