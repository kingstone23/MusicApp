package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AllSongsFragment(private val mode: SongMode = SongMode.ADD_ONLY) : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private val songList = mutableListOf<Song>()

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val songsRef by lazy { FirebaseDatabase.getInstance().getReference("Songs") }
    private val userAlbumRef by lazy {
        FirebaseDatabase.getInstance().getReference("Users")
            .child(auth.currentUser?.uid ?: "")
            .child("albums")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_songs, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewSongs)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        songAdapter = SongAdapter(requireContext(), songList, object : SongAdapter.OnSongClickListener {
            override fun onAddClick(song: Song) {
                val songId = song.id
                userAlbumRef.child(songId).setValue(true)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Đã thêm vào album", Toast.LENGTH_SHORT).show()

                        // Cập nhật trạng thái ngay trên UI
                        val index = songList.indexOf(song)
                        if (index != -1) {
                            songList[index].isInAlbum = true
                            songAdapter.notifyItemChanged(index)
                        }

                        // Thông báo cho AlbumFragment để tự làm mới
                        (activity as? MainActivity)?.refreshAlbumFragment()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Lỗi khi thêm: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            // <<< HÀM NÀY ĐÃ ĐƯỢC TỐI ƯU HÓA >>>
            override fun onRemoveClick(song: Song) {
                val songId = song.id
                userAlbumRef.child(songId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Đã xoá khỏi album", Toast.LENGTH_SHORT).show()

                        // Chỉ cần cập nhật trạng thái ngay trên UI là đủ
                        val index = songList.indexOf(song)
                        if (index != -1) {
                            songList[index].isInAlbum = false
                            songAdapter.notifyItemChanged(index)
                        }

                        // Thông báo cho AlbumFragment để tự làm mới
                        (activity as? MainActivity)?.refreshAlbumFragment()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Lỗi khi xoá bài hát: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onUpdateClick(song: Song) { }
            override fun onDelete(song: Song) { }

            override fun onSongClick(song: Song) {
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("title", song.title)
                    putExtra("artist", song.artist)
                    putExtra("cover", song.cover)
                    putExtra("audioUrl", song.audioUrl)
                }
                startActivity(intent)
            }
        }, mode, isAdmin = false)

        recyclerView.adapter = songAdapter

        // Không cần gọi fetch ở đây nữa, onResume sẽ xử lý
        return view
    }

    // <<< THÊM PHƯƠNG THỨC NÀY VÀO - ĐÂY LÀ PHẦN SỬA LỖI QUAN TRỌNG NHẤT >>>
    override fun onResume() {
        super.onResume()
        // Mỗi khi fragment này được hiển thị lại, hãy tải dữ liệu mới nhất
        auth.currentUser?.let {
            fetchSongsWithAlbumStatus()
        } ?: Log.d("AllSongsFragment", "User not logged in, cannot fetch songs")
    }

    fun fetchSongsWithAlbumStatus() {
        userAlbumRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(albumSnap: DataSnapshot) {
                val albumIds = albumSnap.children.mapNotNull { it.key }.toSet()

                loadSongs { songs ->
                    songList.clear()
                    songs.forEach { song ->
                        song.isInAlbum = albumIds.contains(song.id)
                        songList.add(song)
                    }
                    songAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Không thể tải album: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun loadSongs(onLoaded: (List<Song>) -> Unit) {
        songsRef.get().addOnSuccessListener { snapshot ->
            val songs = snapshot.children.mapNotNull { songSnap ->
                val song = songSnap.getValue(Song::class.java)
                song?.apply { id = songSnap.key ?: "" }
            }
            onLoaded(songs)
        }
    }

    fun updateSongList(newSongList: List<Song>) {
        songList.clear()
        songList.addAll(newSongList)
        songAdapter.notifyDataSetChanged()
    }
}