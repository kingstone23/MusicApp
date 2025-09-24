package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var btnAddSong: Button
    private lateinit var btnDeleteUser: Button
    private lateinit var recyclerAllSongs: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private val songList = mutableListOf<Song>()

    private val songRef = FirebaseDatabase.getInstance().getReference("Songs")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val username: String = intent.getStringExtra("username") ?: "Admin"
        findViewById<TextView>(R.id.tvWelcome).text = "Xin chào $username"

        btnAddSong = findViewById(R.id.btnAddSong)
        btnDeleteUser = findViewById(R.id.btnDeleteUser)
        recyclerAllSongs = findViewById(R.id.recyclerAllSongs)

        btnAddSong.setOnClickListener {
            startActivity(Intent(this, AddSongActivity::class.java))
        }

        btnDeleteUser.setOnClickListener {
            startActivity(Intent(this, DeleteUserActivity::class.java))
        }


        setupRecyclerView()
        loadAllSongs()
    }
    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(this, songList, object : SongAdapter.OnSongClickListener {
            override fun onAddClick(song: Song) {
                // Không dùng trong admin dashboard
            }

            override fun onRemoveClick(song: Song) {
                AlertDialog.Builder(this@AdminDashboardActivity)
                    .setTitle("Xác nhận xoá")
                    .setMessage("Bạn có chắc muốn xoá bài hát '${song.title}'?")
                    .setPositiveButton("Xoá") { _, _ ->
                        songRef.child(song.id).removeValue()
                            .addOnSuccessListener {
                                setResult(RESULT_OK)
                                Toast.makeText(this@AdminDashboardActivity, "Đã xoá: ${song.title}", Toast.LENGTH_SHORT).show()
                                songList.remove(song)
                                songAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@AdminDashboardActivity, "Lỗi khi xoá bài hát", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Huỷ", null)
                    .show()
            }
            override fun onDelete(song: Song) {
                // TODO: Xử lý xoá bài hát khỏi Firebase ở đây
            }

            override fun onSongClick(song: Song) {
                val intent = Intent(this@AdminDashboardActivity, PlayerActivity::class.java).apply {
                    putExtra("title", song.title)
                    putExtra("artist", song.artist)
                    putExtra("cover", song.cover)
                    putExtra("audioUrl", song.audioUrl)

                }
                startActivity(intent)
            }
        }, SongMode.REMOVE_ONLY,
            isAdmin = false)



        recyclerAllSongs.layoutManager = LinearLayoutManager(this)
        recyclerAllSongs.adapter = songAdapter
    }

    private fun loadAllSongs() {
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