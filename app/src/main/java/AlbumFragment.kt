package com.example.musicapp
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// AlbumFragment đã implement OnSongClickListener, nên phải có tất cả các hàm của nó
class AlbumFragment : Fragment(), SongAdapter.OnSongClickListener {


    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private val albumSongs = mutableListOf<Song>()

    private val database by lazy { FirebaseDatabase.getInstance().reference }
    private val userId get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_album, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = SongAdapter(requireContext(), albumSongs, this, SongMode.REMOVE_ONLY,isAdmin = false)
        recyclerView.adapter = adapter

        loadAlbumSongs()
        return view
    }

    private fun loadAlbumSongs() {
        val uid = userId ?: return
        val albumRef = database.child("Users").child(uid).child("albums")
        val songsRef = database.child("Songs")

        albumRef.get().addOnSuccessListener { albumSnap ->
            val albumIds = albumSnap.children.mapNotNull { it.key }.toSet()

            songsRef.get().addOnSuccessListener { songsSnap ->
                albumSongs.clear()
                for (songSnap in songsSnap.children) {

                    val song = songSnap.getValue(Song::class.java)
                    if (song != null) {
                        song.id = songSnap.key ?: ""
                        if (albumIds.contains(song.id)) {
                            song.isInAlbum = true
                            albumSongs.add(song)
                        }
                    }
                    Log.d("AlbumCheck", "song.id = ${song?.id}, albumIds = $albumIds")
                }
                adapter.notifyDataSetChanged()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi tải bài hát: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi tải album: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAddClick(song: Song) {
        // Không dùng trong AlbumFragment
    }
    override fun onDelete(song: Song) {
        // Không dùng trong AlbumActivity
    }

    // <<<< BẠN CHỈ CẦN THÊM HÀM NÀY VÀO >>>>
    override fun onUpdateClick(song: Song) {
        // Chức năng này không dùng trong AlbumFragment, để trống.
    }
    // <<<< KẾT THÚC PHẦN THÊM MỚI >>>>


    override fun onRemoveClick(song: Song) {
        val uid = userId ?: return
        val id = song.id.takeIf { it.isNotEmpty() } ?: return

        database.child("Users").child(uid).child("albums").child(id).removeValue()
            .addOnSuccessListener {
                song.isInAlbum = false
                albumSongs.remove(song)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Đã xoá khỏi album", Toast.LENGTH_SHORT).show()
                refreshAlbumSongs()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi xoá: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSongClick(song: Song) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("title", song.title)
            putExtra("artist", song.artist)
            putExtra("cover", song.cover)
            putExtra("audioUrl", song.audioUrl)
            putExtra("songId", song.id)
        }
        startActivity(intent)
    }

    fun refreshAlbumSongs() {
        loadAlbumSongs()
    }
    override fun onResume() {
        super.onResume()
        // Tải lại dữ liệu mỗi khi quay lại màn hình này
        // Để cập nhật số View mới nhất
        loadAlbumSongs()
    }
}
