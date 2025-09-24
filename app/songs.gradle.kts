import com.google.firebase.database.FirebaseDatabase

data class Song(
    val id: Long = 0,
    val title: String = "",
    val artist: String = "",
    val url: String = "",
    val cover: String = ""
)


fun saveSong(song: Song) {
    val db = FirebaseDatabase.getInstance()
    val ref = db.getReference("songs")
    ref.child(song.id).setValue(song)
}
