package com.example.musicapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Thêm tham số (val onItemClick: (Song) -> Unit) vào cuối
class RankingAdapter(
    private val context: Context,
    private val songList: List<Song>,
    private val onItemClick: (Song) -> Unit
) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {

    class RankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        val ivCover: ImageView = itemView.findViewById(R.id.ivCover)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val tvViews: TextView = itemView.findViewById(R.id.tvViews)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_ranking_song, parent, false)
        return RankingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val song = songList[position]

        // 1. Hiển thị thứ hạng (Top 1, 2, 3...)
        holder.tvRank.text = (position + 1).toString()

        // Đổi màu cho Top 3 cho đẹp (Tùy chọn)
        when (position) {
            0 -> holder.tvRank.setTextColor(context.resources.getColor(android.R.color.holo_red_dark)) // Top 1 màu đỏ
            1 -> holder.tvRank.setTextColor(context.resources.getColor(android.R.color.holo_orange_dark)) // Top 2 cam
            2 -> holder.tvRank.setTextColor(context.resources.getColor(android.R.color.holo_green_dark)) // Top 3 xanh
            else -> holder.tvRank.setTextColor(context.resources.getColor(android.R.color.black))
        }

        // 2. Hiển thị thông tin bài hát
        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.artist
        holder.tvViews.text = "${song.views} lượt nghe"

        // 3. Load ảnh bằng Glide
        Glide.with(context)
            .load(song.cover)
            .placeholder(R.mipmap.ic_launcher) // Ảnh chờ nếu load lỗi
            .into(holder.ivCover)

        // 4. [QUAN TRỌNG] Bắt sự kiện Click vào bài hát
        holder.itemView.setOnClickListener {
            onItemClick(song) // Gọi hàm click để Fragment xử lý
        }
    }

    override fun getItemCount(): Int {
        return songList.size
    }
}