package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_ADMIN_DASHBOARD = 1001
    }

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: MainPagerAdapter
    private var btnLogout: Button? = null

    private var btnAdminDashboard: Button? = null
    private lateinit var btnAdminPanel: Button



    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Nếu chưa login → mở LoginActivity
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        btnLogout = findViewById(R.id.btnLogout)
        btnAdminDashboard = findViewById<Button>(R.id.btnAdminDashboard)
        btnAdminPanel = findViewById(R.id.btnAdminPanel)
        btnAdminPanel.visibility = View.GONE
        btnAdminDashboard?.visibility = View.GONE




        // Tạo fragments
        val allSongsFragment = AllSongsFragment()
        val albumFragment = AlbumFragment()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Users/$uid/isAdmin")




        // Khởi tạo adapter với danh sách fragment
        pagerAdapter = MainPagerAdapter(this, listOf(allSongsFragment, albumFragment))
        viewPager.adapter = pagerAdapter

        // Thiết lập tab với ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Tất cả bài hát" else "Album cá nhân"
        }.attach()

        // Xử lý logout
        btnLogout?.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        btnAdminDashboard?.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Không xác định được người dùng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

            userRef.child("isAdmin").get()
                .addOnSuccessListener { snapshot ->
                    val isAdmin = snapshot.getValue(Boolean::class.java) ?: false
                    if (isAdmin) {
                        val intent = Intent(this, AdminDashboardActivity::class.java)
                        adminDashboardLauncher.launch(intent)
                    } else {
                        Toast.makeText(this, "Bạn không phải admin", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Lỗi kiểm tra quyền admin: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }

        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(Boolean::class.java) == true) {
                btnAdminDashboard?.visibility = View.VISIBLE
            }
        }


    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADMIN_DASHBOARD && resultCode == RESULT_OK) {
            // Gọi lại hàm load bài hát trong AllSongsFragment
            pagerAdapter.getAllSongsFragment()?.fetchSongsWithAlbumStatus()
        }
    }

    private val adminDashboardLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Gọi lại hàm cập nhật bài hát
            pagerAdapter.getAllSongsFragment()?.fetchSongsWithAlbumStatus()
        }
    }




    fun refreshAlbumFragment() {
        pagerAdapter.getAlbumFragment()?.refreshAlbumSongs()
    }

    fun refreshAllSongs(newSongList: List<Song>) {
        Log.d("MainActivity", "Refreshing with ${newSongList.size} songs")
        pagerAdapter.getAllSongsFragment()?.updateSongList(newSongList)
    }
}
