import com.google.firebase.database.*

fun listenSongs() {
    val db = FirebaseDatabase.getInstance()
    val ref = db.getReference("Songs")

    ref.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = mutableListOf<Song>()
            for (data in snapshot.children) {
                val song = data.getValue(Song::class.java)
                if (song != null) list.add(song)
            }
            println("Danh sách bài hát: $list")
        }

        override fun onCancelled(error: DatabaseError) {
            println("Lỗi: ${error.message}")
        }

    })
}
