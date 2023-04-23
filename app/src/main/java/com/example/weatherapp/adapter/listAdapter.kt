package com.example.weatherapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.WeatherData
import com.example.weatherapp.databinding.ListItemBinding
import com.squareup.picasso.Picasso

class listAdapter(val listener: Listener?) : ListAdapter<WeatherData, listAdapter.Holder>(Comparator()) {
    class Holder(view: View, val listener: Listener?) : RecyclerView.ViewHolder(view) {
        val binding = ListItemBinding.bind(view)
        var itemTemp : WeatherData? = null
        init {
            itemView.setOnClickListener{
                itemTemp?.let { it1 -> listener?.onClick(it1) }
            }
        }
        fun bind(item: WeatherData) = with(binding) {
            itemTemp = item
            //val currentTemp = "${item.currentTempData}°С"
            dateTextView.text = item.dateAndTimeData
            tempTextView.text = item.currentTempData.ifEmpty{ "${item.maxTempData}°С / ${item.minTempData}°С"}
            conditionTextView.text = item.conditionStatusData
            Picasso.get().load("https:" + item.iconUrl).into(weatherIcon)
        }

    }

    /*
    порівняння інформації в списку, і чи потрібно її змінюванти
     */
    class Comparator : DiffUtil.ItemCallback<WeatherData>() {
        override fun areItemsTheSame(oldItem: WeatherData, newItem: WeatherData): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: WeatherData, newItem: WeatherData): Boolean {
            return oldItem == newItem
        }

    }

    /*
    заповнення списку від 0 позиції
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return Holder(view, listener)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }
  interface Listener{
      fun onClick(item: WeatherData)

  }

}