package com.example.weatherapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/*
Адаптер для фрагментів. Сюди передається вибрана позиція TabLayout
 */

/*
Повернення розміру списку в фрагменті
 */
class FragmentAdapter(fa: FragmentActivity, private val list: List<Fragment>) :
    FragmentStateAdapter(fa) {
    override fun getItemCount(): Int {
        return list.size
    }
    //


    /*
    Повернення вибраного фрагменту
     */

    override fun createFragment(position: Int): Fragment {
        return list[position]
    }
}