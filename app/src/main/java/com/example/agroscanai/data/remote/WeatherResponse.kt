package com.example.agroscanai.data.remote

import com.google.gson.annotations.SerializedName

data class OpenMeteoResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "",
    val current: CurrentWeatherData = CurrentWeatherData(),
    val daily: DailyWeatherData = DailyWeatherData()
)

data class CurrentWeatherData(
    @SerializedName("temperature_2m")         val temperatura: Float = 0f,
    @SerializedName("relative_humidity_2m")   val humedad: Int = 0,
    @SerializedName("weather_code")           val weatherCode: Int = 0,
    @SerializedName("wind_speed_10m")         val velocidadViento: Float = 0f,
    @SerializedName("precipitation_probability") val probabilidadLluvia: Int = 0
)

data class DailyWeatherData(
    val time: List<String> = emptyList(),
    @SerializedName("temperature_2m_max")           val tempMax: List<Float> = emptyList(),
    @SerializedName("temperature_2m_min")           val tempMin: List<Float> = emptyList(),
    @SerializedName("weather_code")                 val weatherCodes: List<Int> = emptyList(),
    @SerializedName("precipitation_probability_max") val precipProb: List<Int> = emptyList(),
    @SerializedName("uv_index_max")                 val uvIndex: List<Float> = emptyList()
)
