package com.example.agroscanai.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel

// ── Reglas de análisis de IA (basadas en datos reales de escaneos) ────────────

private data class RecomendacionIA(
    val texto: String,
    val tipo: TipoRecom
)

private enum class TipoRecom { RIEGO, PLAGAS, FERTILIZANTE, OPTIMO, ATENCION }

private fun generarAnalisisIA(cultivo: Cultivo): String {
    val partes = mutableListOf<String>()

    when {
        cultivo.estadoEnum() == EstadoCultivo.SIN_ESCANEO ->
            return "Sin datos de escaneo disponibles. Realiza un escaneo con el dron para obtener análisis detallado."
        cultivo.estadoEnum() == EstadoCultivo.CRITICO ->
            partes += "Estado crítico detectado. Se requiere intervención inmediata."
        cultivo.estadoEnum() == EstadoCultivo.ALERTA ->
            partes += "La parcela presenta irregularidades. Monitoreo frecuente recomendado."
        else ->
            partes += "La parcela se encuentra en buen estado de salud."
    }

    when {
        cultivo.humedadPromedio < 10f -> partes += "Humedad muy baja (${cultivo.humedadPromedio.toInt()}%): riesgo de estrés hídrico severo."
        cultivo.humedadPromedio < 20f -> partes += "Humedad baja (${cultivo.humedadPromedio.toInt()}%): se recomienda riego inmediato."
        cultivo.humedadPromedio < 30f -> partes += "Humedad ligeramente baja (${cultivo.humedadPromedio.toInt()}%): monitorear el riego."
        cultivo.humedadPromedio > 75f -> partes += "Humedad excesiva (${cultivo.humedadPromedio.toInt()}%): riesgo de hongos y pudrición radicular."
        else                          -> partes += "Humedad óptima (${cultivo.humedadPromedio.toInt()}%)."
    }

    when {
        cultivo.nitrogenio < 30f -> partes += "Nitrógeno muy bajo (${cultivo.nitrogenio.toInt()}%): fertilización urgente necesaria."
        cultivo.nitrogenio < 50f -> partes += "Nitrógeno bajo (${cultivo.nitrogenio.toInt()}%): aplicar fertilizante nitrogenado."
        cultivo.nitrogenio > 85f -> partes += "Exceso de nitrógeno (${cultivo.nitrogenio.toInt()}%): reducir fertilización."
        else                     -> partes += "Nitrógeno en niveles adecuados (${cultivo.nitrogenio.toInt()}%)."
    }

    if (cultivo.fosforo < 40f)  partes += "Fósforo bajo (${cultivo.fosforo.toInt()}%): aplicar fosfato para mejorar enraizamiento."
    if (cultivo.potasio < 50f)  partes += "Potasio bajo (${cultivo.potasio.toInt()}%): reforzar con fertilizante potásico."
    if (cultivo.plagasDetectadas) partes += "Presencia de plagas detectada: aplicar tratamiento fitosanitario a la brevedad."

    return partes.joinToString(" ")
}

private fun generarRecomendaciones(cultivo: Cultivo): List<RecomendacionIA> {
    if (cultivo.estadoEnum() == EstadoCultivo.SIN_ESCANEO)
        return listOf(RecomendacionIA("Realiza el primer escaneo con el dron", TipoRecom.ATENCION))

    val lista = mutableListOf<RecomendacionIA>()

    when {
        cultivo.humedadPromedio < 20f ->
            lista += RecomendacionIA("Riego urgente recomendado (humedad: ${cultivo.humedadPromedio.toInt()}%)", TipoRecom.RIEGO)
        cultivo.humedadPromedio in 20f..30f ->
            lista += RecomendacionIA("Monitorear y ajustar frecuencia de riego", TipoRecom.RIEGO)
        else ->
            lista += RecomendacionIA("Riego en niveles óptimos", TipoRecom.OPTIMO)
    }

    when {
        cultivo.nitrogenio < 50f || cultivo.fosforo < 40f || cultivo.potasio < 50f ->
            lista += RecomendacionIA("Aplicar fertilizante NPK balanceado", TipoRecom.FERTILIZANTE)
        else ->
            lista += RecomendacionIA("Nutrientes en equilibrio, mantener plan actual", TipoRecom.OPTIMO)
    }

    if (cultivo.plagasDetectadas)
        lista += RecomendacionIA("Tratamiento fitosanitario urgente", TipoRecom.PLAGAS)
    else
        lista += RecomendacionIA("Sin plagas detectadas, continuar monitoreo preventivo", TipoRecom.OPTIMO)

    return lista
}

private fun construirTextoReporte(cultivos: List<Cultivo>): String {
    val sb = StringBuilder()
    sb.appendLine("=== REPORTE DE IA — AGROSCAN AI ===")
    sb.appendLine("Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
    sb.appendLine()
    sb.appendLine("Resumen general: ${cultivos.size} cultivos analizados")
    sb.appendLine("  Saludables: ${cultivos.count { it.estadoEnum() == EstadoCultivo.SALUDABLE }}")
    sb.appendLine("  En alerta:  ${cultivos.count { it.estadoEnum() == EstadoCultivo.ALERTA }}")
    sb.appendLine("  Críticos:   ${cultivos.count { it.estadoEnum() == EstadoCultivo.CRITICO }}")
    sb.appendLine()
    cultivos.forEach { c ->
        sb.appendLine("--- ${c.nombre} (${c.tipoCultivo}) ---")
        sb.appendLine("Estado: ${c.estadoTexto()}")
        sb.appendLine("Índice de salud: ${c.indiceSalud.toInt()}%")
        sb.appendLine("Análisis: ${generarAnalisisIA(c)}")
        sb.appendLine()
    }
    return sb.toString()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ReportesIAScreen(
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel
) {
    val cultivos by cultivosViewModel.cultivos.collectAsState()
    val context  = LocalContext.current

    val saludables = cultivos.count { it.estadoEnum() == EstadoCultivo.SALUDABLE }
    val alertas    = cultivos.count { it.estadoEnum() == EstadoCultivo.ALERTA    }
    val criticos   = cultivos.count { it.estadoEnum() == EstadoCultivo.CRITICO   }
    val sinEscaneo = cultivos.count { it.estadoEnum() == EstadoCultivo.SIN_ESCANEO }

    Scaffold(
        containerColor = Color(0xFFF0F4F0),
        bottomBar = {
            AgroBottomBar(
                onHomeClick = onHomeClick,
                onNotificacionesClick = onNotificacionesClick,
                onPerfilClick = onPerfilClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(VerdeBosqueOscuro, VerdeBosque)))
                    .padding(bottom = 20.dp)
            ) {
                IconButton(
                    onClick  = onBackClick,
                    modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Psychology,
                        contentDescription = null,
                        tint     = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Reportes de IA", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Análisis basado en datos reales de escaneos",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (cultivos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Psychology, contentDescription = null,
                            tint = GrisMedio, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Sin cultivos registrados", fontSize = 16.sp, color = GrisMedio, fontWeight = FontWeight.SemiBold)
                        Text("Agrega cultivos y realiza escaneos para ver análisis de IA.",
                            fontSize = 13.sp, color = GrisMedio)
                    }
                }
                return@Scaffold
            }

            // ── Resumen general ───────────────────────────────────────────────
            Text(
                "RESUMEN GENERAL",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = GrisMedio,
                modifier   = Modifier.padding(start = 24.dp, bottom = 6.dp)
            )
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ResumenChip(saludables.toString(), "Saludables", VerdeBosque)
                        ResumenChip(alertas.toString(),    "En alerta",  Color(0xFFF57C00))
                        ResumenChip(criticos.toString(),   "Críticos",   Color(0xFFD32F2F))
                        ResumenChip(sinEscaneo.toString(), "Sin escaneo", GrisMedio)
                    }
                    Spacer(Modifier.height(12.dp))
                    val promSalud = if (cultivos.isNotEmpty())
                        cultivos.map { it.indiceSalud }.average().toFloat() else 0f
                    Text(
                        "Índice de salud promedio: ${promSalud.toInt()}%",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF1A1A1A)
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress  = { (promSalud / 100f).coerceIn(0f, 1f) },
                        modifier  = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color     = when {
                            promSalud >= 70 -> VerdeBosque
                            promSalud >= 40 -> Color(0xFFF57C00)
                            else            -> Color(0xFFD32F2F)
                        },
                        trackColor = Color(0xFFEEEEEE)
                    )
                }
            }

            // ── Botón compartir reporte global ────────────────────────────────
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val texto = construirTextoReporte(cultivos)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, texto)
                        putExtra(Intent.EXTRA_SUBJECT, "Reporte AgroScan AI")
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdeBosque),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VerdeBosque)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartir reporte completo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))

            // ── Reportes por cultivo ──────────────────────────────────────────
            Text(
                "ANÁLISIS POR CULTIVO",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = GrisMedio,
                modifier   = Modifier.padding(start = 24.dp, bottom = 6.dp)
            )

            cultivos.forEach { cultivo ->
                ReporteCard(cultivo = cultivo, context = context)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Composables privados ──────────────────────────────────────────────────────

@Composable
private fun ResumenChip(valor: String, etiqueta: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor,    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(etiqueta, fontSize = 11.sp, color = GrisMedio)
    }
}

@Composable
private fun ReporteCard(cultivo: Cultivo, context: android.content.Context) {
    var expandido by remember { mutableStateOf(false) }

    val estadoColor = when (cultivo.estadoEnum()) {
        EstadoCultivo.SALUDABLE   -> VerdeBosque
        EstadoCultivo.ALERTA      -> Color(0xFFF57C00)
        EstadoCultivo.CRITICO     -> Color(0xFFD32F2F)
        EstadoCultivo.SIN_ESCANEO -> GrisMedio
    }
    val recomendaciones = remember(cultivo) { generarRecomendaciones(cultivo) }
    val analisisTexto   = remember(cultivo) { generarAnalisisIA(cultivo) }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Cabecera clickeable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandido = !expandido }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(estadoColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Grass, contentDescription = null,
                        tint = estadoColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cultivo.nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Text(cultivo.tipoCultivo, fontSize = 12.sp, color = GrisMedio)
                }
                Box(
                    modifier = Modifier
                        .background(estadoColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(cultivo.estadoTexto(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = estadoColor)
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint     = GrisMedio,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Contenido expandible
            AnimatedVisibility(
                visible = expandido,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

                    // Índice de salud
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Índice de salud:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                        Spacer(Modifier.width(8.dp))
                        Text("${cultivo.indiceSalud.toInt()}%", fontSize = 13.sp, color = estadoColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress  = { (cultivo.indiceSalud / 100f).coerceIn(0f, 1f) },
                        modifier  = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                        color     = estadoColor,
                        trackColor = Color(0xFFEEEEEE)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Datos de sensores en chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DatoChip("H: ${cultivo.humedadPromedio.toInt()}%", Color(0xFF1565C0))
                        DatoChip("N: ${cultivo.nitrogenio.toInt()}%",      VerdeBosque)
                        DatoChip("P: ${cultivo.fosforo.toInt()}%",         Color(0xFFE65100))
                        DatoChip("K: ${cultivo.potasio.toInt()}%",         Color(0xFF6A1B9A))
                    }

                    Spacer(Modifier.height(12.dp))

                    // Análisis de IA
                    Text("Análisis de IA", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = VerdeBosque)
                    Spacer(Modifier.height(4.dp))
                    Text(analisisTexto, fontSize = 12.sp, color = Color(0xFF424242), lineHeight = 18.sp)

                    Spacer(Modifier.height(12.dp))

                    // Recomendaciones
                    Text("Recomendaciones", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = VerdeBosque)
                    Spacer(Modifier.height(6.dp))
                    recomendaciones.forEach { rec ->
                        val (bgColor, iconRes) = when (rec.tipo) {
                            TipoRecom.RIEGO        -> Color(0xFFE3F2FD) to Icons.Filled.WaterDrop
                            TipoRecom.PLAGAS       -> Color(0xFFFFEBEE) to Icons.Filled.BugReport
                            TipoRecom.FERTILIZANTE -> Color(0xFFFFF3E0) to Icons.Filled.Science
                            TipoRecom.OPTIMO       -> Color(0xFFE8F5E9) to Icons.Filled.CheckCircle
                            TipoRecom.ATENCION     -> Color(0xFFFFF9C4) to Icons.Filled.Warning
                        }
                        val fgColor = when (rec.tipo) {
                            TipoRecom.RIEGO        -> Color(0xFF1565C0)
                            TipoRecom.PLAGAS       -> Color(0xFFD32F2F)
                            TipoRecom.FERTILIZANTE -> Color(0xFFE65100)
                            TipoRecom.OPTIMO       -> VerdeBosque
                            TipoRecom.ATENCION     -> Color(0xFFF9A825)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(bgColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(iconRes, contentDescription = null, tint = fgColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(rec.texto, fontSize = 12.sp, color = fgColor)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Botón compartir reporte individual
                    OutlinedButton(
                        onClick = {
                            val texto = buildString {
                                appendLine("Reporte IA — ${cultivo.nombre}")
                                appendLine("Estado: ${cultivo.estadoTexto()}")
                                appendLine("Índice de salud: ${cultivo.indiceSalud.toInt()}%")
                                appendLine()
                                appendLine("Análisis: $analisisTexto")
                                appendLine()
                                appendLine("Recomendaciones:")
                                recomendaciones.forEach { appendLine("• ${it.texto}") }
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, texto)
                                putExtra(Intent.EXTRA_SUBJECT, "Reporte IA — ${cultivo.nombre}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartir"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape  = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdeBosque),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VerdeBosque)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compartir este reporte", fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DatoChip(texto: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(texto, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
