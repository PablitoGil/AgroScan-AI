package com.example.agroscanai.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import com.example.agroscanai.ui.viewmodel.UltimoEscaneoData

@Composable
fun DetalleCultivoScreen(
    cultivoId: String,
    onBackClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onCultivoEliminado: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos        by cultivosViewModel.cultivos.collectAsState()
    val ultimoEscaneo   by cultivosViewModel.ultimoEscaneo.collectAsState()
    val cargandoEscaneo by cultivosViewModel.cargandoEscaneo.collectAsState()

    val cultivo = cultivos.find { it.id == cultivoId }

    var showDeleteDialog  by remember { mutableStateOf(false) }
    var expandedEscaneo   by remember { mutableStateOf(false) }

    LaunchedEffect(cultivoId) {
        cultivosViewModel.cargarUltimoEscaneo(cultivoId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon  = { Icon(Icons.Filled.Delete, null, tint = RojoAlerta, modifier = Modifier.size(30.dp)) },
            title = { Text("Eliminar cultivo", fontWeight = FontWeight.Bold) },
            text  = { Text("¿Estás seguro de que deseas eliminar '${cultivo?.nombre}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        cultivosViewModel.eliminarCultivo(cultivoId)
                        onCultivoEliminado()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RojoAlerta),
                    shape  = RoundedCornerShape(10.dp)
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = GrisHumo)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (cultivo == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VerdeBosque)
        }
        return
    }

    val estadoColor = when (cultivo.estadoEnum()) {
        EstadoCultivo.SALUDABLE   -> VerdeEsmeralda
        EstadoCultivo.ALERTA      -> Color(0xFFFFA000)
        EstadoCultivo.CRITICO     -> RojoAlerta
        EstadoCultivo.SIN_ESCANEO -> GrisMedio
    }
    val headerTop = when (cultivo.estadoEnum()) {
        EstadoCultivo.CRITICO     -> Color(0xFF8B0000)
        EstadoCultivo.ALERTA      -> Color(0xFF6D4C41)
        EstadoCultivo.SALUDABLE   -> VerdeBosqueOscuro
        EstadoCultivo.SIN_ESCANEO -> Color(0xFF455A64)
    }
    val headerBot = when (cultivo.estadoEnum()) {
        EstadoCultivo.CRITICO     -> Color(0xFFD32F2F)
        EstadoCultivo.ALERTA      -> Color(0xFFFFA000)
        EstadoCultivo.SALUDABLE   -> VerdeBosque
        EstadoCultivo.SIN_ESCANEO -> Color(0xFF607D8B)
    }

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
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(headerTop, headerBot)))
                .padding(bottom = 28.dp)
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
                Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White)
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val estadoLabel = when (cultivo.estadoEnum()) {
                    EstadoCultivo.SALUDABLE   -> "Saludable"
                    EstadoCultivo.ALERTA      -> "En alerta"
                    EstadoCultivo.CRITICO     -> "Crítico"
                    EstadoCultivo.SIN_ESCANEO -> "Sin escaneo"
                }
                val badgeBg = when (cultivo.estadoEnum()) {
                    EstadoCultivo.SALUDABLE   -> Color(0xFF1B5E20)
                    EstadoCultivo.ALERTA      -> Color(0xFFE65100)
                    EstadoCultivo.CRITICO     -> Color(0xFFB71C1C)
                    EstadoCultivo.SIN_ESCANEO -> Color(0xFF37474F)
                }
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(estadoLabel, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(cultivo.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (cultivo.tipoCultivo.isNotBlank()) {
                    Text(
                        "${cultivo.tipoCultivo}${if (cultivo.variedadSemilla.isNotBlank()) " · ${cultivo.variedadSemilla}" else ""}",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height((-16).dp))

        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape     = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut índice de salud
                val saludColor = when {
                    cultivo.indiceSalud >= 70f -> VerdeEsmeralda
                    cultivo.indiceSalud >= 40f -> Color(0xFFFFA000)
                    cultivo.indiceSalud > 0f   -> RojoAlerta
                    else                       -> GrisMedio
                }
                val displaySalud = cultivo.indiceSalud.coerceAtLeast(0f)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
                    Canvas(modifier = Modifier.size(92.dp)) {
                        drawArc(
                            color = saludColor.copy(alpha = 0.15f),
                            startAngle = -210f, sweepAngle = 240f,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = saludColor,
                            startAngle = -210f,
                            sweepAngle = 240f * (displaySalud / 100f).coerceIn(0f, 1f),
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${displaySalud.toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = saludColor)
                        Text("Salud", fontSize = 10.sp, color = GrisMedio)
                    }
                }

                // Columna 1
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat("Humedad",    "${cultivo.humedadPromedio.toInt()}%",   Color(0xFF1565C0))
                    MiniStat("Nitrógeno",  "${cultivo.nitrogenio.toInt()} ppm",     VerdeBosque)
                    MiniStat("Área",       "${cultivo.hectareas} ha",               Color(0xFF6A1B9A))
                }

                // Columna 2
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat("Fósforo",    "${cultivo.fosforo.toInt()} ppm",        Color(0xFFE65100))
                    MiniStat("Potasio",    "${cultivo.potasio.toInt()} ppm",        Color(0xFF00695C))
                    MiniStat(
                        "Plagas",
                        if (cultivo.plagasDetectadas) "Detectadas" else "No",
                        if (cultivo.plagasDetectadas) RojoAlerta else VerdeEsmeralda
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (cultivo.nitrogenio > 0f || cultivo.fosforo > 0f || cultivo.potasio > 0f) {
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Niveles de nutrientes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
                    Spacer(Modifier.height(14.dp))
                    NutrienteBar("Nitrógeno (N)", cultivo.nitrogenio,      150f, VerdeBosque)
                    Spacer(Modifier.height(10.dp))
                    NutrienteBar("Fósforo (P)",   cultivo.fosforo,         150f, Color(0xFFE65100))
                    Spacer(Modifier.height(10.dp))
                    NutrienteBar("Potasio (K)",   cultivo.potasio,         200f, Color(0xFF00695C))
                    Spacer(Modifier.height(10.dp))
                    NutrienteBar("Humedad suelo", cultivo.humedadPromedio, 100f, Color(0xFF1565C0))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

         Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Información del cultivo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
                Spacer(Modifier.height(12.dp))
                InfoFilaCultivo(Icons.Filled.Grass,         "Tipo de cultivo", cultivo.tipoCultivo.ifBlank { "—" },             VerdeBosque)
                if (cultivo.variedadSemilla.isNotBlank())
                    InfoFilaCultivo(Icons.Filled.LocalFlorist,  "Variedad",        cultivo.variedadSemilla,                         VerdeEsmeralda)
                InfoFilaCultivo(Icons.Filled.Straighten,    "Extensión",       "${cultivo.hectareas} hectáreas",                Color(0xFF6A1B9A))
                if (cultivo.fechaSiembra.isNotBlank())
                    InfoFilaCultivo(Icons.Filled.CalendarToday, "Fecha de siembra", cultivo.fechaSiembra,                         Color(0xFF1565C0))
                if (cultivo.ubicacion.isNotBlank())
                    InfoFilaCultivo(Icons.Filled.LocationOn,    "Ubicación",       cultivo.ubicacion,                             Color(0xFFE65100))
                InfoFilaCultivo(Icons.Filled.Schedule,      "Último escaneo",  cultivo.ultimoEscaneo.ifBlank { "Sin escaneos aún" }, GrisMedio)
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { expandedEscaneo = !expandedEscaneo },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Analytics, null, tint = VerdeBosque, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Detalle del último escaneo",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (expandedEscaneo) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null, tint = GrisMedio
                    )
                }

                if (expandedEscaneo) {
                    Spacer(Modifier.height(14.dp))
                    when {
                        cargandoEscaneo -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = VerdeBosque, modifier = Modifier.size(26.dp))
                        }
                        ultimoEscaneo != null -> EscaneoDetalleContent(ultimoEscaneo!!)
                        else -> Text(
                            "Esta parcela aún no tiene escaneos registrados.",
                            fontSize = 13.sp, color = GrisMedio,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

         OutlinedButton(
            onClick  = { showDeleteDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RojoAlerta),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RojoAlerta)
        ) {
            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Eliminar cultivo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(32.dp))
    }
    }
}


@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = GrisMedio)
    }
}

@Composable
private fun NutrienteBar(nombre: String, valor: Float, max: Float, color: Color) {
    val progreso = (valor / max).coerceIn(0f, 1f)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(nombre, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text("${valor.toInt()}", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress    = progreso,
            modifier    = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color       = color,
            trackColor  = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun InfoFilaCultivo(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = GrisMedio)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun EscaneoDetalleContent(esc: UltimoEscaneoData) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (esc.fecha.isNotBlank())
            EscaneoFila("Fecha del escaneo", esc.fecha, Icons.Filled.Schedule, GrisMedio)
        EscaneoFila(
            "Confianza IA", "${esc.confianzaIA}%",
            Icons.Filled.Analytics, Color(0xFF6A1B9A)
        )
        EscaneoFila(
            "Plagas detectadas",
            if (esc.plagasDetectadas) "Sí${if (esc.descripcionPlagas.isNotBlank()) " — ${esc.descripcionPlagas}" else ""}"
            else "No se detectaron plagas",
            Icons.Filled.BugReport,
            if (esc.plagasDetectadas) RojoAlerta else VerdeEsmeralda
        )
        if (esc.estresSuelo.isNotBlank())
            EscaneoFila("Estrés del suelo", esc.estresSuelo, Icons.Filled.Terrain, Color(0xFFE65100))
        if (esc.descripcionSuelo.isNotBlank())
            EscaneoFila("Estado del suelo", esc.descripcionSuelo, Icons.Filled.Landscape, GrisMedio)
        if (esc.descripcionNutrientes.isNotBlank())
            EscaneoFila("Análisis nutricional", esc.descripcionNutrientes, Icons.Filled.Science, VerdeBosque)
        if (esc.recomendacionNutrientes.isNotBlank())
            EscaneoFila("Recomendación", esc.recomendacionNutrientes, Icons.Filled.Lightbulb, Color(0xFFF57F17))
        if (esc.gpsLat != 0.0 || esc.gpsLon != 0.0)
            EscaneoFila("Coordenadas GPS", "Lat: %.5f  Lon: %.5f".format(esc.gpsLat, esc.gpsLon), Icons.Filled.LocationOn, Color(0xFF1565C0))
    }
}

@Composable
private fun EscaneoFila(label: String, value: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(17.dp).padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = GrisMedio, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
        }
    }
}
