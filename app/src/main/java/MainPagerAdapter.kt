package com.example.musicapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

// Adapter chuẩn cho ViewPager2
class MainPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val fragments: List<Fragment> // <--- Danh sách phải là Fragment
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    // Hàm tiện ích để lấy Fragment ra (dùng để refresh dữ liệu)
    fun getAllSongsFragment(): AllSongsFragment? {
        return fragments.filterIsInstance<AllSongsFragment>().firstOrNull()
    }

    fun getAlbumFragment(): AlbumFragment? {
        return fragments.filterIsInstance<AlbumFragment>().firstOrNull()
    }
}