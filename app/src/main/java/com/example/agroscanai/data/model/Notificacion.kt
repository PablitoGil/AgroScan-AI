package com.example.agroscanai.data.model

data class Notificacion(
    val id: Int = 0,
    val titulo: String = "",
    val mensaje: String = "",
    val fecha: String = "",
    val hora: String = "",
    val tipo: TipoNotificacion = TipoNotificacion.INFO,
    val leida: Boolean = false,
    val cultivoId: Int? = null
)

enum class TipoNotificacion {
    INFO,
    ALERTA,
    CRITICA,
    TAREA,
    RECOMENDACION,
    CLIMA
}