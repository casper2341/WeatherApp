package guri.projects.weatherapp.Models

import java.io.Serializable

data class Weather(
    val id : Int,
    val main: String,
    val desciption: String,
    val icon: String
) : Serializable
