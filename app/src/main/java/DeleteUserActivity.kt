package com.example.musicapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class DeleteUserActivity : AppCompatActivity() {

    private lateinit var edtUserId: EditText
    private lateinit var btnDelete: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_user)

        edtUserId = findViewById(R.id.edtUserId)
        btnDelete = findViewById(R.id.btnDelete)

        btnDelete.setOnClickListener {
            val uid = edtUserId.text.toString().trim()

            if (uid.isEmpty()) {
                Toast.makeText(this, "Vui lòng xoá trên firebase", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val usersRef = FirebaseDatabase.getInstance().getReference("Users")
            usersRef.child(uid).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Đã xoá người dùng", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "❌ Lỗi xoá: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}