package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu // Import Menu
import android.view.MenuItem // Import MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: MainPagerAdapter

    // private lateinit var tvWelcome: TextView -> KHÔNG CẦN NỮA
    // private var btnLogout: View? = null -> KHÔNG CẦN NỮA

    private var btnAdminDashboard: Button? = null
    private var btnAdminPanel: Button? = null

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val adminDashboardLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            pagerAdapter.getAllSongsFragment()?.fetchSongsWithAlbumStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Ánh xạ View
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        // btnLogout = findViewById(R.id.btnLogout) -> XÓA DÒNG NÀY

        btnAdminDashboard = findViewById(R.id.btnAdminDashboard)
        btnAdminPanel = findViewById(R.id.btnAdminPanel)

        btnAdminPanel?.visibility = View.GONE
        btnAdminDashboard?.visibility = View.GONE

        // Setup Pager & Tabs
        val rankingFragment = RankingFragment()
        val allSongsFragment = AllSongsFragment()
        val albumFragment = AlbumFragment()

        pagerAdapter = MainPagerAdapter(this, listOf(rankingFragment, allSongsFragment, albumFragment))
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Bảng xếp hạng"
                1 -> "Tất cả bài hát"
                2 -> "Album cá nhân"
                else -> ""
            }
        }.attach()

        // Xử lý Logout cũ -> XÓA ĐOẠN btnLogout.setOnClickListener CŨ ĐI

        checkAdminPermission()
        loadUserName() // Load tên lên thanh Action Bar

        // Admin Button Logic
        btnAdminDashboard?.setOnClickListener {
            // ... (Giữ nguyên logic Admin) ...
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
            userRef.child("isAdmin").get().addOnSuccessListener { snapshot ->
                val isAdmin = snapshot.getValue(Boolean::class.java) ?: false
                if (isAdmin) {
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    adminDashboardLauncher.launch(intent)
                }
            }
        }
    }

    // --- 1. KHỞI TẠO MENU (Hiện nút Logout lên góc phải) ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // --- 2. XỬ LÝ SỰ KIỆN BẤM MENU ---
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // Logic đăng xuất ở đây
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadUserName() {
        val user = auth.currentUser
        val uid = user?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            val fullName = snapshot.child("fullName").getValue(String::class.java)
                ?: snapshot.child("name").getValue(String::class.java)
            val displayName = if (!fullName.isNullOrEmpty()) fullName else user.email?.substringBefore("@") ?: "User"

            // Hiện tên lên thanh tiêu đề
            supportActionBar?.title = "Hi, $displayName "

        }.addOnFailureListener {
            supportActionBar?.title = "Music App"
        }
    }

    // ... (Các hàm checkAdminPermission, refresh... giữ nguyên)
    private fun checkAdminPermission() { /*...*/ }
    fun refreshAlbumFragment() { /*...*/ }
    fun refreshAllSongs(newSongList: List<Song>) { /*...*/ }
}