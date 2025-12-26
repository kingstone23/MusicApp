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
import com.google.firebase.database.FirebaseDatabase

class RankingFragment : Fragment() {

    private lateinit var recycler: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_ranking, container, false) // Vẫn dùng layout cũ ok

        recycler = view.findViewById(R.id.recyclerRanking)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        loadRankingSongs()

        return view
    }

    private fun loadRankingSongs() {
        FirebaseDatabase.getInstance()
            .getReference("Songs")
            .get()
            .addOnSuccessListener { snapshot ->
                // Kiểm tra nếu Fragment đã bị đóng thì không làm gì cả để tránh crash
                if (!isAdded) return@addOnSuccessListener

                val songs = snapshot.children.mapNotNull {
                    val song = it.getValue(Song::class.java)
                    song?.apply { id = it.key ?: "" }
                }

                // Sắp xếp giảm dần theo lượt xem
                val sortedSongs = songs.sortedByDescending { it.views }

                // --- [THAY ĐỔI Ở ĐÂY] ---
                // Truyền thêm lambda function xử lý click
                recycler.adapter = RankingAdapter(requireContext(), sortedSongs) { song ->

                    // Code chuyển sang màn hình phát nhạc
                    val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                        putExtra("songId", song.id) // Quan trọng để update lượt view
                        putExtra("title", song.title)
                        putExtra("artist", song.artist)
                        putExtra("cover", song.cover)
                        putExtra("audioUrl", song.audioUrl)
                        putExtra("genre", song.genre)
                    }
                    startActivity(intent)
                }
                // ------------------------
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Lỗi tải BXH", Toast.LENGTH_SHORT).show()
                }
                Log.e("RankingFragment", it.message ?: "")
            }
    }

    override fun onResume() {
        super.onResume()
        // Load lại ranking mỗi khi quay lại tab này (để cập nhật view mới nhất)
        loadRankingSongs()
    }
}