package com.example.musicapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RankingAdapter(
    private val context: Context,
    private val songs: List<Song>
) : RecyclerView.Adapter<RankingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvArtist: TextView = view.findViewById(R.id.tvArtist)
        val tvViews: TextView = view.findViewById(R.id.tvViews)
        val ivCover: ImageView = view.findViewById(R.id.ivCover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_ranking_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]

        holder.tvRank.text = (position + 1).toString()
        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.artist
        holder.tvViews.text = "${song.views} lượt"

        Glide.with(context).load(song.cover).into(holder.ivCover)
    }

    override fun getItemCount() = songs.size
}
