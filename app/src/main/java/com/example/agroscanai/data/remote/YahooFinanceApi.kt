package com.example.agroscanai.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface YahooFinanceApi {

    @GET
    suspend fun getCotizacion(
        @Url url: String
    ): YahooChartResponse

    companion object {
        private const val BASE = "https://query1.finance.yahoo.com/v8/finance/chart/"

        fun urlParaSimbolo(symbol: String) =
            "$BASE$symbol?interval=1d&range=5d"

        val instance: YahooFinanceApi by lazy {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36"
                        )
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(req)
                }
                .build()

            Retrofit.Builder()
                .baseUrl("https://query1.finance.yahoo.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YahooFinanceApi::class.java)
        }
    }
}
