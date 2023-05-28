package com.example.weatherapp.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import com.android.volley.ClientError
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherapp.DialogManager
import com.example.weatherapp.DialogManager.incorrectCityName
import com.example.weatherapp.DialogManager.noConnection
import com.example.weatherapp.MainViewModel
import com.example.weatherapp.R
import com.example.weatherapp.WeatherData
import com.example.weatherapp.adapter.FragmentAdapter
import com.example.weatherapp.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar


class MainFragment : Fragment() {
    private lateinit var lastUpdated: String
    private lateinit var clientLocation: FusedLocationProviderClient
    private lateinit var pLauncher: ActivityResultLauncher<String>
    private lateinit var binding: FragmentMainBinding
    private val dataModel: MainViewModel by activityViewModels()
    private val fragmentList = listOf(
        HoursFragment.NewInstance(), DaysFragment.NewInstance()
    )
    private val tabList = listOf(
        "Hours", "Days"
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        checkLocationMessage()
        changeBackgroundFromTimeFromStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        init()
        updateCurrentCard()
        changeBackgroundFromTimeFromStart()


    }

    private fun init() = with(binding) {
        clientLocation = LocationServices.getFusedLocationProviderClient(requireContext())
        val adapter = FragmentAdapter(activity as FragmentActivity, fragmentList)

        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = tabList[pos]
        }.attach()
        //функіонал для кнопки синхронізації
        syncButton.setOnClickListener {
            tabLayout.selectTab(tabLayout.getTabAt(0))
            checkLocationMessage()
        }

        //функіонал для поля з текстом
        editCityText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val cityName = editCityText.text.toString()
                requestCurrentWeatherData(cityName)
                changeBackgroundFromTime()
                editCityText.setText("")

                val inputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(editCityText.windowToken, 0)

            }
            true
        }
    }



    //функції для перевірки доступу до місця розташування

    private fun checkPermission() {
        if (!permissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }


    private fun permissionListener() {
        pLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            Toast.makeText(activity, "Permission is $it", Toast.LENGTH_LONG).show()
        }
    }


    //додаємо місто з місця розташування
    private fun getLocation() {

        val ct = CancellationTokenSource()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        clientLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, ct.token)
            .addOnCompleteListener {
                requestCurrentWeatherData("${it.result.latitude},${it.result.longitude}")
            }

    }

    //перевірка, чи включена функція місця розташування
    private fun isLocationEnabled(): Boolean {

        val lm = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

    }

    //виведення AlertDialog при вимкненому розташуванні, і перекидування до налаштувань
    private fun checkLocationMessage() {
        if (isLocationEnabled()) {
            getLocation()
        } else {
            DialogManager.locationDialog(requireContext(), object : DialogManager.Listener {
                override fun onClick(name: String?) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            })
        }

    }

    //зміна фону залежно від часу користувача
    private fun changeBackgroundFromTimeFromStart() = with(binding) {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour in 18..23 || currentHour in 0..6) {
            imageView.setImageResource(R.drawable.night_background)
        } else if (currentHour in 7..17) {
            imageView.setImageResource(R.drawable.day_background)
        }

    }

    //зміна часу залежно від часу сервера (для пошуку)
    private fun changeBackgroundFromTime() = with(binding) {
        dataModel.liveDataCurrent.observe(viewLifecycleOwner) {

            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
            val outputFormat = SimpleDateFormat("HH")
            val date = inputFormat.parse(lastUpdated)
            val currentHour = outputFormat.format(date).toInt()
            Log.d("VolleyError", "Volley error is: $currentHour")
            if (currentHour in 18..23 || currentHour in 0..6) {
                imageView.setImageResource(R.drawable.night_background)
            } else if (currentHour in 7..17) {
                imageView.setImageResource(R.drawable.day_background)
            }
        }
    }


    //робимо запит до WeatherAPI

    private fun requestCurrentWeatherData(city: String) {
        val url =
            "https://api.weatherapi.com/v1/forecast.json?key=$API_KEY&q=$city&days=5&aqi=no&alerst=no"
        val quene = Volley.newRequestQueue(context)
        val request = StringRequest(Request.Method.GET, url, { result ->
            parseWeatherData(result)
        }, { error ->
            when (error) {
                is NoConnectionError -> {
                    noConnection(requireContext())
                }

                is ClientError -> {
                    incorrectCityName(requireContext())
                }

                else -> {
                    Log.d("VolleyError", "Volley error is: $error")
                }
            }
        })
        quene.add(request)
    }

    private fun parseWeatherData(result: String) {
        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentWeatherData(mainObject, list[0])
    }

    //витягаємо дані до картки з актуальною погодою
    private fun parseCurrentWeatherData(mainObject: JSONObject, weatherTempItem: WeatherData) {

        lastUpdated = mainObject.getJSONObject("current").getString("last_updated")
        sharedViewModel.lastUpdated = lastUpdated
        val item = WeatherData(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current").getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c").toFloat().toInt().toString(),
            mainObject.getJSONObject("current").getJSONObject("condition").getString("icon"),
            weatherTempItem.maxTempData,
            weatherTempItem.minTempData,
            weatherTempItem.hoursData
        )
        dataModel.liveDataCurrent.value = item
    }

    private val sharedViewModel: SharedViewModel by activityViewModels()
    class SharedViewModel : ViewModel() {
        var lastUpdated: String = ""
    }

    //витягаємо дані для днів
    private fun parseDays(mainObject: JSONObject): List<WeatherData> {
        val list = ArrayList<WeatherData>()
        val daysArray = mainObject.getJSONObject("forecast")
            .getJSONArray("forecastday")
        val name = mainObject.getJSONObject("location").getString("name")
        for (i in 0 until daysArray.length()) {
            val day = daysArray[i] as JSONObject
            val item = WeatherData(
                name,
                day.getString("date"),
                day.getJSONObject("day").getJSONObject("condition")
                    .getString("text"),
                "",
                day.getJSONObject("day").getJSONObject("condition")
                    .getString("icon"),
                day.getJSONObject("day").getString("maxtemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getString("mintemp_c").toFloat().toInt().toString(),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        dataModel.liveDataList.value = list
        return list
    }


    //додаємо дані до картки з актуальною погодою


    private fun updateCurrentCard() = with(binding) {
        dataModel.liveDataCurrent.observe(viewLifecycleOwner) {
            dateAndTimeTextView.text =
                it.dateAndTimeData.substringBefore(" ").replace("-", "/").split("/").reversed()
                    .joinToString("/")
            val tempMaxMin = "${it.maxTempData}°С /${it.minTempData}"
            cityNameTextView.text =
                String(it.cityNameData.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
            currentTempTextView.text = it.currentTempData.ifEmpty { tempMaxMin } + "°C"
            conditionStatusTextView.text = it.conditionStatusData
            tempMaxMinTextView.text = if (it.currentTempData.isEmpty()) "" else "$tempMaxMin°C"
            //при пустому полі tempMaxMin - переводить до вкладки hours
            if (tempMaxMinTextView.text.isEmpty()) {
                tabLayout.selectTab(tabLayout.getTabAt(0))
            }
            Picasso.get().load("https:" + it.iconUrl).into(weatherImage)
        }

    }

    //
    companion object {

        @JvmStatic
        fun newInstance() = MainFragment()

        //ключ до мого WeatherApi
        const val API_KEY = "d106cfd856664bce921174757231304"

    }
}




