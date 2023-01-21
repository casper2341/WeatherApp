package guri.projects.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Build.VERSION_CODES.M

object Constants
{
    const val APP_ID : String = "b4cfd105dfb26eb406e24358029c7a94"

    const val BASE_URL : String = "http://api.openweathermap.org/data/"

    const val METRIC_UNIT : String = "metric"

    fun isNetworkAvailable(context: Context) : Boolean{

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            val networkinfo = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(networkinfo) ?: return false

            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        }
        else
        {
            val networkinfo = connectivityManager.activeNetworkInfo
            return networkinfo != null && networkinfo.isConnectedOrConnecting
        }
    }
}