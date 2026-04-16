package com.example.agroscanai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.R
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import com.example.agroscanai.ui.viewmodel.UltimoEscaneoData
import kotlin.math.roundToInt

// ──────────────────────────────────────────────────────────────────────────────
//  Helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun calcularIndiceSalud(cultivo: Cultivo, esc: UltimoEscaneoData?): Float {
    if (cultivo.estadoEnum() == EstadoCultivo.SIN_ESCANEO) return 0f
    // Use escaneo data when available, otherwise cultivo stored values
    val humedad = if (esc != null && esc.humedadSuelo > 0f) esc.humedadSuelo else cultivo.humedadPromedio
    val n       = if (esc != null && esc.nivelNitrogenio > 0f) esc.nivelNitrogenio else cultivo.nitrogenio
    val p       = if (esc != null && esc.nivelFosforo > 0f) esc.nivelFosforo else cultivo.fosforo
    val k       = if (esc != null && esc.nivelPotasio > 0f) esc.nivelPotasio else cultivo.potasio
    val plagas  = esc?.plagasDetectadas ?: cultivo.plagasDetectadas
    if (humedad == 0f && n == 0f && p == 0f) return cultivo.indiceSalud.takeIf { it > 0f } ?: 0f
    var score = 100f
    if (plagas) score -= 40f
    score -= when {
        humedad < 10f -> 35f; humedad < 15f -> 25f; humedad < 20f -> 15f
        humedad < 25f -> 5f;  humedad > 70f -> 10f; humedad > 60f -> 5f; else -> 0f
    }
    score -= when { n < 30f -> 20f; n < 45f -> 12f; n < 60f -> 5f; else -> 0f }
    score -= when { p < 40f -> 15f; p < 50f -> 8f;  p < 60f -> 3f; else -> 0f }
    score -= when { k < 50f -> 10f; k < 60f -> 5f;  else -> 0f }
    return score.coerceIn(5f, 100f)
}

private fun colorSalud(pct: Float) = when {
    pct >= 70f -> Color(0xFF2D5A27)
    pct >= 40f -> Color(0xFFFF8F00)
    else       -> Color(0xFFD32F2F)
}

private fun etiquetaSalud(pct: Float, plagas: Boolean) = when {
    pct >= 70f -> "Óptimo · Buen estado"
    pct >= 40f -> "Alerta · Requiere atención"
    plagas     -> "Crítico · Plaga y Estrés"
    else       -> "Crítico · Estado grave"
}

private fun estadoNutriente(ppm: Float, umbralOpt: Float = 60f, umbralAlerta: Float = 40f) = when {
    ppm >= umbralOpt   -> "Óptimo"
    ppm >= umbralAlerta -> "Alerta"
    else               -> "Deficiente"
}

private fun colorNutriente(ppm: Float, umbralOpt: Float = 60f, umbralAlerta: Float = 40f) = when {
    ppm >= umbralOpt    -> Color(0xFF2E7D32)
    ppm >= umbralAlerta -> Color(0xFFFF8F00)
    else                -> Color(0xFFD32F2F)
}

// ──────────────────────────────────────────────────────────────────────────────
//  Main Screen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun DashboardSaludScreen(
    cultivoId: String,
    onHomeClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos        by cultivosViewModel.cultivos.collectAsState()
    val ultimoEscaneo   by cultivosViewModel.ultimoEscaneo.collectAsState()
    val cargandoEscaneo by cultivosViewModel.cargandoEscaneo.collectAsState()

    val cultivo = cultivos.find { it.id == cultivoId }

    LaunchedEffect(cultivoId) {
        cultivosViewModel.cargarUltimoEscaneo(cultivoId)
    }

    var mostrarDetallado by remember { mutableStateOf(false) }

    if (cultivo == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VerdeBosque)
        }
        return
    }

    val indiceSalud   = calcularIndiceSalud(cultivo, ultimoEscaneo)
    val colorPrincipal = colorSalud(indiceSalud)
    val plagasBase    = ultimoEscaneo?.plagasDetectadas ?: cultivo.plagasDetectadas
    val etiqueta      = etiquetaSalud(indiceSalud, plagasBase)

    val n = ultimoEscaneo?.nivelNitrogenio?.takeIf { it > 0f } ?: cultivo.nitrogenio
    val p = ultimoEscaneo?.nivelFosforo?.takeIf    { it > 0f } ?: cultivo.fosforo
    val k = ultimoEscaneo?.nivelPotasio?.takeIf    { it > 0f } ?: cultivo.potasio
    val humedad = ultimoEscaneo?.humedadSuelo?.takeIf { it > 0f } ?: cultivo.humedadPromedio

    Scaffold(
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
            .background(Color(0xFFF5F7F5))
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .shadow(2.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = VerdeBosque)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Dashboard de Salud",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = VerdeBosque
            )
        }

        // ── Lote seleccionado ────────────────────────────────────────────────
        Text(
            text = "Lote seleccionado: ${cultivo.nombre} · ${cultivo.tipoCultivo}",
            fontSize = 13.sp,
            color = GrisHumo,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // ── Salud General ────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Salud General del Cultivo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerdeBosque
                )
                Spacer(Modifier.height(20.dp))

                if (cultivo.estadoEnum() == EstadoCultivo.SIN_ESCANEO) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color(0xFFF5F5F5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.SearchOff, contentDescription = null, tint = GrisMedio, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Sin escaneo", color = GrisMedio, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Esta parcela aún no ha sido escaneada.\nRealiza un escaneo para ver su estado.",
                        fontSize = 13.sp,
                        color = GrisMedio,
                        textAlign = TextAlign.Center
                    )
                } else {
                    DonutChart(
                        percentage = indiceSalud,
                        color = colorPrincipal,
                        modifier = Modifier.size(170.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = etiqueta,
                        fontSize = 13.sp,
                        color = colorPrincipal,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (cultivo.ultimoEscaneo.isNotBlank() && cultivo.ultimoEscaneo != "Sin escaneos aún") {
                        Text(
                            text = "Último escaneo: ${cultivo.ultimoEscaneo}",
                            fontSize = 11.sp,
                            color = GrisMedio,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        if (cultivo.estadoEnum() != EstadoCultivo.SIN_ESCANEO) {

            // ── Estado de nutrientes ─────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Estado de nutrientes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBosque
                    )
                    Spacer(Modifier.height(16.dp))
                    NutrientBarChart(
                        values = listOf(n, p),
                        labels = listOf("Nitrógeno", "Fósforo"),
                        maxValue = 80f,
                        chartHeight = 130.dp
                    )
                    Spacer(Modifier.height(10.dp))
                    LegendaNutrientes()
                }
            }

            // ── Nivel de plaga ───────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Nivel de plaga",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBosque
                    )
                    Spacer(Modifier.height(16.dp))
                    PlagaBarChart(plagasDetectadas = plagasBase)
                }
            }
        }

        // ── Botón Dashboard Detallado ────────────────────────────────────────
        Button(
            onClick = { mostrarDetallado = !mostrarDetallado },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeBosque),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (mostrarDetallado) "Ocultar Dashboard detallado" else "Ver Dashboard detallado",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (mostrarDetallado) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }

        // ── Sección Detallada (expandible) ───────────────────────────────────
        AnimatedVisibility(
            visible = mostrarDetallado,
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(tween(300)),
            exit  = shrinkVertically(animationSpec = tween(300)) + fadeOut(tween(200))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (cargandoEscaneo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = VerdeBosque)
                            Spacer(Modifier.height(10.dp))
                            Text("Cargando datos del escaneo…", fontSize = 13.sp, color = GrisMedio)
                        }
                    }
                } else {
                    DashboardDetallado(
                        cultivo = cultivo,
                        ultimoEscaneo = ultimoEscaneo,
                        n = n, p = p, k = k,
                        humedad = humedad,
                        indiceSalud = indiceSalud,
                        colorPrincipal = colorPrincipal
                    )
                }
            }
        }

        // ── Footer ───────────────────────────────────────────────────────────
        Text(
            text = "AgroScan AI es una marca registrada. Todos los derechos reservados",
            fontSize = 11.sp,
            color = GrisMedio,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        )
    } // end Column
    } // end Scaffold content
}

// ──────────────────────────────────────────────────────────────────────────────
//  Donut Chart
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DonutChart(
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "donut"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 30.dp.toPx()
            val inset       = strokeWidth / 2f
            val arcSize     = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft     = Offset(inset, inset)

            // Background ring
            drawArc(
                color      = Color(0xFFEEEEEE),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = strokeWidth)
            )
            // Colored arc
            val sweep = 360f * (animated / 100f)
            if (sweep > 0f) {
                drawArc(
                    color      = color,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animated.roundToInt()}%",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "Índice\nde Salud",
                fontSize = 11.sp,
                color = GrisMedio,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Nutrient Bar Chart
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NutrientBarChart(
    values: List<Float>,
    labels: List<String>,
    maxValue: Float,
    chartHeight: Dp = 120.dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        values.zip(labels).forEach { (value, label) ->
            val fraction   = (value / maxValue).coerceIn(0f, 1f)
            val barColor   = colorNutriente(value)
            val animated by animateFloatAsState(
                targetValue  = fraction,
                animationSpec = tween(1100, easing = FastOutSlowInEasing),
                label        = "nut_$label"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text  = "${value.roundToInt()} ppm",
                    fontSize = 11.sp,
                    color = barColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(chartHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxHeight(animated.coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(barColor)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = label,
                    fontSize = 12.sp,
                    color    = GrisHumo,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LegendaNutrientes() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(
            Pair(Color(0xFF2E7D32), "Óptimo >60 ppm"),
            Pair(Color(0xFFFF8F00), "Alerta 40–60 ppm"),
            Pair(RojoAlerta,        "Crítico <40 ppm")
        ).forEach { (c, t) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(c, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(t, fontSize = 9.sp, color = GrisMedio)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Plaga Bar Chart
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlagaBarChart(plagasDetectadas: Boolean) {
    // Two bars: Presencia and Riesgo
    val presenciaVal = if (plagasDetectadas) 4.2f else 0.4f
    val riesgoVal    = if (plagasDetectadas) 3.5f else 0.2f
    val maxVal       = 5f

    val presenciaColor = if (plagasDetectadas) RojoAlerta      else VerdeEsmeralda
    val riesgoColor    = if (plagasDetectadas) Color(0xFFFF8F00) else VerdeEsmeralda

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        listOf<Triple<Float, String, Color>>(
            Triple(presenciaVal, "Presencia",    presenciaColor),
            Triple(riesgoVal,    "Nivel Riesgo", riesgoColor)
        ).forEach { (value, label, barColor) ->
            val fraction = value / maxVal
            val animated by animateFloatAsState(
                targetValue   = fraction,
                animationSpec = tween(1100, easing = FastOutSlowInEasing),
                label         = "plaga_$label"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text  = "${value.roundToInt()}/5",
                    fontSize = 11.sp,
                    color = barColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(130.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxHeight(animated.coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(barColor)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(text = label, fontSize = 12.sp, color = GrisHumo, textAlign = TextAlign.Center)
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (plagasDetectadas) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (plagasDetectadas) Icons.Filled.Warning else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (plagasDetectadas) RojoAlerta else VerdeEsmeralda,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (plagasDetectadas)
                "Se detectaron indicios de plaga. Revisa la sección detallada."
            else
                "No se detectaron plagas en este escaneo.",
            fontSize = 12.sp,
            color = if (plagasDetectadas) Color(0xFFB71C1C) else Color(0xFF1B5E20)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Dashboard Detallado
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardDetallado(
    cultivo: Cultivo,
    ultimoEscaneo: UltimoEscaneoData?,
    n: Float, p: Float, k: Float,
    humedad: Float,
    indiceSalud: Float,
    colorPrincipal: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // ── Separador visual ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f), color = Color(0xFFDDDDDD))
            Text(
                "  Análisis Detallado  ",
                fontSize = 11.sp,
                color = GrisMedio,
                fontWeight = FontWeight.Medium
            )
            Divider(modifier = Modifier.weight(1f), color = Color(0xFFDDDDDD))
        }

        Spacer(Modifier.height(10.dp))

        if (ultimoEscaneo == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFFF8F00))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Los datos detallados del último escaneo no están disponibles en este momento.",
                        fontSize = 13.sp,
                        color = Color(0xFF5D4037)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            return
        }

        // ── 1. Análisis Completo de Nutrientes ──────────────────────────────
        DetalleCard(
            titulo = "Análisis Completo de Nutrientes",
            icono  = Icons.Filled.Science
        ) {
            NutrienteDetalleRow("Nitrógeno (N)", n, 80f, "ppm", 60f, 40f)
            Spacer(Modifier.height(10.dp))
            NutrienteDetalleRow("Fósforo (P)", p, 80f, "ppm", 60f, 40f)
            Spacer(Modifier.height(10.dp))
            NutrienteDetalleRow("Potasio (K)", k, 80f, "ppm", 60f, 50f)
            if (ultimoEscaneo.descripcionNutrientes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    ultimoEscaneo.descripcionNutrientes,
                    fontSize = 12.sp,
                    color = GrisHumo,
                    lineHeight = 17.sp
                )
            }
            if (ultimoEscaneo.recomendacionNutrientes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = VerdeEsmeralda, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        ultimoEscaneo.recomendacionNutrientes,
                        fontSize = 12.sp,
                        color = VerdeBosque,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 2. Condiciones del Suelo ─────────────────────────────────────────
        DetalleCard(
            titulo = "Condiciones del Suelo",
            icono  = Icons.Filled.Terrain
        ) {
            // Humedad gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Humedad del Suelo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GrisHumo)
                    Spacer(Modifier.height(4.dp))
                    val humedadFrac = (humedad / 100f).coerceIn(0f, 1f)
                    val humedadColor = when {
                        humedad in 25f..55f -> VerdeEsmeralda
                        humedad >= 20f      -> Color(0xFFFF8F00)
                        else                -> RojoAlerta
                    }
                    val animHum by animateFloatAsState(humedadFrac, tween(1000, easing = FastOutSlowInEasing), label = "hum")
                    LinearProgressIndicator(
                        progress = { animHum },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = humedadColor,
                        trackColor = Color(0xFFEEEEEE)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${humedad.roundToInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = humedadColor)
                        Text(
                            when {
                                humedad in 25f..55f -> "Óptima"
                                humedad >= 20f      -> "Baja"
                                else                -> "Crítica"
                            },
                            fontSize = 12.sp, color = humedadColor
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFE3F2FD), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${humedad.roundToInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                    }
                    Text("Humedad", fontSize = 9.sp, color = GrisMedio)
                }
            }

            if (ultimoEscaneo.estresSuelo.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Estrés Hídrico", fontSize = 11.sp, color = GrisMedio)
                        Text(ultimoEscaneo.estresSuelo, fontSize = 13.sp, color = GrisHumo, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (ultimoEscaneo.descripcionSuelo.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    ultimoEscaneo.descripcionSuelo,
                    fontSize = 12.sp,
                    color = GrisHumo,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 3. Detección de Plagas ───────────────────────────────────────────
        DetalleCard(
            titulo = "Detección de Plagas",
            icono  = Icons.Filled.BugReport
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (ultimoEscaneo.plagasDetectadas) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (ultimoEscaneo.plagasDetectadas) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (ultimoEscaneo.plagasDetectadas) RojoAlerta else VerdeEsmeralda,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (ultimoEscaneo.plagasDetectadas) "Plagas detectadas" else "Sin plagas detectadas",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ultimoEscaneo.plagasDetectadas) RojoAlerta else VerdeEsmeralda
                    )
                    Text(
                        if (ultimoEscaneo.plagasDetectadas)
                            "Se encontraron indicios durante el escaneo"
                        else
                            "El escaneo no detectó actividad de plagas",
                        fontSize = 12.sp,
                        color = GrisMedio
                    )
                }
            }

            if (ultimoEscaneo.descripcionPlagas.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    ultimoEscaneo.descripcionPlagas,
                    fontSize = 12.sp,
                    color = GrisHumo,
                    lineHeight = 17.sp
                )
            }

            if (ultimoEscaneo.plagasDetectadas) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Icon(Icons.Filled.Healing, contentDescription = null, tint = Color(0xFFFF6F00), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Se recomienda consultar con un agrónomo y aplicar tratamiento preventivo a la brevedad posible.",
                        fontSize = 12.sp,
                        color = Color(0xFF5D4037),
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 4. Resumen de Índice de Salud por Categoría ─────────────────────
        DetalleCard(
            titulo = "Resumen del Índice de Salud",
            icono  = Icons.Filled.BarChart
        ) {
            val categorias = listOf<Triple<String, Float, String>>(
                Triple("Nutrientes",  calcularScoreNutrientes(n, p, k), "Nivel nutricional del suelo"),
                Triple("Hidratación", calcularScoreHumedad(humedad),    "Disponibilidad de agua"),
                Triple("Fitosanidad", if (ultimoEscaneo.plagasDetectadas) 20f else 95f, "Ausencia de plagas"),
                Triple("General",     indiceSalud, "Índice global de salud")
            )
            categorias.forEach { (cat, score, desc) ->
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GrisHumo)
                            Text("${score.roundToInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorSalud(score))
                        }
                        Spacer(Modifier.height(3.dp))
                        val animScore by animateFloatAsState((score / 100f).coerceIn(0f, 1f), tween(1000, easing = FastOutSlowInEasing), label = "cat_$cat")
                        LinearProgressIndicator(
                            progress    = { animScore },
                            modifier    = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color       = colorSalud(score),
                            trackColor  = Color(0xFFEEEEEE)
                        )
                        Text(desc, fontSize = 10.sp, color = GrisMedio, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 5. Datos Técnicos del Escaneo ────────────────────────────────────
        DetalleCard(
            titulo = "Datos Técnicos del Escaneo",
            icono  = Icons.Filled.Sensors
        ) {
            val gpsText = if (ultimoEscaneo.gpsLat != 0.0 && ultimoEscaneo.gpsLon != 0.0)
                "%.6f, %.6f".format(ultimoEscaneo.gpsLat, ultimoEscaneo.gpsLon)
            else "No disponible"

            DatoTecnicoItem(Icons.Filled.SmartToy,       "Confianza IA",      "${ultimoEscaneo.confianzaIA}%",    Color(0xFF1565C0))
            DatoTecnicoItem(Icons.Filled.CalendarMonth,  "Fecha de escaneo",  ultimoEscaneo.fecha.ifBlank { "—" }, GrisHumo)
            DatoTecnicoItem(Icons.Filled.LocationOn,     "Coordenadas GPS",   gpsText,                             Color(0xFF6A1B9A))
            DatoTecnicoItem(Icons.Filled.Landscape,      "Superficie",        "${cultivo.hectareas} hectáreas",    VerdeBosque)
            DatoTecnicoItem(Icons.Filled.Agriculture,    "Variedad",          cultivo.variedadSemilla.ifBlank { cultivo.tipoCultivo }, VerdeEsmeraldaOscuro)
            if (cultivo.fechaSiembra.isNotBlank()) {
                DatoTecnicoItem(Icons.Filled.Event, "Fecha de siembra", cultivo.fechaSiembra, GrisHumo)
            }
            if (cultivo.ubicacion.isNotBlank()) {
                DatoTecnicoItem(Icons.Filled.Place, "Ubicación", cultivo.ubicacion, GrisHumo)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Sub-composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetalleCard(
    titulo: String,
    icono: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(VerdeLima, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, contentDescription = null, tint = VerdeBosque, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun NutrienteDetalleRow(
    nombre: String,
    valor: Float,
    maxVal: Float,
    unidad: String,
    umbralOpt: Float = 60f,
    umbralAlerta: Float = 40f
) {
    val fraction  = (valor / maxVal).coerceIn(0f, 1f)
    val barColor  = colorNutriente(valor, umbralOpt, umbralAlerta)
    val estado    = estadoNutriente(valor, umbralOpt, umbralAlerta)
    val animFrac by animateFloatAsState(fraction, tween(1000, easing = FastOutSlowInEasing), label = "nr_$nombre")

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GrisHumo)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${valor.roundToInt()} $unidad",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(barColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(estado, fontSize = 10.sp, color = barColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            progress    = { animFrac },
            modifier    = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color       = barColor,
            trackColor  = Color(0xFFEEEEEE)
        )
    }
}

@Composable
private fun DatoTecnicoItem(
    icono: ImageVector,
    etiqueta: String,
    valor: String,
    iconColor: Color = GrisHumo
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(etiqueta, fontSize = 11.sp, color = GrisMedio)
            Text(valor, fontSize = 13.sp, color = GrisHumo, fontWeight = FontWeight.Medium)
        }
    }
    Divider(color = Color(0xFFF0F0F0))
}

// ──────────────────────────────────────────────────────────────────────────────
//  Score helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun calcularScoreNutrientes(n: Float, p: Float, k: Float): Float {
    var score = 100f
    score -= when { n < 30f -> 30f; n < 45f -> 20f; n < 60f -> 8f; else -> 0f }
    score -= when { p < 40f -> 25f; p < 50f -> 15f; p < 60f -> 5f; else -> 0f }
    score -= when { k < 50f -> 20f; k < 60f -> 10f; else -> 0f }
    return score.coerceIn(5f, 100f)
}

private fun calcularScoreHumedad(humedad: Float): Float {
    return when {
        humedad in 25f..55f -> 92f
        humedad in 20f..25f -> 70f
        humedad in 15f..20f -> 45f
        humedad in 55f..70f -> 75f
        humedad < 15f       -> 20f
        else                -> 55f
    }
}
