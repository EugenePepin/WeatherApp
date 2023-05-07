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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherapp.DialogManager
import com.example.weatherapp.DialogManager.incorrectCityName
import com.example.weatherapp.MainViewModel
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


class MainFragment : Fragment() {

    private lateinit var clientLocation: FusedLocationProviderClient
    /*
    функції для перевірки доступу до місця розташування
    */
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
    //

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

    private fun isLocationEnabled(): Boolean{

         val lm = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

    }

    private fun checkLocationMessage(){
        if(isLocationEnabled()){
            getLocation()
        } else {
            DialogManager.locationDialog(requireContext(), object: DialogManager.Listener{
                override fun onClick(name: String?) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            })
        }

    }

    /*
   витягаємо дані з WeatherAPI
    */
    private fun requestCurrentWeatherData(city: String) {
        val url =
            "https://api.weatherapi.com/v1/forecast.json?" + "key=$API_KEY" + "&q=$city&days=5&aqi=no&alerst=no"
        val quene = Volley.newRequestQueue(context)
        val request = StringRequest(Request.Method.GET, url, { result ->
            parseWeatherData(result)
        }, { error ->
            incorrectCityName(requireContext())
        }

        )
        quene.add(request)
    }
    //


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
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        init()
        updateCurrentCard()


    }

    private fun init() = with(binding) {
        clientLocation = LocationServices.getFusedLocationProviderClient(requireContext())
        val adapter = FragmentAdapter(activity as FragmentActivity, fragmentList)

        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = tabList[pos]
        }.attach()
        syncButton.setOnClickListener{
            tabLayout.selectTab(tabLayout.getTabAt(0))
            checkLocationMessage()
        }
       searchButton.setOnClickListener {
            DialogManager.searchByNameDialog(requireContext(), object : DialogManager.Listener{
                override fun onClick(name: String?) {
                    name?.let { it1 -> requestCurrentWeatherData(it1) }
                }
            })
            }
        }








    private fun parseWeatherData(result: String) {
        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentWeatherData(mainObject, list[0])
    }

    private fun parseDays(mainObject: JSONObject): List<WeatherData> {
        val list = ArrayList<WeatherData>()
        val daysArray = mainObject.getJSONObject("forecast")
            .getJSONArray("forecastday")
        val name =  mainObject.getJSONObject("location").getString("name")
        for (i in 0 until daysArray.length()){
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

    //
    private fun parseCurrentWeatherData(mainObject: JSONObject, weatherTempItem: WeatherData) {


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


    /*
     додаємо дані в TextView
    */

    private fun updateCurrentCard() = with(binding) {
        dataModel.liveDataCurrent.observe(viewLifecycleOwner) {
            val tempMaxMin = "${it.maxTempData}°С / ${it.minTempData}"
            cityNameTextView.text = it.cityNameData
            currentTempTextView.text = it.currentTempData.ifEmpty {tempMaxMin}
            conditionStatusTextView.text = it.conditionStatusData
            tempMaxMinTextView.text = if(it.currentTempData.isEmpty()) "" else "${tempMaxMin}°С"



            Picasso.get().load("https:" + it.iconUrl).into(weatherImage)
        }

    }

    //
    companion object {

        @JvmStatic
        fun newInstance() = MainFragment()
        const val API_KEY = "d106cfd856664bce921174757231304"

    }
}



