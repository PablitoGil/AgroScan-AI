package com.example.agroscanai.data.model

data class Tarea(
    val id: String = "",
    val cultivoId: String = "",
    val cultivoNombre: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val fecha: String = "",
    val hora: String = "",
    val tipo: String = TipoTarea.MANUAL.name,
    val completada: Boolean = false,
    val prioridad: String = PrioridadTarea.MEDIA.name
) {
    fun tipoEnum(): TipoTarea = try {
        TipoTarea.valueOf(tipo)
    } catch (e: Exception) {
        TipoTarea.MANUAL
    }

    fun prioridadEnum(): PrioridadTarea = try {
        PrioridadTarea.valueOf(prioridad)
    } catch (e: Exception) {
        PrioridadTarea.MEDIA
    }
}

enum class TipoTarea {
    MANUAL,
    RIEGO,
    FERTILIZACION,
    FUMIGACION,
    COSECHA,
    ESCANEO,
    RECOMENDACION,
    PODA
}

enum class PrioridadTarea {
    BAJA,
    MEDIA,
    ALTA,
    URGENTE
}
