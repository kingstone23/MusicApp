package com.example.musicapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // Đổi từ Button sang ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

enum class SongMode {
    ADD_ONLY,
    REMOVE_ONLY,
    SELECT_ONLY
}

class SongAdapter(
    private val context: Context,
    private val songs: MutableList<Song>,
    private val listener: OnSongClickListener,
    private val mode: SongMode,
    private val isAdmin: Boolean
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    interface OnSongClickListener {
        fun onSongClick(song: Song)
        fun onAddClick(song: Song)       // Thêm vào album
        fun onRemoveClick(song: Song)    // Gỡ khỏi album
        fun onDelete(song: Song)         // Admin xoá vĩnh viễn
        fun onUpdateClick(song: Song)
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 1. Đổi kiểu dữ liệu thành ImageButton
        private val btnAction: ImageButton = itemView.findViewById(R.id.btnAction)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btndelete)

        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        private val ivCover: ImageView = itemView.findViewById(R.id.ivCover)
        private val tvViews: TextView = itemView.findViewById(R.id.tvViews)

        fun bind(song: Song, position: Int) {
            // Reset trạng thái view
            btnDelete.visibility = View.GONE
            btnAction.visibility = View.GONE
            // Xóa sự kiện cũ để tránh lỗi logic khi tái sử dụng view
            btnDelete.setOnClickListener(null)
            btnAction.setOnClickListener(null)

            // Hiển thị thông tin bài hát
            tvTitle.text = song.title
            tvArtist.text = song.artist
            tvViews.text = "${song.views} lượt nghe"

            Glide.with(context)
                .load(song.cover)
                .placeholder(R.mipmap.ic_launcher) // Hiện ảnh nhẹ trong lúc chờ
                .error(R.mipmap.ic_launcher)
                .override(150, 150) // [CỰC KỲ QUAN TRỌNG]: Chỉ load ảnh nhỏ 150x150px
                .encodeFormat(android.graphics.Bitmap.CompressFormat.WEBP)
                .into(ivCover)

            // Click vào cả dòng -> Phát nhạc
            itemView.setOnClickListener {
                listener.onSongClick(song)
            }

            // --- XỬ LÝ LOGIC ICON (+ / -) ---
            if (isAdmin) {
                // ADMIN: Hiện nút thùng rác đỏ để xoá vĩnh viễn
                btnDelete.visibility = View.VISIBLE
                btnDelete.setImageResource(android.R.drawable.ic_menu_delete)
                btnDelete.setColorFilter(context.resources.getColor(android.R.color.holo_red_dark))

                btnDelete.setOnClickListener {
                    listener.onDelete(song)
                }
            } else {
                // USER THƯỜNG
                when (mode) {
                    SongMode.ADD_ONLY -> {
                        // --- MÀN HÌNH TẤT CẢ BÀI HÁT ---
                        if (song.isInAlbum) {
                            // Nếu đã có trong Album -> ẨN NÚT LUÔN (Theo yêu cầu của bạn)
                            btnAction.visibility = View.INVISIBLE
                            // Dùng INVISIBLE để giữ khoảng cách, hoặc GONE để mất hẳn

                            btnAction.setOnClickListener(null) // Xoá sự kiện click
                        } else {
                            // Chưa có trong Album -> Hiện icon CỘNG (+)
                            btnAction.visibility = View.VISIBLE
                            btnAction.setImageResource(android.R.drawable.ic_input_add)
                            btnAction.setColorFilter(context.resources.getColor(R.color.purple_500))

                            btnAction.setOnClickListener {
                                listener.onAddClick(song)
                            }
                        }
                    }

                    SongMode.REMOVE_ONLY -> {
                        // --- MÀN HÌNH ALBUM CÁ NHÂN ---
                        // Luôn hiện nút Xoá (-)
                        btnAction.visibility = View.VISIBLE
                        btnAction.setImageResource(android.R.drawable.ic_menu_delete)
                        btnAction.setColorFilter(context.resources.getColor(android.R.color.holo_red_dark))

                        btnAction.setOnClickListener {
                            listener.onRemoveClick(song)
                        }
                    }

                    SongMode.SELECT_ONLY -> {
                        btnAction.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position], position)
    }

    override fun getItemCount(): Int = songs.size

    fun updateSongs(newSongs: List<Song>) {
        val diffCallback = SongDiffCallback(songs, newSongs)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        songs.clear()
        songs.addAll(newSongs)
        diffResult.dispatchUpdatesTo(this)
    }
}