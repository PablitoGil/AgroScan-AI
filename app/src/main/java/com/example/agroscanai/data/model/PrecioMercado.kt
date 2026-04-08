package com.example.agroscanai.data.model

data class PrecioMercado(
    val id: Int = 0,
    val grano: String = "",
    val precioActual: Double = 0.0,
    val precioMinimo: Double = 0.0,
    val precioMaximo: Double = 0.0,
    val unidad: String = "tonelada",
    val region: String = "",
    val fecha: String = "",
    val tendencia: TendenciaPrecio = TendenciaPrecio.ESTABLE,
    val recomendacion: String = ""
)

enum class TendenciaPrecio {
    SUBIENDO,
    BAJANDO,
    ESTABLE
}

data class HistorialPrecio(
    val fecha: String = "",
    val precio: Double = 0.0
)