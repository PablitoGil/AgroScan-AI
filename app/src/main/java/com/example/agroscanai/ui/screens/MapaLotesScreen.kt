package com.example.agroscanai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.R
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel

private enum class PasoMapa { SELECCIONAR, VER_MAPA }

@Composable
fun MapaLotesScreen(
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos   by cultivosViewModel.cultivos.collectAsState()
    val isLoading  by cultivosViewModel.isLoading.collectAsState()

    var paso                by remember { mutableStateOf(PasoMapa.SELECCIONAR) }
    var cultivoSeleccionado by remember { mutableStateOf<Cultivo?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7F5))) {

        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .shadow(2.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Regresar",
                tint = VerdeBosque,
                modifier = Modifier
                    .size(26.dp)
                    .clickable {
                        if (paso == PasoMapa.VER_MAPA) {
                            paso = PasoMapa.SELECCIONAR
                            cultivoSeleccionado = null
                        } else {
                            onHomeClick()
                        }
                    }
            )
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painterResource(R.drawable.logo_agroscan), "Logo",
                    modifier = Modifier.size(32.dp)
                )
                Text("AgroScan AI", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEEF2EE))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = GrisMedio, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("¿Te apetece regar hoy?", fontSize = 13.sp, color = GrisMedio)
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(VerdeBosque)
                    .clickable { onPerfilClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // ── Nav bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Home, "Inicio", tint = GrisMedio,
                modifier = Modifier.size(28.dp).clickable { onHomeClick() }
            )
            Icon(
                Icons.Filled.Notifications, "Notificaciones", tint = GrisMedio,
                modifier = Modifier.size(28.dp).clickable { onNotificacionesClick() }
            )
        }

        HorizontalDivider(color = Color(0xFFE8ECE8))

        // ── Contenido según paso ─────────────────────────────────────────────
        when (paso) {
            PasoMapa.SELECCIONAR -> SeleccionarLoteStep(
                cultivos  = cultivos,
                isLoading = isLoading,
                onSeleccionar = {
                    cultivoSeleccionado = it
                    paso = PasoMapa.VER_MAPA
                }
            )
            PasoMapa.VER_MAPA -> MapaCalorStep(
                cultivo = cultivoSeleccionado!!
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// PASO 1 – Seleccionar lote
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SeleccionarLoteStep(
    cultivos: List<Cultivo>,
    isLoading: Boolean,
    onSeleccionar: (Cultivo) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdeEsmeralda)
                }
            }
            cultivos.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Map, null,
                        tint = VerdeEsmeralda.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Sin parcelas registradas",
                        fontSize = 16.sp, color = GrisMedio,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Agrega un cultivo en \"Mis Cultivos\" para ver su mapa de calor.",
                        fontSize = 13.sp, color = GrisMedio.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cultivos, key = { it.id }) { cultivo ->
                        LoteCard(cultivo = cultivo, onClick = { onSeleccionar(cultivo) })
                    }
                    item {
                        Text(
                            "AgroScan AI es una marca registrada. Todos los derechos reservados.",
                            fontSize = 10.sp, color = GrisMedio.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoteCard(cultivo: Cultivo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono circular verde
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(VerdeEsmeralda),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.ic_cultivos), null,
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(cultivo.nombre)
                        if (cultivo.tipoCultivo.isNotBlank()) append(" - ${cultivo.tipoCultivo}")
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerdeBosque,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Ver Mapa de Calor",
                    fontSize = 12.sp,
                    color = GrisMedio
                )
            }

            Spacer(Modifier.width(10.dp))

            // Thumbnail del cultivo
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VerdeEsmeralda.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.ic_cultivos), null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                    alpha = 0.85f
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// PASO 2 – Mapa de calor del lote
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapaCalorStep(cultivo: Cultivo) {
    var panelActivo by remember { mutableStateOf<String?>(null) }

    val tienePlagas   = cultivo.estadoEnum() == EstadoCultivo.CRITICO
    val tieneEstres   = cultivo.humedadPromedio in 0.01f..19.99f
    val confianzaIA   = if (cultivo.ultimoEscaneo.isNotBlank() && cultivo.ultimoEscaneo != "Sin escaneos aún") 86 else 0

    // Coordenadas GPS: usa ubicacion si tiene formato lat,lon, si no usa default México
    val (gpsLon, gpsLat) = remember(cultivo.ubicacion) {
        val parts = cultivo.ubicacion.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        if (parts.size == 2) Pair(parts[0], parts[1]) else Pair(-99.16, 19.43)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        // Título con icono dron
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.FlightTakeoff, null,
                tint = VerdeBosque,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "AgroScan AI",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Lote seleccionado: ${cultivo.nombre}" +
                        if (cultivo.tipoCultivo.isNotBlank()) " - ${cultivo.tipoCultivo}" else "",
                fontSize = 13.sp,
                color = GrisHumo
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Mapa de calor ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            HeatMapCanvas(
                tienePlagas = tienePlagas,
                tieneEstres = tieneEstres,
                estadoEnum  = cultivo.estadoEnum(),
                modifier    = Modifier.fillMaxSize()
            )

            // Overlay de panel de info
            if (panelActivo != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { panelActivo = null },
                    contentAlignment = Alignment.Center
                ) {
                    MapaInfoPanel(
                        panel   = panelActivo!!,
                        cultivo = cultivo,
                        onClose = { panelActivo = null }
                    )
                }
            }

            // Marcadores sobre el mapa
            if (panelActivo == null) {
                if (tienePlagas) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = (-18).dp, y = 14.dp)
                    ) {
                        MarcadorChip("Plaga Detectada", Color(0xFFD32F2F))
                    }
                }
                if (tieneEstres) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = 22.dp, y = (-10).dp)
                    ) {
                        MarcadorChip("Estrés Hídrico", Color(0xFFFFA000))
                    }
                }
            }

            // GPS y confianza
            if (panelActivo == null) {
                Text(
                    "GPS: ${"%.2f".format(gpsLon)}, ${"%.2f".format(gpsLat)}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 10.dp)
                )
                Text(
                    "Confianza IA: ${confianzaIA}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Botones inferiores ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MapaBotonCircular(
                label     = "Plagas",
                icon      = Icons.Filled.BugReport,
                isActive  = panelActivo == "plagas",
                hasAlert  = tienePlagas,
                onClick   = { panelActivo = if (panelActivo == "plagas") null else "plagas" }
            )
            MapaBotonCircular(
                label     = "Nutrientes",
                icon      = Icons.Filled.Science,
                isActive  = panelActivo == "nutrientes",
                hasAlert  = cultivo.nitrogenio in 0.01f..39.99f,
                onClick   = { panelActivo = if (panelActivo == "nutrientes") null else "nutrientes" }
            )
            MapaBotonCircular(
                label     = "Satélite",
                icon      = Icons.Filled.Satellite,
                isActive  = panelActivo == "satelite",
                hasAlert  = false,
                onClick   = { panelActivo = if (panelActivo == "satelite") null else "satelite" }
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "AgroScan AI es una marca registrada. Todos los derechos reservados.",
            fontSize = 10.sp,
            color = GrisMedio.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 16.dp)
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Canvas: mapa de calor NDVI simulado
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeatMapCanvas(
    tienePlagas: Boolean,
    tieneEstres: Boolean,
    estadoEnum: EstadoCultivo,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heatmap")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Fondo: campo verde oscuro
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C))
            )
        )

        // Zonas saludables (manchas verdes brillantes)
        listOf(
            Offset(w * 0.15f, h * 0.2f)  to w * 0.28f,
            Offset(w * 0.8f,  h * 0.25f) to w * 0.22f,
            Offset(w * 0.1f,  h * 0.75f) to w * 0.20f,
            Offset(w * 0.75f, h * 0.78f) to w * 0.25f
        ).forEach { (center, radius) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF66BB6A).copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )
        }

        // Zona problemática central
        val problemCenter = Offset(w * 0.5f, h * 0.48f)
        val problemRadius = w * 0.32f * pulse

        when (estadoEnum) {
            EstadoCultivo.CRITICO -> {
                // Rojo intenso: plagas
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB71C1C).copy(alpha = 0.95f),
                            Color(0xFFE53935).copy(alpha = 0.80f),
                            Color(0xFFFFA000).copy(alpha = 0.55f),
                            Color(0xFF66BB6A).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = problemCenter,
                        radius = problemRadius
                    ),
                    center = problemCenter,
                    radius = problemRadius
                )
            }
            EstadoCultivo.ALERTA -> {
                // Naranja/amarillo: estrés
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF6F00).copy(alpha = 0.85f),
                            Color(0xFFFFA000).copy(alpha = 0.65f),
                            Color(0xFFFFD54F).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = problemCenter,
                        radius = problemRadius
                    ),
                    center = problemCenter,
                    radius = problemRadius
                )
            }
            else -> {
                // Verde: saludable, mancha suave
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00C853).copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = problemCenter,
                        radius = w * 0.25f
                    ),
                    center = problemCenter,
                    radius = w * 0.25f
                )
            }
        }

        // Líneas de cuadrícula
        val gridColor = Color.White.copy(alpha = 0.18f)
        val stroke = Stroke(width = 1.dp.toPx())
        for (i in 1..3) {
            val x = w * i / 4f
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
        }
        for (i in 1..3) {
            val y = h * i / 4f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
        }

        // Cruz central
        val cx = w * 0.5f
        val cy = h * 0.48f
        val crossColor = Color.White.copy(alpha = 0.7f)
        val crossLen = 14.dp.toPx()
        drawLine(crossColor, Offset(cx - crossLen, cy), Offset(cx + crossLen, cy),
            strokeWidth = 1.5f.dp.toPx(), cap = StrokeCap.Round)
        drawLine(crossColor, Offset(cx, cy - crossLen), Offset(cx, cy + crossLen),
            strokeWidth = 1.5f.dp.toPx(), cap = StrokeCap.Round)

        // Borde del mapa
        drawRect(
            color = Color.White.copy(alpha = 0.1f),
            style = stroke
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Chip de marcador sobre el mapa
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun MarcadorChip(texto: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.9f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        Spacer(Modifier.width(4.dp))
        Text(texto, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Botones circulares inferiores
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapaBotonCircular(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    hasAlert: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isActive) VerdeBosque else VerdeEsmeralda)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            if (hasAlert) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .align(Alignment.TopEnd)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = VerdeBosque, fontWeight = FontWeight.Medium)
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Panel de información al tocar los botones
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapaInfoPanel(
    panel: String,
    cultivo: Cultivo,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .clickable(enabled = false) {},
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when (panel) {
                        "plagas"     -> "Detección de Plagas"
                        "nutrientes" -> "Niveles de Nutrientes"
                        else         -> "Vista Satélite"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerdeBosque
                )
                Icon(
                    Icons.Filled.Close, "Cerrar",
                    tint = GrisMedio,
                    modifier = Modifier.size(20.dp).clickable { onClose() }
                )
            }

            Spacer(Modifier.height(14.dp))

            when (panel) {
                "plagas" -> PlagasInfo(cultivo)
                "nutrientes" -> NutrientesInfo(cultivo)
                "satelite" -> SateliteInfo(cultivo)
            }
        }
    }
}

@Composable
private fun PlagasInfo(cultivo: Cultivo) {
    val tienePlagas = cultivo.estadoEnum() == EstadoCultivo.CRITICO
    val colorEstado = if (tienePlagas) Color(0xFFD32F2F) else Color(0xFF4CAF50)
    val iconEstado  = if (tienePlagas) Icons.Filled.BugReport else Icons.Filled.CheckCircle

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(iconEstado, null, tint = colorEstado, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            if (tienePlagas) "Plaga detectada en la parcela"
            else "Sin plagas detectadas",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorEstado
        )
    }
    Spacer(Modifier.height(10.dp))
    Text(
        if (tienePlagas)
            "Se detectaron signos de plaga en el área escaneada. Se recomienda aplicar tratamiento preventivo en las zonas afectadas marcadas en rojo."
        else
            "No se detectó ninguna plaga durante el último escaneo. El cultivo se encuentra libre de infestaciones.",
        fontSize = 13.sp,
        color = GrisHumo,
        lineHeight = 20.sp
    )
}

@Composable
private fun NutrientesInfo(cultivo: Cultivo) {
    val nitrogenio = cultivo.nitrogenio
    val fosforo    = cultivo.fosforo
    val potasio    = cultivo.potasio

    if (nitrogenio <= 0f && fosforo <= 0f && potasio <= 0f) {
        Text(
            "Realiza un escaneo para obtener los niveles de nutrientes de esta parcela.",
            fontSize = 13.sp, color = GrisMedio, lineHeight = 20.sp
        )
        return
    }

    listOf(
        Triple("Nitrógeno (N)", nitrogenio, 80f),
        Triple("Fósforo (P)",   fosforo,    80f),
        Triple("Potasio (K)",   potasio,    80f)
    ).forEach { (nombre, valor, maximo) ->
        val pct    = (valor / maximo).coerceIn(0f, 1f)
        val color  = when {
            pct < 0.4f -> Color(0xFFD32F2F)
            pct < 0.65f -> Color(0xFFFFA000)
            else        -> Color(0xFF4CAF50)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(nombre, fontSize = 12.sp, color = GrisHumo, modifier = Modifier.width(100.dp))
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color    = color,
                trackColor = GrisClaro
            )
            Spacer(Modifier.width(8.dp))
            Text("${valor.toInt()}", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp))
        }
    }
}

@Composable
private fun SateliteInfo(cultivo: Cultivo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Satellite, null, tint = VerdeBosque, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text("Datos del último escaneo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = VerdeBosque)
    }
    Spacer(Modifier.height(10.dp))
    InfoRow("Parcela",       cultivo.nombre)
    InfoRow("Tipo",          cultivo.tipoCultivo.ifBlank { "—" })
    InfoRow("Área",          "${cultivo.hectareas} hectáreas")
    InfoRow("Último escaneo", cultivo.ultimoEscaneo.ifBlank { "Sin escaneos aún" })
    InfoRow("Estado",        cultivo.estadoTexto())
    if (cultivo.humedadPromedio > 0f) {
        InfoRow("Humedad suelo", "${"%.1f".format(cultivo.humedadPromedio)}%")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = GrisMedio)
        Text(value, fontSize = 12.sp, color = GrisHumo, fontWeight = FontWeight.Medium)
    }
}
