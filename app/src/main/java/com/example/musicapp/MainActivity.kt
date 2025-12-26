package com.example.musicapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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

    private var btnLogout: Button? = null
    private var btnAdminDashboard: Button? = null
    private var btnAdminPanel: Button? = null // Đã sửa thành nullable cho an toàn

    private val auth by lazy { FirebaseAuth.getInstance() }

    // Launcher xử lý kết quả trả về từ AdminDashboard (Thay thế cho onActivityResult cũ)
    private val adminDashboardLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Khi quay lại từ Admin Dashboard, refresh lại dữ liệu AllSongsFragment
            // để cập nhật các bài hát mới thêm hoặc đã sửa
            pagerAdapter.getAllSongsFragment()?.fetchSongsWithAlbumStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Kiểm tra Login
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)




        // Ánh xạ View
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        btnLogout = findViewById(R.id.btnLogout)
        btnAdminDashboard = findViewById(R.id.btnAdminDashboard)
        btnAdminPanel = findViewById(R.id.btnAdminPanel)

        // Ẩn mặc định
        btnAdminPanel?.visibility = View.GONE
        btnAdminDashboard?.visibility = View.GONE

        // Khởi tạo các Fragment
        val rankingFragment = RankingFragment()
        val allSongsFragment = AllSongsFragment()
        val albumFragment = AlbumFragment()

        // Setup ViewPager Adapter
        // (Đảm bảo MainPagerAdapter của bạn có hàm getAllSongsFragment() và getAlbumFragment())
        pagerAdapter = MainPagerAdapter(this, listOf(rankingFragment, allSongsFragment, albumFragment))
        viewPager.adapter = pagerAdapter

        // Setup TabLayout
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Bảng xếp hạng"    // Tab đầu tiên
                1 -> "Tất cả bài hát"   // Tab thứ hai
                2 -> "Album cá nhân"    // Tab thứ ba
                else -> ""
            }
        }.attach()

        // Xử lý Logout
        btnLogout?.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // Kiểm tra quyền Admin để hiện nút Dashboard
        checkAdminPermission()

        // Sự kiện click nút Admin Dashboard
        btnAdminDashboard?.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            // Kiểm tra lại lần nữa cho chắc chắn
            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
            userRef.child("isAdmin").get()
                .addOnSuccessListener { snapshot ->
                    val isAdmin = snapshot.getValue(Boolean::class.java) ?: false
                    if (isAdmin) {
                        val intent = Intent(this, AdminDashboardActivity::class.java)
                        // Dùng launcher để nhận kết quả trả về
                        adminDashboardLauncher.launch(intent)
                    } else {
                        Toast.makeText(this, "Bạn không phải admin", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Lỗi kiểm tra quyền: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkAdminPermission() {
        val uid = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Users/$uid/isAdmin")

        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(Boolean::class.java) == true) {
                btnAdminDashboard?.visibility = View.VISIBLE
            }
        }.addOnFailureListener {
            Log.e("MainActivity", "Failed to check admin status: ${it.message}")
        }
    }

    // Hàm gọi từ AllSongsFragment để refresh AlbumFragment
    fun refreshAlbumFragment() {
        pagerAdapter.getAlbumFragment()?.refreshAlbumSongs()
    }

    // Hàm gọi từ nơi khác nếu cần refresh list AllSongs
    fun refreshAllSongs(newSongList: List<Song>) {
        Log.d("MainActivity", "Refreshing with ${newSongList.size} songs")
        pagerAdapter.getAllSongsFragment()?.updateSongList(newSongList)
    }
}