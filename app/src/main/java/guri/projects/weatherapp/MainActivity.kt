package guri.projects.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import guri.projects.weatherapp.Models.WeatherResponse
import guri.projects.weatherapp.databinding.ActivityMainBinding
import guri.projects.weatherapp.network.WeatherService
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity()
{

    private var binding : ActivityMainBinding? = null

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        if(!isLocationEnabled()) {
            Toast.makeText(this, "Location is OFF", Toast.LENGTH_LONG).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        else
        {
            Dexter.withActivity(this)
                .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
                .withListener(object : MultiplePermissionsListener
                {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                        if (report!!.areAllPermissionsGranted())
                        {
                            requestLocationData()
                        }

                        if(report.isAnyPermissionPermanentlyDenied)
                        {
                            Toast.makeText(
                                this@MainActivity,
                                "Permission Denied",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread().check()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData(){

        val mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )

    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastloc: Location = locationResult.lastLocation
            val latitude = lastloc.latitude

            val longitude = lastloc.longitude

            getLocationWeatherDetails(latitude, longitude)
        }
    }

   private fun isLocationEnabled() : Boolean {

       val locationManager : LocationManager =
           getSystemService(Context.LOCATION_SERVICE) as LocationManager

       return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
               || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

   }


    private fun showRationalDialogForPermissions()
    {
        AlertDialog.Builder(this)
            .setMessage("Kindly On the Settings ")
            .setPositiveButton(
                "GO TO SETTINGS") { _, _ ->

                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)

            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }




    private fun getLocationWeatherDetails(latitude : Double, longitude : Double){

        if(Constants.isNetworkAvailable(this))
        {
            val retrofit : Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()


            val service : WeatherService = retrofit.create<WeatherService>(WeatherService::class.java);

            val listCall : Call<WeatherResponse> = service.getWeather(
                latitude,
                longitude,
                Constants.METRIC_UNIT,
                Constants.APP_ID)

            showCustomProgressDialog()

            listCall.enqueue(object  : Callback<WeatherResponse> {

                @RequiresApi(Build.VERSION_CODES.N)
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    if(response!!.isSuccess)
                    {
                        hideProgressDialog()

                        val weatherList : WeatherResponse = response.body()

                        val weatherResponseJsonString =
                        screen(weatherList)


                        Log.i("Response Result", "$weatherList")
                    }
                    else
                    {
                        val rc = response.code()
                        when(rc)
                        {
                            400 -> {
                                Log.e("Error 400", "Bad Connection")
                             }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else ->{
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable?) {
                    Log.e("Error", t!!.message.toString())
                    hideProgressDialog()
                }

            })
        }
        else
        {
            Toast.makeText(this, "Problem", Toast.LENGTH_LONG).show()
        }
    }


    private fun showCustomProgressDialog(){

        mProgressDialog = Dialog(this)

        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        mProgressDialog!!.show()

    }

    private fun hideProgressDialog(){

        if(mProgressDialog != null)
        {
            mProgressDialog!!.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun screen(weatherList : WeatherResponse)
    {
        for(i in weatherList.weather.indices)
        {
            binding?.tvMain?.text = weatherList.weather[i].main
            binding?.tvMainDescription?.text = weatherList.weather[i].desciption
            binding?.tvTemp?.text = weatherList.main.temp.toString() + unit(application.resources.configuration.locales.toString())


            binding?.tvSunriseTime?.text = unixTime(weatherList.sys.sunrise)
            binding?.tvSunsetTime?.text = unixTime(weatherList.sys.sunset)


            binding?.tvHumidity?.text = weatherList.main.humidity.toString() + " per cent"
            binding?.tvMin?.text = weatherList.main.temp_min.toString()
            binding?.tvMax?.text = weatherList.main.temp_max.toString()
            binding?.tvSpeed?.text = weatherList.wind.speed.toString()
            binding?.tvCountry?.text = weatherList.sys.country
            binding?.tvName?.text = weatherList.name


            when(weatherList.weather[i].icon)
            {
                "01d" -> binding?.ivMain?.setImageResource(R.drawable.sunny)
                "02d", "03d", "04d", "04n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                "10d" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                "11d" -> binding?.ivMain?.setImageResource(R.drawable.storm)
                "13d" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
                "01n", "02n", "03n", "10n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                "11n" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                "13n" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)

            }
        }
    }

    private fun unit(value : String) : String?
    {
        var value = " C"

        if("US" == value || "LR" == value || "MM" == value)
        {
            value = " F"
        }

        return value
    }


    private fun unixTime(timex : Long) : String? {

        val date = Date(timex*1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()

        return sdf.format(date)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return when(item.itemId)
        {

            R.id.refresh -> { requestLocationData()
                true}

            else ->  super.onOptionsItemSelected(item)

        }
    }
}