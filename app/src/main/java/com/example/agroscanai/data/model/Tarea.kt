package com.example.agroscanai.data.model

data class Tarea(
    val id: Int = 0,
    val cultivoId: Int = 0,
    val titulo: String = "",
    val descripcion: String = "",
    val fecha: String = "",
    val hora: String = "",
    val tipo: TipoTarea = TipoTarea.MANUAL,
    val completada: Boolean = false,
    val prioridad: PrioridadTarea = PrioridadTarea.MEDIA
)

enum class TipoTarea {
    MANUAL,
    RIEGO,
    FERTILIZACION,
    FUMIGACION,
    COSECHA,
    ESCANEO,
    RECOMENDACION
}

enum class PrioridadTarea {
    BAJA,
    MEDIA,
    ALTA,
    URGENTE
}