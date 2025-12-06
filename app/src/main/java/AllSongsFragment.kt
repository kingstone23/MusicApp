package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AllSongsFragment(private val mode: SongMode = SongMode.ADD_ONLY) : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter

    // danh sách gốc (full) và danh sách hiển thị (filter)
    private val fullSongList = mutableListOf<Song>()
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

        // Adapter - bạn giữ nguyên listener của bạn (chỉ dùng songList làm nguồn dữ liệu)
        songAdapter = SongAdapter(requireContext(), songList, object : SongAdapter.OnSongClickListener {
            override fun onAddClick(song: Song) {
                val songId = song.id
                if (songId.isBlank()) {
                    Toast.makeText(requireContext(), "ID bài hát không hợp lệ", Toast.LENGTH_SHORT).show()
                    return
                }

                userAlbumRef.child(songId).setValue(true)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Đã thêm vào album", Toast.LENGTH_SHORT).show()

                        // Cập nhật trạng thái trên cả fullSongList và songList
                        updateSongAlbumStatus(songId, true)
                        (activity as? MainActivity)?.refreshAlbumFragment()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Lỗi khi thêm: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onRemoveClick(song: Song) {
                val songId = song.id
                if (songId.isBlank()) {
                    Toast.makeText(requireContext(), "ID bài hát không hợp lệ", Toast.LENGTH_SHORT).show()
                    return
                }

                userAlbumRef.child(songId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Đã xoá khỏi album", Toast.LENGTH_SHORT).show()

                        updateSongAlbumStatus(songId, false)
                        (activity as? MainActivity)?.refreshAlbumFragment()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Lỗi khi xoá bài hát: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onUpdateClick(song: Song) {}
            override fun onDelete(song: Song) {}

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

        // setup SearchView nếu có trong layout
        val searchView = view.findViewById<SearchView?>(R.id.searchViewSongs)
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterSongs(newText)
                return true
            }
        })

        // Không load ngay ở onCreateView, nhưng nếu muốn cũng có thể gọi fetch ở đây
        return view
    }

    override fun onResume() {
        super.onResume()
        auth.currentUser?.let {
            fetchSongsWithAlbumStatus()
        } ?: Log.d("AllSongsFragment", "User not logged in, cannot fetch songs")
    }

    private fun updateSongAlbumStatus(songId: String, inAlbum: Boolean) {
        // cập nhật cả trong fullSongList và songList
        var changedIndex = -1
        for ((i, s) in fullSongList.withIndex()) {
            if (s.id == songId) {
                s.isInAlbum = inAlbum
                // nếu item đang hiển thị (songList chứa đối tượng này) thì cập nhật adapter
                val indexInDisplay = songList.indexOfFirst { it.id == songId }
                if (indexInDisplay != -1) {
                    songAdapter.notifyItemChanged(indexInDisplay)
                }
                changedIndex = i
                break
            }
        }
        // cũng cập nhật display list element nếu cần (objects tham chiếu cùng nhau thường cập nhật tự động)
    }

    fun fetchSongsWithAlbumStatus() {
        // show loading nếu bạn có
        userAlbumRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(albumSnap: DataSnapshot) {
                val albumIds = albumSnap.children.mapNotNull { it.key }.toSet()

                loadSongs { songs ->
                    fullSongList.clear()
                    songList.clear()

                    songs.forEach { song ->
                        song.isInAlbum = albumIds.contains(song.id)
                        fullSongList.add(song)
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
        }.addOnFailureListener { ex ->
            Toast.makeText(requireContext(), "Lỗi khi tải bài hát: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterSongs(keyword: String?) {
        songList.clear()
        if (keyword.isNullOrBlank()) {
            songList.addAll(fullSongList)
        } else {
            val lower = keyword.lowercase()
            songList.addAll(
                fullSongList.filter {
                    it.title.lowercase().contains(lower) || it.artist.lowercase().contains(lower)
                }
            )
        }
        songAdapter.notifyDataSetChanged()
    }

    // nếu bạn cần update list từ nơi khác
    fun updateSongList(newSongList: List<Song>) {
        fullSongList.clear()
        fullSongList.addAll(newSongList)
        songList.clear()
        songList.addAll(newSongList)
        songAdapter.notifyDataSetChanged()
    }
}
