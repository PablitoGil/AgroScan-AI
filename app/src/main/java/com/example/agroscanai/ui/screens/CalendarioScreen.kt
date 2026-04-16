package com.example.agroscanai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agroscanai.data.model.PrioridadTarea
import com.example.agroscanai.data.model.Tarea
import com.example.agroscanai.data.model.TipoTarea
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CalendarioViewModel
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Helpers ───────────────────────────────────────────────────────────────────

private val MESES_ES = listOf(
    "Enero","Febrero","Marzo","Abril","Mayo","Junio",
    "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
)

private val DIAS_ES = listOf("L","M","X","J","V","S","D")

private fun diasEnMes(mes: Int, anio: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(anio, mes, 1)
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun primerDiaSemana(mes: Int, anio: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(anio, mes, 1)
    // Lunes=0..Domingo=6
    return (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
}

private fun formatearFecha(dia: Int, mes: Int, anio: Int): String {
    return String.format("%02d/%02d/%04d", dia, mes + 1, anio)
}

private fun colorTipo(tipo: TipoTarea): Color = when (tipo) {
    TipoTarea.RIEGO          -> Color(0xFF1565C0)
    TipoTarea.FERTILIZACION  -> Color(0xFFE65100)
    TipoTarea.FUMIGACION     -> Color(0xFFD32F2F)
    TipoTarea.COSECHA        -> Color(0xFF6A1B9A)
    TipoTarea.PODA           -> Color(0xFF00695C)
    TipoTarea.ESCANEO        -> Color(0xFF37474F)
    TipoTarea.RECOMENDACION  -> Color(0xFFF9A825)
    TipoTarea.MANUAL         -> VerdeBosque
}

private fun iconoTipo(tipo: TipoTarea): ImageVector = when (tipo) {
    TipoTarea.RIEGO          -> Icons.Filled.WaterDrop
    TipoTarea.FERTILIZACION  -> Icons.Filled.Science
    TipoTarea.FUMIGACION     -> Icons.Filled.BugReport
    TipoTarea.COSECHA        -> Icons.Filled.Grass
    TipoTarea.PODA           -> Icons.Filled.ContentCut
    TipoTarea.ESCANEO        -> Icons.Filled.CameraAlt
    TipoTarea.RECOMENDACION  -> Icons.Filled.Lightbulb
    TipoTarea.MANUAL         -> Icons.Filled.Assignment
}

private fun etiquetaTipo(tipo: TipoTarea): String = when (tipo) {
    TipoTarea.RIEGO          -> "Riego"
    TipoTarea.FERTILIZACION  -> "Fertilización"
    TipoTarea.FUMIGACION     -> "Fumigación"
    TipoTarea.COSECHA        -> "Cosecha"
    TipoTarea.PODA           -> "Poda"
    TipoTarea.ESCANEO        -> "Escaneo"
    TipoTarea.RECOMENDACION  -> "Recomendación"
    TipoTarea.MANUAL         -> "Manual"
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreen(
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel,
    calendarioViewModel: CalendarioViewModel
) {
    val tareas      by calendarioViewModel.tareas.collectAsState()
    val cultivos    by cultivosViewModel.cultivos.collectAsState()
    val mensajeExito by calendarioViewModel.mensajeExito.collectAsState()
    val snackbar    = remember { SnackbarHostState() }

    val hoy     = remember { Calendar.getInstance() }
    var mesActual  by remember { mutableStateOf(hoy.get(Calendar.MONTH)) }
    var anioActual by remember { mutableStateOf(hoy.get(Calendar.YEAR)) }
    var diaSeleccionado by remember { mutableStateOf(hoy.get(Calendar.DAY_OF_MONTH)) }

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(mensajeExito) {
        mensajeExito?.let {
            snackbar.showSnackbar(it)
            calendarioViewModel.clearMensajeExito()
        }
    }

    val fechaSeleccionada = formatearFecha(diaSeleccionado, mesActual, anioActual)
    val tareasDelDia = tareas.filter { it.fecha == fechaSeleccionada }
    val tareasDelMes = tareas.filter {
        it.fecha.endsWith("/${mesActual + 1}/${anioActual}") ||
                it.fecha.length >= 10 && run {
                    val parts = it.fecha.split("/")
                    parts.size == 3 &&
                            parts[1].toIntOrNull() == (mesActual + 1) &&
                            parts[2].toIntOrNull() == anioActual
                }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color(0xFFF0F4F0),
        bottomBar = {
            AgroBottomBar(
                onHomeClick = onHomeClick,
                onNotificacionesClick = onNotificacionesClick,
                onPerfilClick = onPerfilClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                containerColor = VerdeBosque,
                contentColor   = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva tarea")
            }
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
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Calendario de Tareas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Planifica y gestiona tus actividades", fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Navegación de mes ─────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Fila navegación
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (mesActual == 0) { mesActual = 11; anioActual-- }
                            else mesActual--
                            diaSeleccionado = 1
                        }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Mes anterior", tint = VerdeBosque)
                        }
                        Text(
                            "${MESES_ES[mesActual].uppercase()} $anioActual",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF1A1A1A)
                        )
                        IconButton(onClick = {
                            if (mesActual == 11) { mesActual = 0; anioActual++ }
                            else mesActual++
                            diaSeleccionado = 1
                        }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Mes siguiente", tint = VerdeBosque)
                        }
                    }

                    // Cabecera días
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DIAS_ES.forEach { d ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(d, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrisMedio)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Grilla de días
                    val diasTotales = diasEnMes(mesActual, anioActual)
                    val primerDia  = primerDiaSemana(mesActual, anioActual)
                    val totalCeldas = primerDia + diasTotales

                    val filasNecesarias = (totalCeldas + 6) / 7
                    for (fila in 0 until filasNecesarias) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..6) {
                                val index = fila * 7 + col
                                val dia = index - primerDia + 1
                                if (dia < 1 || dia > diasTotales) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val fecha = formatearFecha(dia, mesActual, anioActual)
                                    val tareasDia = tareas.filter { it.fecha == fecha }
                                    val esHoy = dia == hoy.get(Calendar.DAY_OF_MONTH) &&
                                            mesActual == hoy.get(Calendar.MONTH) &&
                                            anioActual == hoy.get(Calendar.YEAR)
                                    val seleccionado = dia == diaSeleccionado

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    seleccionado -> VerdeBosque
                                                    esHoy        -> VerdeBosque.copy(alpha = 0.12f)
                                                    else         -> Color.Transparent
                                                }
                                            )
                                            .clickable { diaSeleccionado = dia },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                dia.toString(),
                                                fontSize   = 13.sp,
                                                fontWeight = if (esHoy || seleccionado) FontWeight.Bold else FontWeight.Normal,
                                                color      = when {
                                                    seleccionado -> Color.White
                                                    esHoy        -> VerdeBosque
                                                    else         -> Color(0xFF1A1A1A)
                                                }
                                            )
                                            if (tareasDia.isNotEmpty()) {
                                                Row(horizontalArrangement = Arrangement.Center) {
                                                    tareasDia.take(3).forEach { t ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (seleccionado) Color.White
                                                                    else colorTipo(t.tipoEnum())
                                                                )
                                                        )
                                                        Spacer(Modifier.width(2.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Tareas del día seleccionado ────────────────────────────────────
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 16.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TAREAS DEL ${diaSeleccionado} ${MESES_ES[mesActual].uppercase()}",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = GrisMedio,
                    modifier   = Modifier.weight(1f)
                )
                if (tareasDelDia.isNotEmpty()) {
                    Text(
                        "${tareasDelDia.count { it.completada }}/${tareasDelDia.size} completadas",
                        fontSize = 11.sp,
                        color    = VerdeBosque
                    )
                }
            }

            if (tareasDelDia.isEmpty()) {
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    shape     = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.EventAvailable, contentDescription = null,
                                tint = GrisMedio, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Sin tareas para este día", fontSize = 14.sp, color = GrisMedio)
                            Text("Pulsa + para agregar una tarea", fontSize = 12.sp, color = GrisMedio)
                        }
                    }
                }
            } else {
                tareasDelDia.forEach { tarea ->
                    TareaCard(
                        tarea = tarea,
                        onToggle  = { calendarioViewModel.toggleCompletada(tarea.id, tarea.completada) },
                        onEliminar = { calendarioViewModel.eliminarTarea(tarea.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Resumen del mes ───────────────────────────────────────────────
            if (tareasDelMes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "RESUMEN DEL MES",
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
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ResumenMesChip(tareasDelMes.size.toString(),              "Total",       Color(0xFF1A1A1A))
                        ResumenMesChip(tareasDelMes.count { it.completada }.toString(), "Hechas",    VerdeBosque)
                        ResumenMesChip(tareasDelMes.count { !it.completada }.toString(), "Pendientes", Color(0xFFF57C00))
                    }
                }
            }

            Spacer(Modifier.height(88.dp)) // espacio para FAB
        }
    }

    // ── BottomSheet: nueva tarea ──────────────────────────────────────────────
    if (showSheet) {
        NuevaTareaSheet(
            sheetState         = sheetState,
            fechaPreseleccionada = fechaSeleccionada,
            cultivos           = cultivos.map { it.id to it.nombre },
            onDismiss          = { showSheet = false },
            onGuardar          = { titulo, desc, fecha, hora, tipo, prioridad, cultivoId, cultivoNombre ->
                calendarioViewModel.agregarTarea(titulo, desc, fecha, hora, tipo, prioridad, cultivoId, cultivoNombre)
                showSheet = false
            }
        )
    }
}

// ── Composables privados ──────────────────────────────────────────────────────

@Composable
private fun TareaCard(
    tarea: Tarea,
    onToggle: () -> Unit,
    onEliminar: () -> Unit
) {
    var confirmarEliminar by remember { mutableStateOf(false) }
    val tipo  = tarea.tipoEnum()
    val color = colorTipo(tipo)

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked  = tarea.completada,
                onCheckedChange = { onToggle() },
                colors   = CheckboxDefaults.colors(checkedColor = VerdeBosque)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconoTipo(tipo), contentDescription = null,
                    tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tarea.titulo,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (tarea.completada) GrisMedio else Color(0xFF1A1A1A),
                    textDecoration = if (tarea.completada) TextDecoration.LineThrough else null
                )
                if (tarea.descripcion.isNotBlank())
                    Text(tarea.descripcion, fontSize = 11.sp, color = GrisMedio, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(etiquetaTipo(tipo), fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
                    }
                    if (tarea.hora.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(tarea.hora, fontSize = 10.sp, color = GrisMedio)
                    }
                    if (tarea.cultivoNombre.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text("• ${tarea.cultivoNombre}", fontSize = 10.sp, color = GrisMedio)
                    }
                }
            }
            IconButton(onClick = { confirmarEliminar = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar",
                    tint = GrisMedio, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title   = { Text("Eliminar tarea") },
            text    = { Text("¿Eliminar \"${tarea.titulo}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { onEliminar(); confirmarEliminar = false }) {
                    Text("Eliminar", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ResumenMesChip(valor: String, etiqueta: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor,    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(etiqueta, fontSize = 11.sp, color = GrisMedio)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaTareaSheet(
    sheetState: SheetState,
    fechaPreseleccionada: String,
    cultivos: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onGuardar: (String, String, String, String, TipoTarea, PrioridadTarea, String, String) -> Unit
) {
    var titulo       by remember { mutableStateOf("") }
    var descripcion  by remember { mutableStateOf("") }
    var fecha        by remember { mutableStateOf(fechaPreseleccionada) }
    var hora         by remember { mutableStateOf("") }
    var tipoSelec    by remember { mutableStateOf(TipoTarea.MANUAL) }
    var prioridadSel by remember { mutableStateOf(PrioridadTarea.MEDIA) }
    var cultivoSelId by remember { mutableStateOf("") }
    var cultivoSelNombre by remember { mutableStateOf("") }

    val tiposOrden = listOf(
        TipoTarea.RIEGO, TipoTarea.FERTILIZACION, TipoTarea.FUMIGACION,
        TipoTarea.COSECHA, TipoTarea.PODA, TipoTarea.ESCANEO, TipoTarea.MANUAL
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Nueva tarea", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value         = titulo,
                onValueChange = { titulo = it },
                label         = { Text("Título *") },
                leadingIcon   = { Icon(Icons.Filled.Title, contentDescription = null, tint = VerdeBosque) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = VerdeBosque,
                    unfocusedBorderColor = Color(0xFFCCCCCC),
                    focusedLabelColor    = VerdeBosque
                ),
                singleLine = true
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value         = descripcion,
                onValueChange = { descripcion = it },
                label         = { Text("Descripción (opcional)") },
                leadingIcon   = { Icon(Icons.Filled.Notes, contentDescription = null, tint = VerdeBosque) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = VerdeBosque,
                    unfocusedBorderColor = Color(0xFFCCCCCC),
                    focusedLabelColor    = VerdeBosque
                ),
                maxLines = 2
            )

            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value         = fecha,
                    onValueChange = { fecha = it },
                    label         = { Text("Fecha") },
                    leadingIcon   = { Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = VerdeBosque) },
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = VerdeBosque,
                        unfocusedBorderColor = Color(0xFFCCCCCC),
                        focusedLabelColor    = VerdeBosque
                    ),
                    singleLine = true,
                    placeholder = { Text("dd/MM/yyyy") }
                )
                OutlinedTextField(
                    value         = hora,
                    onValueChange = { hora = it },
                    label         = { Text("Hora") },
                    leadingIcon   = { Icon(Icons.Filled.Schedule, contentDescription = null, tint = VerdeBosque) },
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = VerdeBosque,
                        unfocusedBorderColor = Color(0xFFCCCCCC),
                        focusedLabelColor    = VerdeBosque
                    ),
                    singleLine = true,
                    placeholder = { Text("HH:mm") }
                )
            }

            Spacer(Modifier.height(12.dp))

            Text("Tipo de tarea", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Spacer(Modifier.height(6.dp))
            val filasTipo = tiposOrden.chunked(4)
            filasTipo.forEach { fila ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fila.forEach { tipo ->
                        val sel = tipoSelec == tipo
                        val col = colorTipo(tipo)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(if (sel) col else col.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .border(1.dp, if (sel) col else col.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable { tipoSelec = tipo },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                etiquetaTipo(tipo),
                                fontSize   = 10.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color      = if (sel) Color.White else col
                            )
                        }
                    }
                    // Rellenar fila si tiene menos de 4
                    repeat(4 - fila.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(4.dp))
            Text("Prioridad", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(PrioridadTarea.BAJA, PrioridadTarea.MEDIA, PrioridadTarea.ALTA, PrioridadTarea.URGENTE).forEach { p ->
                    val sel = prioridadSel == p
                    val col = when (p) {
                        PrioridadTarea.BAJA    -> Color(0xFF4CAF50)
                        PrioridadTarea.MEDIA   -> Color(0xFFF57C00)
                        PrioridadTarea.ALTA    -> Color(0xFFD32F2F)
                        PrioridadTarea.URGENTE -> Color(0xFF880E4F)
                    }
                    val etq = when (p) {
                        PrioridadTarea.BAJA    -> "Baja"
                        PrioridadTarea.MEDIA   -> "Media"
                        PrioridadTarea.ALTA    -> "Alta"
                        PrioridadTarea.URGENTE -> "Urgente"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(if (sel) col else col.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, if (sel) col else col.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { prioridadSel = p },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(etq, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) Color.White else col)
                    }
                }
            }

            if (cultivos.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Cultivo (opcional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Opción "ninguno"
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .background(
                                if (cultivoSelId.isEmpty()) VerdeBosque else Color(0xFFE8F5E9),
                                RoundedCornerShape(10.dp)
                            )
                            .border(1.dp,
                                if (cultivoSelId.isEmpty()) VerdeBosque else VerdeBosque.copy(alpha = 0.3f),
                                RoundedCornerShape(10.dp))
                            .clickable { cultivoSelId = ""; cultivoSelNombre = "" }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ninguno", fontSize = 11.sp,
                            color = if (cultivoSelId.isEmpty()) Color.White else VerdeBosque)
                    }
                }
                Spacer(Modifier.height(4.dp))
                cultivos.chunked(2).forEach { fila ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        fila.forEach { (id, nombre) ->
                            val sel = cultivoSelId == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .background(
                                        if (sel) VerdeBosque else Color(0xFFE8F5E9),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(1.dp,
                                        if (sel) VerdeBosque else VerdeBosque.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp))
                                    .clickable { cultivoSelId = id; cultivoSelNombre = nombre },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(nombre, fontSize = 11.sp,
                                    color = if (sel) Color.White else VerdeBosque,
                                    maxLines = 1)
                            }
                        }
                        if (fila.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick  = {
                    if (titulo.isNotBlank()) {
                        onGuardar(titulo, descripcion, fecha, hora, tipoSelec, prioridadSel, cultivoSelId, cultivoSelNombre)
                    }
                },
                enabled  = titulo.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = VerdeBosque),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Guardar tarea", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
