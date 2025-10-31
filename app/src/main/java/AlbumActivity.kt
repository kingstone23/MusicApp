package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AlbumActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private val albumSongs = mutableListOf<Song>()

    private lateinit var llLoginRegister: LinearLayout
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    private val database by lazy { FirebaseDatabase.getInstance().reference }
    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    private var albumListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        llLoginRegister = findViewById(R.id.llLoginRegister)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        adapter = SongAdapter(this, albumSongs, object : SongAdapter.OnSongClickListener {
            override fun onSongClick(song: Song) {
                val intent = Intent(this@AlbumActivity, PlayerActivity::class.java).apply {
                    putExtra("title", song.title)
                    putExtra("artist", song.artist)
                    putExtra("cover", song.cover)
                    putExtra("audioUrl", song.audioUrl)
                }
                startActivity(intent)
            }
            override fun onUpdateClick(song: Song) {
                // Chức năng này không dùng trong AlbumActivity, để trống.
            }
            override fun onAddClick(song: Song) {
                // Không dùng trong AlbumActivity
            }

            override fun onDelete(song: Song) {
                // Không dùng trong AlbumActivity
            }

            override fun onRemoveClick(song: Song) {
                val uid = userId ?: return
                val id = song.id
                if (id.isEmpty()) return

                database.child("Users").child(uid).child("albums").child(id).removeValue()
                    .addOnSuccessListener {
                        song.isInAlbum = false
                        albumSongs.remove(song)
                        Toast.makeText(this@AlbumActivity, "After remove: ${albumSongs}", Toast.LENGTH_SHORT).show()

                        adapter.updateSongs(albumSongs)
                        Toast.makeText(this@AlbumActivity, "After update: ${albumSongs}", Toast.LENGTH_SHORT).show()

                        Toast.makeText(this@AlbumActivity, "Đã xoá khỏi album", Toast.LENGTH_SHORT).show()
                    }

                    .addOnFailureListener {
                        Toast.makeText(this@AlbumActivity, "Lỗi xoá: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                startAlbumListener()
            }
        }, SongMode.REMOVE_ONLY, isAdmin = true)

        recyclerView.adapter = adapter

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        if (userId == null) {
            recyclerView.visibility = View.GONE
            llLoginRegister.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            llLoginRegister.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        if (userId != null) startAlbumListener()
    }

    override fun onStop() {
        super.onStop()
        stopAlbumListener()
    }

    private fun startAlbumListener() {
        val uid = userId ?: return
        val albumRef = database.child("Users").child(uid).child("albums")
        val songsRef = database.child("Songs")

        albumListener = object : ValueEventListener {
            override fun onDataChange(albumSnap: DataSnapshot) {
                val albumIds = albumSnap.children.mapNotNull { it.key }.toSet()

                songsRef.get().addOnSuccessListener { allSongsSnap ->
                    albumSongs.clear()
                    for (songSnap in allSongsSnap.children) {
                        val song = songSnap.getValue(Song::class.java)
                        val songId = songSnap.key ?: continue
                        if (song != null && albumIds.contains(songId)) {
                            song.id = songId
                            song.isInAlbum = true
                            albumSongs.add(song)
                        }
                    }
                    adapter.updateSongs(albumSongs)
                }.addOnFailureListener {
                    Toast.makeText(this@AlbumActivity, "Lỗi tải bài hát: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AlbumActivity, "Lỗi album: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        albumRef.addValueEventListener(albumListener!!)
    }

    private fun stopAlbumListener() {
        val uid = userId ?: return
        val albumRef = database.child("Users").child(uid).child("albums")
        albumListener?.let { albumRef.removeEventListener(it) }
    }
}