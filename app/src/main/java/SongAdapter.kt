package com.example.musicapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
    private val mode: SongMode, // vẫn giữ để dùng nếu cần
    private val isAdmin: Boolean
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    interface OnSongClickListener {
        fun onSongClick(song: Song)
        fun onAddClick(song: Song)       // thêm vào album
        fun onRemoveClick(song: Song)
        fun onDelete(song: Song)// gỡ khỏi album

        fun onUpdateClick(song: Song)
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnAction: Button = itemView.findViewById(R.id.btnAction)
        private val btnDelete: Button = itemView.findViewById(R.id.btndelete)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        private val ivCover: ImageView = itemView.findViewById(R.id.ivCover)


        fun bind(song: Song, position: Int) {
            // 1. Reset các nút về trạng thái ẩn (Giữ nguyên)
            btnDelete.visibility = View.GONE
            btnDelete.setOnClickListener(null)
            btnAction.visibility = View.GONE
            btnAction.setOnClickListener(null)

            // 2. Hiển thị thông tin bài hát (Giữ nguyên)
            tvTitle.text = song.title
            tvArtist.text = song.artist
            Glide.with(context).load(song.cover).into(ivCover)

            // 3. Sự kiện click vào item (Giữ nguyên)
            itemView.setOnClickListener {
                listener.onSongClick(song)
            }

            // --- BẮT ĐẦU PHẦN SỬA LỖI LOGIC ---
            // 4. Chỉ dùng MỘT cấu trúc if/else duy nhất để quyết định hiển thị nút nào
            if (isAdmin) {
                // Nếu là Admin, chỉ hiển thị nút Delete
                btnDelete.visibility = View.VISIBLE
                btnDelete.text = "Xoá"
                btnDelete.setOnClickListener {
                    listener.onDelete(song)
                }
            } else {
                // Nếu là người dùng thường, dùng 'when' để quyết định hành động của btnAction
                when (mode) {
                    SongMode.ADD_ONLY -> {
                        btnAction.visibility = View.VISIBLE
                        if (song.isInAlbum) {
                            btnAction.visibility = View.INVISIBLE // Ẩn đi nếu đã có trong album
                        }
                        btnAction.text = "Thêm"
                        btnAction.setOnClickListener {
                            if (!song.isInAlbum) listener.onAddClick(song)
                        }
                    }

                    SongMode.REMOVE_ONLY -> {
                        btnAction.visibility = View.VISIBLE
                        btnAction.text = "Xoá" // "Xoá" này là gỡ khỏi album
                        btnAction.setOnClickListener {
                            listener.onRemoveClick(song)
                        }
                    }

                    SongMode.SELECT_ONLY -> {
                        // Không làm gì cả.
                        // Vì btnAction đã được set là GONE ở đầu hàm,
                        // nên nó sẽ không hiển thị. Đây là điều chúng ta muốn.
                    }
                }
            }
            // --- KẾT THÚC PHẦN SỬA LỖI LOGIC ---
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