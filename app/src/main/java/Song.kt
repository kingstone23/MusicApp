package com.example.musicapp

data class Song(
    var title: String = "",
    var artist: String = "",
    var audioUrl: String = "",
    var cover: String = "",
    @Transient var id: String = "", // ✅ không lưu vào Firebase
    var isInAlbum: Boolean = false  // ✅ trạng thái tạm thời, không cần lưu
)
