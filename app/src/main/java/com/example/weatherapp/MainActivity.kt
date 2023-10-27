package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.data.RvAdapter
import com.example.weatherapp.data.forecastModels.ForecastData
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.databinding.BottomSheetLayoutBinding
import com.example.weatherapp.utils.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sheetBinding: BottomSheetLayoutBinding
    private lateinit var dialog: BottomSheetDialog
    lateinit var pollutionFragment: PolloutionFragment
    private  var city="cairo"
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        sheetBinding = BottomSheetLayoutBinding.inflate(layoutInflater)
        pollutionFragment = PolloutionFragment()
        dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        dialog.setContentView(sheetBinding.root)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        binding.serchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    city = query
                    getCurrentWeather(city)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        fetchLocation()
        getCurrentWeather(city)
        binding.tvFiveDays.setOnClickListener {
            openDialog()
        }
        binding.myLocation.setOnClickListener {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        val task: Task<Location> = fusedLocationProviderClient.lastLocation
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        task.addOnSuccessListener {
            val geoCoder = Geocoder(this, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val callBack = object : Geocoder.GeocodeListener {
                    override fun onGeocode(p0: MutableList<Address>) {
                        city = p0[0].locality
                    }
                }
                geoCoder.getFromLocation(it.latitude, it.longitude, 1, callBack)
            } else {
                val address =
                    geoCoder.getFromLocation(it.latitude, it.longitude, 1) as List<Address>
                city = address[0].locality
            }
            Log.i("seif",city)
            getCurrentWeather(city)
        }
    }

    private fun openDialog() {
        getForecast(city)

        sheetBinding.rvForecast.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@MainActivity, 1, RecyclerView.HORIZONTAL, false)
        }
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    private fun getForecast(city: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentForecast(
                    city,
                    "metric",
                    getString(R.string.api_key)
                )
            } catch (e: IOException) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    val data = response.body()!!
                    var forecastArray = arrayListOf<ForecastData>()
                    forecastArray = data.list as ArrayList<ForecastData>
                    val adapter = RvAdapter(forecastArray)
                    sheetBinding.rvForecast.adapter = adapter
                    sheetBinding.tvSheet.text = "Five days forecast in ${data.city.name}"
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    private fun getCurrentWeather(city: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentWeather(
                    city,
                    "metric",
                    getString(R.string.api_key)
                )
            } catch (e: IOException) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    val data = response.body()!!
                    val iconId = data.weather[0].icon

                    val iconUrl = "https://openweathermap.org/img/wn/$iconId@4x.png"

                    Picasso.get().load(iconUrl).into(binding.imageView)
                    Log.i("seif", iconUrl)

                    binding.tvSunrise.text =
                        SimpleDateFormat(
                            "hh:mm a",
                            Locale.ENGLISH
                        ).format(Date(data.sys.sunrise.toLong() * 1000))
                    binding.tvSunset.text = dateFormatConverter(data.sys.sunset.toLong())

                    binding.apply {
                        tvStatus.text = data.weather[0].description
                        tvWindspeed.text = data.wind.speed.toString() + "KM/H"
                        tvLocation.text = "${data.name} ,${data.sys.country}"
                        tvTemp.text = data.main.temp.toInt().toString() + "°C"
                        tvMin.text = "Min Temp :${data.main.temp_min.toInt().toString()}°C"
                        tvMax.text = "Max Temp :${data.main.temp_max.toInt().toString()}°C"
                        tvFeelsLike.text = "Feels like:${data.main.feels_like.toInt().toString()}°C"
                        tvHumidity.text = data.main.humidity.toString() + "%"
                        tvPressure.text = data.main.pressure.toString() + "hPa"
                        tvUpdatedTime.text = "Last Update: ${
                            SimpleDateFormat(
                                "hh:mm a",
                                Locale.ENGLISH
                            ).format(data.dt * 1000)
                        }"
                        getPollution(data.coord.lat, data.coord.lon)

                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getPollution(lat: Double, lon: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getPollution(
                    lat,
                    lon,
                    "metric",
                    getString(R.string.api_key)
                )
            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                Log.i("seif", e.message.toString())
                return@launch
            } catch (e: HttpException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                Log.i("seif", e.message.toString())
                return@launch
            }
            Log.i("seif", "$lat \n $lon")
            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    val data = response.body()!!
                    val num = data.list[0].main.aqi
                    Log.i("seif", num.toString())
                    binding.tvAirquality.text =
                        when (num) {
                            1 -> "Good"
                            2 -> "Fair"
                            3 -> "Moderate"
                            4 -> "Poor"
                            5 -> "Very Poor"
                            else -> "no data"
                        }
                    binding.linearPollution.setOnClickListener {
                        val bundle = Bundle()
                        bundle.putDouble("co", data.list[0].components.co)
                        bundle.putDouble("nh3", data.list[0].components.nh3)
                        bundle.putDouble("no", data.list[0].components.no)
                        bundle.putDouble("no2", data.list[0].components.no2)
                        bundle.putDouble("o3", data.list[0].components.o3)
                        bundle.putDouble("pm10", data.list[0].components.pm10)
                        bundle.putDouble("pm2_5", data.list[0].components.pm2_5)
                        bundle.putDouble("so2", data.list[0].components.so2)

                        pollutionFragment.arguments = bundle

                        supportFragmentManager.beginTransaction().apply {
                            replace(R.id.frame_trans, pollutionFragment)
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                }
            }
        }
    }

    private fun dateFormatConverter(date: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(date * 1000)

    }

}



