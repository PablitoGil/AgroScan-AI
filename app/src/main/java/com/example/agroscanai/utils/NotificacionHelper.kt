package com.example.agroscanai.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.agroscanai.R
import kotlin.random.Random

object NotificacionHelper {

    private const val CANAL_ESCANEOS = "agroscan_escaneos"
    private const val CANAL_ALERTAS  = "agroscan_alertas"

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    CANAL_ESCANEOS,
                    "Escaneos completados",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones cuando un escaneo de parcela finaliza"
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CANAL_ALERTAS,
                    "Alertas de cultivos",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas urgentes: plagas detectadas o estado crítico"
                    enableVibration(true)
                }
            )
        }
    }

    fun notificarEscaneoCompleto(
        context: Context,
        nombreCultivo: String,
        estado: String,           // "CRITICO" | "ALERTA" | "SALUDABLE"
        plagasDetectadas: Boolean
    ) {
        val (canalId, titulo, cuerpo) = when {
            plagasDetectadas -> Triple(
                CANAL_ALERTAS,
                "⚠️ Plagas detectadas en $nombreCultivo",
                "Se detectaron plagas en el escaneo. Revisa el dashboard de salud."
            )
            estado == "CRITICO" -> Triple(
                CANAL_ALERTAS,
                "🔴 Estado crítico en $nombreCultivo",
                "El cultivo requiere atención urgente. Revisa el reporte."
            )
            estado == "ALERTA" -> Triple(
                CANAL_ESCANEOS,
                "🟡 Alerta en $nombreCultivo",
                "Se encontraron parámetros fuera de rango. Revisa el dashboard."
            )
            else -> Triple(
                CANAL_ESCANEOS,
                "✅ Escaneo completado: $nombreCultivo",
                "El cultivo está en buen estado. Índice de salud actualizado."
            )
        }

        val notificacion = NotificationCompat.Builder(context, canalId)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(
                if (canalId == CANAL_ALERTAS) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(Random.nextInt(10_000), notificacion)
        } catch (_: SecurityException) {
            // Permiso POST_NOTIFICATIONS no otorgado — sin acción
        }
    }
}
