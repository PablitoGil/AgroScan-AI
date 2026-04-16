package com.example.agroscanai.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun getPronostico(
        @Query("latitude")      latitude: Double,
        @Query("longitude")     longitude: Double,
        @Query("current")       current: String =
            "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,precipitation_probability",
        @Query("daily")         daily: String =
            "temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,uv_index_max",
        @Query("timezone")      timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoResponse

    companion object {
        val instance: OpenMeteoApi by lazy {
            Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenMeteoApi::class.java)
        }
    }
}
