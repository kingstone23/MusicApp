package com.example.musicapp

// 1. Import @Exclude của Firebase
// (Dùng 1 trong 2, tùy bạn dùng Realtime DB hay Firestore)
import com.google.firebase.database.Exclude // Cho Realtime Database
// import com.google.firebase.firestore.Exclude // Cho Firestore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable // Bạn không cần Serializable nếu đã dùng Parcelable

@Parcelize
data class Song(
    var title: String = "",
    var artist: String = "",
    var audioUrl: String = "",
    var cover: String = "",
    var genre: String? = null,
    var views: Int = 0,

    // 2. Thay @Transient bằng @Exclude
    @get:Exclude
    var id: String = "",

    // 3. Thêm @Exclude cho isInAlbum nếu bạn cũng không muốn lưu nó
    @get:Exclude
    var isInAlbum: Boolean = false
) : Parcelable