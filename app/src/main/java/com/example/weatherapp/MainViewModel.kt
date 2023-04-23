package com.example.weatherapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
class MainViewModel : ViewModel() {
    val liveDataCurrent = MutableLiveData<WeatherData>()
    val liveDataList = MutableLiveData<List<WeatherData>>()
}