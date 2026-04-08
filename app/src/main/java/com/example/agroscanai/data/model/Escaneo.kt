package com.example.agroscanai.data.model

data class Escaneo(
    val id: Int = 0,
    val cultivoId: Int = 0,
    val fecha: String = "",
    val progreso: Float = 0f,
    val estadoGeneral: EstadoEscaneo = EstadoEscaneo.PENDIENTE,
    val plagasDetectadas: List<Plaga> = emptyList(),
    val nivelHumedad: Float = 0f,
    val nivelNitrogenio: Float = 0f,
    val nivelFosforo: Float = 0f,
    val nivelPotasio: Float = 0f,
    val indiceSalud: Float = 0f,
    val observaciones: String = "",
    val duracionMinutos: Int = 0
)

enum class EstadoEscaneo {
    PENDIENTE,
    EN_PROGRESO,
    COMPLETADO,
    ERROR
}

data class Plaga(
    val id: Int = 0,
    val nombre: String = "",
    val descripcion: String = "",
    val severidad: SeveridadPlaga = SeveridadPlaga.LEVE,
    val tratamiento: String = "",
    val productoRecomendado: String = "",
    val areaAfectada: Float = 0f
)

enum class SeveridadPlaga {
    LEVE,
    MODERADA,
    GRAVE
}