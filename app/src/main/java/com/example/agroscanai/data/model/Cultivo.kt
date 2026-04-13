package com.example.agroscanai.data.model

data class Cultivo(
    val id: String = "",
    val nombre: String = "",
    val tipoCultivo: String = "",
    val variedadSemilla: String = "",
    val hectareas: Double = 0.0,
    val fechaSiembra: String = "",
    val ubicacion: String = "",
    val estado: String = EstadoCultivo.SIN_ESCANEO.name,
    val humedadPromedio: Float = 0f,
    val temperatura: Float = 0f,
    val nitrogenio: Float = 0f,
    val fosforo: Float = 0f,
    val potasio: Float = 0f,
    val indiceSalud: Float = 0f,
    val plagasDetectadas: Boolean = false,
    val ultimoEscaneo: String = "",
    val imagenUrl: String = ""
) {
    fun estadoEnum(): EstadoCultivo = try {
        EstadoCultivo.valueOf(estado)
    } catch (e: Exception) {
        EstadoCultivo.SIN_ESCANEO
    }

    fun estadoTexto(): String = when (estadoEnum()) {
        EstadoCultivo.SALUDABLE -> "Óptima"
        EstadoCultivo.ALERTA    -> "En riesgo"
        EstadoCultivo.CRITICO   -> "Crítica"
        EstadoCultivo.SIN_ESCANEO -> "Sin escaneo"
    }

    fun descripcionSalud(): String = when (estadoEnum()) {
        EstadoCultivo.SALUDABLE ->
            "La parcela se encuentra en excelente estado de salud, no necesita aplicar ningún tipo de producto en este momento."
        EstadoCultivo.ALERTA ->
            "La parcela presenta algunas irregularidades. Se recomienda revisar el riego y aplicar nutrientes preventivos."
        EstadoCultivo.CRITICO ->
            "La parcela está en estado crítico. Se requiere atención inmediata para evitar pérdidas en el cultivo."
        EstadoCultivo.SIN_ESCANEO ->
            "Esta parcela aún no ha sido escaneada. Realiza un escaneo para obtener información sobre su estado."
    }
}

enum class EstadoCultivo {
    SALUDABLE,
    ALERTA,
    CRITICO,
    SIN_ESCANEO
}
