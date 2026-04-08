package com.example.agroscanai.data.model

data class Clima(
    val fecha: String = "",
    val temperatura: Float = 0f,
    val temperaturaMin: Float = 0f,
    val temperaturaMax: Float = 0f,
    val humedad: Int = 0,
    val descripcion: String = "",
    val iconoCodigo: String = "",
    val velocidadViento: Float = 0f,
    val probabilidadLluvia: Int = 0,
    val uvIndex: Int = 0,
    val region: String = ""
)

data class PronosticoClima(
    val diasPronostico: List<Clima> = emptyList(),
    val recomendaciones: List<String> = emptyList()
)