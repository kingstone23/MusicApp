package com.example.musicapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class RankingActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        recycler = findViewById(R.id.recyclerRanking)
        recycler.layoutManager = LinearLayoutManager(this)

        loadRankingSongs()
    }

    private fun loadRankingSongs() {
        FirebaseDatabase.getInstance()
            .getReference("Songs")
            .get()
            .addOnSuccessListener { snapshot ->
                val songs = snapshot.children.mapNotNull {
                    val song = it.getValue(Song::class.java)
                    song?.apply { id = it.key ?: "" }
                }
                // 🔥 SẮP XẾP THEO VIEW GIẢM DẦN
                val sortedSongs = songs.sortedByDescending { it.views }

                recycler.adapter = RankingAdapter(this, sortedSongs)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi tải BXH", Toast.LENGTH_SHORT).show()
                Log.e("RankingActivity", it.message ?: "")
            }
    }
}
