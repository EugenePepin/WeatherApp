package com.example.weatherapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.MainViewModel
import com.example.weatherapp.WeatherData
import com.example.weatherapp.adapter.listAdapter
import com.example.weatherapp.databinding.FragmentHoursBinding
import org.json.JSONArray
import org.json.JSONObject


class HoursFragment : Fragment() {
    private lateinit var binding: FragmentHoursBinding
    private lateinit var adapter: listAdapter
    private val model: MainViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHoursBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        model.liveDataCurrent.observe(viewLifecycleOwner){
            adapter.submitList(getHoursList(it))
        }
    }
    private fun initRecyclerView() = with(binding){
        hoursRecyclerView.layoutManager = LinearLayoutManager(activity)
        adapter = listAdapter(null)
        hoursRecyclerView.adapter = adapter

    }

    private fun getHoursList(weatherItem: WeatherData): List<WeatherData>{
        val hoursArray = JSONArray(weatherItem.hoursData)
        val list = ArrayList<WeatherData>()
        for (i in 0 until hoursArray.length()){
            val item = WeatherData(
                weatherItem.cityNameData,
                (hoursArray[i] as JSONObject).getString("time"),
                (hoursArray[i] as JSONObject).getJSONObject("condition").getString("text"),
                (hoursArray[i] as JSONObject).getString("temp_c").toFloat().toInt().toString(),
                (hoursArray[i] as JSONObject).getJSONObject("condition").getString("icon"),
                "",
                "",
                ""
            )
            list.add(item)
        }
        return list
    }
    companion object {

        @JvmStatic

        fun NewInstance() = HoursFragment()
    }
}
