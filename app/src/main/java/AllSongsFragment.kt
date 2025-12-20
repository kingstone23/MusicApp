package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.PopupMenu
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
    private lateinit var btnFilter: ImageButton // Nút lọc mới

    // 1. fullSongList: Lưu trữ toàn bộ dữ liệu gốc từ Firebase
    // 2. songList: Lưu trữ dữ liệu đang hiển thị (sau khi lọc)
    private val fullSongList = mutableListOf<Song>()
    private val songList = mutableListOf<Song>()

    // Biến lưu trạng thái lọc
    private var currentGenre = "Tất cả"
    private var currentSearchText = ""

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
        // Inflate layout (Đảm bảo XML của bạn đã có btnFilter như hướng dẫn trước)
        val view = inflater.inflate(R.layout.fragment_all_songs, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewSongs)
        btnFilter = view.findViewById(R.id.btnFilter) // Ánh xạ nút Filter

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Setup Adapter
        songAdapter = SongAdapter(requireContext(), songList, object : SongAdapter.OnSongClickListener {
            override fun onAddClick(song: Song) {
                handleAddToAlbum(song)
            }

            override fun onRemoveClick(song: Song) {
                handleRemoveFromAlbum(song)
            }

            override fun onUpdateClick(song: Song) {}
            override fun onDelete(song: Song) {}

            override fun onSongClick(song: Song) {
                // Chuyển sang màn hình phát nhạc
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("songId", song.id)
                    putExtra("title", song.title)
                    putExtra("artist", song.artist)
                    putExtra("cover", song.cover)
                    putExtra("audioUrl", song.audioUrl)
                    putExtra("genre", song.genre)
                }
                startActivity(intent)
            }
        }, mode, isAdmin = false)

        recyclerView.adapter = songAdapter

        // Setup SearchView
        val searchView = view.findViewById<SearchView?>(R.id.searchViewSongs)
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchText = newText ?: ""
                applyFilters() // Gọi hàm lọc kết hợp
                return true
            }
        })

        // Setup nút Filter
        btnFilter.setOnClickListener {
            showFilterMenu()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Mỗi lần quay lại màn hình này thì tải lại dữ liệu để cập nhật trạng thái album
        auth.currentUser?.let {
            fetchSongsWithAlbumStatus()
        } ?: Log.d("AllSongsFragment", "User not logged in")
    }

    // --- LOGIC HIỂN THỊ MENU FILTER ---
    private fun showFilterMenu() {
        val popup = PopupMenu(requireContext(), btnFilter)

        // Danh sách thể loại (Bạn có thể thêm bớt tùy ý)
        popup.menu.add("Tất cả")
        popup.menu.add("Pop")
        popup.menu.add("Ballad")
        popup.menu.add("Rap")
        popup.menu.add("Indie")
        popup.menu.add("R&B")
        popup.menu.add("Rock")
        popup.menu.add("EDM")

        popup.setOnMenuItemClickListener { item ->
            currentGenre = item.title.toString()
            Toast.makeText(requireContext(), "Đang lọc: $currentGenre", Toast.LENGTH_SHORT).show()
            applyFilters() // Gọi hàm lọc kết hợp
            true
        }
        popup.show()
    }

    // --- LOGIC LỌC KÉP (SEARCH + GENRE) ---
    private fun applyFilters() {
        songList.clear()

        val filteredList = fullSongList.filter { song ->
            // Điều kiện 1: Thể loại
            val matchGenre = if (currentGenre == "Tất cả") {
                true
            } else {
                // So sánh không phân biệt hoa thường
                song.genre?.trim()?.equals(currentGenre, ignoreCase = true) == true
            }

            // Điều kiện 2: Tìm kiếm (Tên bài hát hoặc Ca sĩ)
            val matchSearch = if (currentSearchText.isEmpty()) {
                true
            } else {
                val lowerKey = currentSearchText.lowercase()
                (song.title?.lowercase()?.contains(lowerKey) == true) ||
                        (song.artist?.lowercase()?.contains(lowerKey) == true)
            }

            // Phải thỏa mãn CẢ HAI
            matchGenre && matchSearch
        }

        songList.addAll(filteredList)
        songAdapter.notifyDataSetChanged()
    }

    // --- XỬ LÝ DATABASE: THÊM/XÓA ALBUM ---
    private fun handleAddToAlbum(song: Song) {
        val songId = song.id
        if (songId.isBlank()) return

        userAlbumRef.child(songId).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Đã thêm vào album", Toast.LENGTH_SHORT).show()
                updateSongAlbumStatus(songId, true)
                // Gọi Activity cha để refresh AlbumFragment nếu cần
                (activity as? MainActivity)?.refreshAlbumFragment()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleRemoveFromAlbum(song: Song) {
        val songId = song.id
        if (songId.isBlank()) return

        userAlbumRef.child(songId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Đã xoá khỏi album", Toast.LENGTH_SHORT).show()
                updateSongAlbumStatus(songId, false)
                (activity as? MainActivity)?.refreshAlbumFragment()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Cập nhật trạng thái isInAlbum cục bộ để giao diện đổi màu trái tim ngay lập tức
    private fun updateSongAlbumStatus(songId: String, inAlbum: Boolean) {
        // Cập nhật list gốc
        fullSongList.find { it.id == songId }?.isInAlbum = inAlbum

        // Cập nhật list hiển thị
        val indexInDisplay = songList.indexOfFirst { it.id == songId }
        if (indexInDisplay != -1) {
            songList[indexInDisplay].isInAlbum = inAlbum
            songAdapter.notifyItemChanged(indexInDisplay)
        }
    }

    // --- TẢI DỮ LIỆU TỪ FIREBASE ---
    fun fetchSongsWithAlbumStatus() {
        // 1. Lấy danh sách ID bài hát trong Album của User
        userAlbumRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(albumSnap: DataSnapshot) {
                val albumIds = albumSnap.children.mapNotNull { it.key }.toSet()

                // 2. Lấy toàn bộ bài hát từ node Songs
                loadSongs { songs ->
                    fullSongList.clear()

                    songs.forEach { song ->
                        // Kiểm tra xem bài hát này có trong album user không
                        song.isInAlbum = albumIds.contains(song.id)
                        fullSongList.add(song)
                    }

                    // 3. Quan trọng: Sau khi tải xong, chạy bộ lọc để hiển thị đúng
                    applyFilters()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Lỗi tải album: ${error.message}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "Lỗi tải nhạc: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // --- PUBLIC FUNCTION: Để MainActivity gọi khi cần update list từ bên ngoài ---
    fun updateSongList(newSongList: List<Song>) {
        fullSongList.clear()
        fullSongList.addAll(newSongList)
        // Gọi applyFilters thay vì addAll trực tiếp để giữ trạng thái lọc
        applyFilters()
    }
}