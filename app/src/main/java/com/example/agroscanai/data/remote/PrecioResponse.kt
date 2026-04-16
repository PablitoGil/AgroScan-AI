package com.example.agroscanai.data.remote

import com.google.gson.annotations.SerializedName

data class YahooChartResponse(
    val chart: YahooChart = YahooChart()
)

data class YahooChart(
    val result: List<YahooChartResult>? = null,
    val error: Any? = null
)

data class YahooChartResult(
    val meta: YahooMeta = YahooMeta()
)

data class YahooMeta(
    val currency: String = "",
    val symbol: String = "",
    @SerializedName("regularMarketPrice")    val precioActual: Double = 0.0,
    @SerializedName("previousClose")         val cierrePrevio: Double = 0.0,
    @SerializedName("regularMarketDayHigh")  val maxDia: Double = 0.0,
    @SerializedName("regularMarketDayLow")   val minDia: Double = 0.0,
    val shortName: String = ""
)
