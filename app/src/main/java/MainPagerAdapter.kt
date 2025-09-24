package com.example.musicapp

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(
    activity: AppCompatActivity,
    private val fragments: List<Fragment>   // cho phép truyền list fragment
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    // Hàm tiện ích lấy fragment theo kiểu
    fun getAlbumFragment(): AlbumFragment? {
        return fragments.find { it is AlbumFragment } as? AlbumFragment
    }

    fun getAllSongsFragment(): AllSongsFragment? {
        return fragments.find { it is AllSongsFragment } as? AllSongsFragment
    }
}
