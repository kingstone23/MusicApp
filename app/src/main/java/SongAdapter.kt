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
    REMOVE_ONLY
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
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnAction: Button = itemView.findViewById(R.id.btnAction)
        private val btnDelete: Button = itemView.findViewById(R.id.btndelete)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        private val ivCover: ImageView = itemView.findViewById(R.id.ivCover)


        fun bind(song: Song, position: Int) {
            // Reset để tránh reuse lỗi
            btnDelete.visibility = View.GONE
            btnDelete.setOnClickListener(null)
            btnAction.visibility = View.GONE
            btnAction.setOnClickListener(null)

            // Hiển thị thông tin bài hát
            tvTitle.text = song.title
            tvArtist.text = song.artist
            Glide.with(context).load(song.cover).into(ivCover)

            itemView.setOnClickListener {
                listener.onSongClick(song)
            }


            // ✅ Chức năng chung cho tất cả người dùng: thêm / xoá khỏi album

            btnAction.visibility = View.VISIBLE

            when (mode) {
                SongMode.ADD_ONLY -> {
                    btnAction.text = "Thêm"
                    btnAction.setOnClickListener {

                        if (song.isInAlbum) {
                            Toast.makeText(context, "Bài hát đã có trong album", Toast.LENGTH_SHORT).show()
                        } else {
                            song.isInAlbum = true
                            notifyItemChanged(position)
                            listener.onAddClick(song)
                        }
                    }
                }

                SongMode.REMOVE_ONLY -> {
                    btnAction.text = "Xoá"
                    btnAction.setOnClickListener {
                        song.isInAlbum = false
                        notifyItemChanged(position)
                        listener.onRemoveClick(song)
                    }
                }
            }

            // ✅ Chức năng mở rộng cho admin: xoá khỏi hệ thống
            if (isAdmin) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.text = "Xoá"
                btnDelete.setOnClickListener {
                    listener.onDelete(song)
                }
            } else {
                btnDelete.visibility = View.GONE // ⚠️ Dòng này rất quan trọng
                btnDelete.setOnClickListener(null)
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