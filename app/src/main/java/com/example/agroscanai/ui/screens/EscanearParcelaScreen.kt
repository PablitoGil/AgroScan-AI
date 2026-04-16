package com.example.agroscanai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.R
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import com.example.agroscanai.ui.viewmodel.DroneEstado
import com.example.agroscanai.ui.viewmodel.DroneViewModel
import com.example.agroscanai.ui.viewmodel.ResultadoEscaneo

private enum class PasoEscaneo { SELECCIONAR_PARCELA, ESCANEAR }

@Composable
fun EscanearParcelaScreen(
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    droneViewModel: DroneViewModel = viewModel(),
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val estado       by droneViewModel.estado.collectAsState()
    val droneInfo    by droneViewModel.droneInfo.collectAsState()
    val resultado    by droneViewModel.resultado.collectAsState()
    val mensajeEstado by droneViewModel.mensajeEstado.collectAsState()
    val errorMensaje by droneViewModel.errorMensaje.collectAsState()
    val modoSimulacion by droneViewModel.modoSimulacion.collectAsState()
    val cultivos     by cultivosViewModel.cultivos.collectAsState()
    val isLoading    by cultivosViewModel.isLoading.collectAsState()

    var paso                  by remember { mutableStateOf(PasoEscaneo.SELECCIONAR_PARCELA) }
    var cultivoSeleccionado   by remember { mutableStateOf<Cultivo?>(null) }
    var infoActivaPanel       by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoConexion by remember { mutableStateOf(false) }
    var escaneadoGuardado     by remember { mutableStateOf(false) }

    LaunchedEffect(estado) {
        if (estado == DroneEstado.ESCANEANDO) infoActivaPanel = null
    }

    LaunchedEffect(estado) {
        if (estado == DroneEstado.ESCANEO_COMPLETO && !escaneadoGuardado) {
            cultivoSeleccionado?.let { cultivo ->
                cultivosViewModel.guardarResultadoEscaneo(cultivo.id, resultado)
                escaneadoGuardado = true
            }
        }
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
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(innerPadding)) {

        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).shadow(2.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack, "Regresar", tint = VerdeBosque,
                modifier = Modifier.size(26.dp).clickable {
                    if (paso == PasoEscaneo.ESCANEAR) {
                        paso = PasoEscaneo.SELECCIONAR_PARCELA
                        cultivoSeleccionado = null
                        droneViewModel.reiniciarEscaneo()
                        escaneadoGuardado = false
                    } else {
                        onHomeClick()
                    }
                }
            )
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.logo_agroscan), "Logo",
                    modifier = Modifier.size(32.dp))
                Text("AgroScan AI", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    color = VerdeBosque, lineHeight = 10.sp)
            }
        }

        HorizontalDivider(color = Color(0xFFE8ECE8), thickness = 1.dp)

        when (paso) {
            PasoEscaneo.SELECCIONAR_PARCELA -> {
                SeleccionarParcelaStep(
                    cultivos = cultivos,
                    isLoading = isLoading,
                    onSeleccionar = { cultivo ->
                        cultivoSeleccionado = cultivo
                        escaneadoGuardado = false
                        paso = PasoEscaneo.ESCANEAR
                    }
                )
            }
            PasoEscaneo.ESCANEAR -> {
                EscaneoStep(
                    cultivo = cultivoSeleccionado!!,
                    estado = estado,
                    droneInfo = droneInfo,
                    resultado = resultado,
                    mensajeEstado = mensajeEstado,
                    errorMensaje = errorMensaje,
                    modoSimulacion = modoSimulacion,
                    infoActivaPanel = infoActivaPanel,
                    escaneadoGuardado = escaneadoGuardado,
                    onCambiarParcela = {
                        cultivoSeleccionado = null
                        droneViewModel.reiniciarEscaneo()
                        escaneadoGuardado = false
                        paso = PasoEscaneo.SELECCIONAR_PARCELA
                    },
                    onInfoPanelClick = { panel ->
                        infoActivaPanel = if (infoActivaPanel == panel) null else panel
                    },
                    onConectarClick = { mostrarDialogoConexion = true },
                    onDesconectarClick = { droneViewModel.desconectar() },
                    onEscanearClick = {
                        when (estado) {
                            DroneEstado.DESCONECTADO, DroneEstado.ERROR ->
                                mostrarDialogoConexion = true
                            DroneEstado.CONECTADO ->
                                droneViewModel.iniciarEscaneo()
                            DroneEstado.ESCANEO_COMPLETO -> {
                                droneViewModel.reiniciarEscaneo()
                                escaneadoGuardado = false
                            }
                            else -> {}
                        }
                    },
                    onClearError = { droneViewModel.clearError() }
                )
            }
        }

        if (mostrarDialogoConexion) {
            DroneConnectionDialog(
                onDismiss = { mostrarDialogoConexion = false },
                onConectarWifi = {
                    mostrarDialogoConexion = false
                    droneViewModel.buscarYConectarDron()
                },
                onSimular = {
                    mostrarDialogoConexion = false
                    droneViewModel.conectarModoSimulacion()
                }
            )
        }
    } // end Column
    } // end Scaffold content
}

@Composable
private fun SeleccionarParcelaStep(
    cultivos: List<Cultivo>,
    isLoading: Boolean,
    onSeleccionar: (Cultivo) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().background(Color.White)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("Escanear Parcela", fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = VerdeBosque)
            Text("Selecciona la parcela que deseas escanear",
                fontSize = 13.sp, color = GrisMedio)
        }

        HorizontalDivider(color = Color(0xFFE8ECE8), thickness = 1.dp)

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdeEsmeralda)
                }
            }
            cultivos.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Grass, null, tint = GrisMedio,
                        modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Sin parcelas registradas", fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, color = GrisHumo)
                    Spacer(Modifier.height(8.dp))
                    Text("Primero agrega un cultivo en\n\"Mis Cultivos\" para poder escanearlo.",
                        fontSize = 13.sp, color = GrisMedio, textAlign = TextAlign.Center,
                        lineHeight = 20.sp)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cultivos) { cultivo ->
                        ParcelaSeleccionCard(cultivo = cultivo, onClick = { onSeleccionar(cultivo) })
                    }
                    item {
                        Text(
                            "AgroScan AI es una marca registrada. Todos los derechos reservados.",
                            fontSize = 11.sp, color = GrisMedio, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParcelaSeleccionCard(cultivo: Cultivo, onClick: () -> Unit) {
    val estadoColor = when (cultivo.estadoEnum()) {
        EstadoCultivo.SALUDABLE  -> VerdeEsmeralda
        EstadoCultivo.ALERTA     -> Color(0xFFFFA000)
        EstadoCultivo.CRITICO    -> RojoAlerta
        EstadoCultivo.SIN_ESCANEO -> GrisMedio
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(VerdeEsmeralda.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Grass, null, tint = VerdeEsmeralda,
                    modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(cultivo.nombre, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, color = VerdeBosque)
                Text(
                    buildString {
                        append(cultivo.tipoCultivo)
                        if (cultivo.variedadSemilla.isNotBlank()) append(" · ${cultivo.variedadSemilla}")
                    },
                    fontSize = 12.sp, color = GrisMedio
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Straighten, null, tint = GrisMedio,
                            modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("${cultivo.hectareas} ha", fontSize = 11.sp, color = GrisMedio)
                    }
                    if (cultivo.ubicacion.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = GrisMedio,
                                modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(cultivo.ubicacion, fontSize = 11.sp, color = GrisMedio,
                                maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(estadoColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(cultivo.estadoTexto(), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, color = estadoColor)
                }
                Spacer(Modifier.height(8.dp))
                Icon(Icons.Filled.ChevronRight, null, tint = GrisMedio,
                    modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun EscaneoStep(
    cultivo: Cultivo,
    estado: DroneEstado,
    droneInfo: com.example.agroscanai.ui.viewmodel.DroneInfo,
    resultado: ResultadoEscaneo,
    mensajeEstado: String,
    errorMensaje: String?,
    modoSimulacion: Boolean,
    infoActivaPanel: String?,
    escaneadoGuardado: Boolean,
    onCambiarParcela: () -> Unit,
    onInfoPanelClick: (String) -> Unit,
    onConectarClick: () -> Unit,
    onDesconectarClick: () -> Unit,
    onEscanearClick: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Grass, null, tint = VerdeEsmeralda,
                    modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cultivo.nombre, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, color = VerdeBosque)
                    Text(
                        buildString {
                            append(cultivo.tipoCultivo)
                            if (cultivo.ubicacion.isNotBlank()) append(" · ${cultivo.ubicacion}")
                            append(" · ${cultivo.hectareas} ha")
                        },
                        fontSize = 12.sp, color = GrisMedio
                    )
                }
                if (estado != DroneEstado.ESCANEANDO) {
                    TextButton(
                        onClick = onCambiarParcela,
                        colors = ButtonDefaults.textButtonColors(contentColor = VerdeEsmeralda),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Cambiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        DroneStatusBar(
            estado = estado, droneInfo = droneInfo,
            mensajeEstado = mensajeEstado, modoSimulacion = modoSimulacion,
            onConectarClick = onConectarClick, onDesconectarClick = onDesconectarClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .height(260.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2D5A27))
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_cultivos),
                contentDescription = "Campo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                        startY = 100f
                    )
                )
            )
            if (estado == DroneEstado.ESCANEANDO) ScanLineAnimation()

            if (infoActivaPanel != null && estado == DroneEstado.ESCANEO_COMPLETO) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    InfoPanelOverlay(
                        panel = infoActivaPanel,
                        resultado = resultado,
                        onClose = { onInfoPanelClick(infoActivaPanel) }
                    )
                }
            }

            if (estado == DroneEstado.ESCANEANDO || estado == DroneEstado.ESCANEO_COMPLETO) {
                Text(
                    "GPS: ${"%.4f".format(resultado.gpsLon)}, ${"%.4f".format(resultado.gpsLat)}",
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 40.dp)
                )
                Text(
                    "Confianza IA: ${resultado.confianzaIA}%",
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 40.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text("Escaneo al: ${resultado.progresoPercent}%",
                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { resultado.progresoPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = VerdeEsmeralda, trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            if (estado == DroneEstado.ESCANEANDO) {
                DroneIconAnimated(modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp))
            }

            if (estado == DroneEstado.DESCONECTADO || estado == DroneEstado.BUSCANDO ||
                estado == DroneEstado.CONECTANDO || estado == DroneEstado.ERROR
            ) {
                Column(modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Air, null, tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Conecta el dron\npara iniciar el escaneo",
                        color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                        textAlign = TextAlign.Center)
                }
            }

            if (estado == DroneEstado.CONECTADO) {
                Column(modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FlightTakeoff, null, tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Dron listo\nPresiona ESCANEAR",
                        color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                        textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            InfoCircleButton("Plagas", Icons.Filled.BugReport,
                enabled = estado == DroneEstado.ESCANEO_COMPLETO,
                isActive = infoActivaPanel == "plagas",
                hasAlert = resultado.plagasDetectadas,
                onClick = { onInfoPanelClick("plagas") })
            InfoCircleButton("Nutrientes", Icons.Filled.Science,
                enabled = estado == DroneEstado.ESCANEO_COMPLETO,
                isActive = infoActivaPanel == "nutrientes",
                hasAlert = resultado.nivelNitrogenio < 40f && resultado.nivelNitrogenio > 0f,
                onClick = { onInfoPanelClick("nutrientes") })
            InfoCircleButton("Suelo", Icons.Filled.Terrain,
                enabled = estado == DroneEstado.ESCANEO_COMPLETO,
                isActive = infoActivaPanel == "suelo",
                hasAlert = resultado.humedadSuelo < 20f && resultado.humedadSuelo > 0f,
                onClick = { onInfoPanelClick("suelo") })
        }

        Spacer(Modifier.height(20.dp))

        if (estado == DroneEstado.BUSCANDO || estado == DroneEstado.CONECTANDO) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = VerdeEsmeralda,
                    modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(mensajeEstado, fontSize = 13.sp, color = GrisHumo)
            }
        }

        errorMensaje?.let { err ->
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F3)),
                shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Warning, null, tint = RojoAlerta,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(err, fontSize = 12.sp, color = RojoAlerta, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.Close, "Cerrar", tint = GrisMedio,
                        modifier = Modifier.size(18.dp).clickable { onClearError() })
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (estado == DroneEstado.ESCANEO_COMPLETO) {
            if (escaneadoGuardado) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = VerdeEsmeralda,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Escaneo guardado en \"${cultivo.nombre}\"",
                            fontSize = 12.sp, color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            ResumenResultados(resultado = resultado,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onEscanearClick,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (estado) {
                    DroneEstado.ESCANEANDO -> GrisMedio
                    DroneEstado.ESCANEO_COMPLETO -> VerdeBosqueOscuro
                    else -> VerdeEsmeralda
                },
                contentColor = Color.White
            ),
            enabled = estado != DroneEstado.ESCANEANDO &&
                    estado != DroneEstado.BUSCANDO &&
                    estado != DroneEstado.CONECTANDO
        ) {
            when (estado) {
                DroneEstado.ESCANEANDO -> {
                    CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Escaneando...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                DroneEstado.ESCANEO_COMPLETO -> {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nuevo escaneo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                else -> {
                    Icon(Icons.Filled.FlightTakeoff, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (estado == DroneEstado.DESCONECTADO || estado == DroneEstado.ERROR)
                            "Conectar y escanear" else "Escanear",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("AgroScan AI es una marca registrada. Todos los derechos reservados.",
            fontSize = 11.sp, color = GrisMedio, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DroneStatusBar(
    estado: DroneEstado,
    droneInfo: com.example.agroscanai.ui.viewmodel.DroneInfo,
    mensajeEstado: String,
    modoSimulacion: Boolean,
    onConectarClick: () -> Unit,
    onDesconectarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor: Color
    val textColor: Color
    val iconColor: Color
    val statusText: String
    when (estado) {
        DroneEstado.CONECTADO, DroneEstado.ESCANEO_COMPLETO -> {
            bgColor = Color(0xFFE8F5E9); textColor = Color(0xFF2E7D32)
            iconColor = Color(0xFF4CAF50)
            statusText = if (modoSimulacion) "Simulación activa" else "Dron conectado"
        }
        DroneEstado.ESCANEANDO -> {
            bgColor = Color(0xFFFFF9C4); textColor = Color(0xFFF57F17)
            iconColor = Color(0xFFFFC107); statusText = "Escaneando..."
        }
        DroneEstado.BUSCANDO, DroneEstado.CONECTANDO -> {
            bgColor = Color(0xFFE3F2FD); textColor = Color(0xFF1565C0)
            iconColor = Color(0xFF2196F3); statusText = "Conectando..."
        }
        DroneEstado.ERROR -> {
            bgColor = Color(0xFFFFEBEE); textColor = Color(0xFFC62828)
            iconColor = Color(0xFFE53935); statusText = "Error de conexión"
        }
        else -> {
            bgColor = Color(0xFFF5F5F5); textColor = GrisHumo
            iconColor = GrisMedio; statusText = "Sin conexión"
        }
    }

    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                val icon = when (estado) {
                    DroneEstado.CONECTADO, DroneEstado.ESCANEO_COMPLETO -> Icons.Filled.FlightTakeoff
                    DroneEstado.ESCANEANDO -> Icons.Filled.Air
                    DroneEstado.BUSCANDO, DroneEstado.CONECTANDO -> Icons.Filled.Search
                    DroneEstado.ERROR -> Icons.Filled.Warning
                    else -> Icons.Filled.FlightLand
                }
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(statusText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = textColor)
                if (estado == DroneEstado.CONECTADO || estado == DroneEstado.ESCANEO_COMPLETO) {
                    if (droneInfo.modelo.isNotBlank())
                        Text(droneInfo.modelo, fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                    if (droneInfo.bateria > 0)
                        Text("Batería: ${droneInfo.bateria}%  •  GPS: ${droneInfo.señalGPS} satélites",
                            fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                } else if (mensajeEstado.isNotBlank()) {
                    Text(mensajeEstado.lines().first(), fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.7f))
                }
            }
            when (estado) {
                DroneEstado.DESCONECTADO, DroneEstado.ERROR ->
                    TextButton(onClick = onConectarClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = VerdeEsmeralda)) {
                        Text("Conectar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                DroneEstado.CONECTADO, DroneEstado.ESCANEO_COMPLETO ->
                    TextButton(onClick = onDesconectarClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = RojoAlerta)) {
                        Text("Desconectar", fontSize = 12.sp)
                    }
                else -> {}
            }
        }
    }
}

@Composable
private fun InfoCircleButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    isActive: Boolean,
    hasAlert: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(when { !enabled -> Color(0xFFE0E0E0); isActive -> VerdeBosque; else -> VerdeEsmeralda })
                    .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (!enabled) GrisMedio else Color.White,
                    modifier = Modifier.size(28.dp))
            }
            if (hasAlert && enabled) {
                Box(
                    modifier = Modifier.size(16.dp).clip(CircleShape).background(RojoAlerta)
                        .align(Alignment.TopEnd).border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (enabled) VerdeBosque else GrisMedio)
    }
}

@Composable
private fun InfoPanelOverlay(panel: String, resultado: ResultadoEscaneo, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(0.9f).padding(horizontal = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (panel) {
                        "suelo" -> "Información de suelo"
                        "plagas" -> "Información de plagas"
                        else -> "Información de nutrientes"
                    },
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VerdeBosque
                )
                Icon(Icons.Filled.Close, "Cerrar", tint = GrisMedio,
                    modifier = Modifier.size(20.dp).clickable { onClose() })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE8ECE8))
            when (panel) {
                "suelo" -> {
                    Text(resultado.estresSuelo, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = GrisHumo)
                    Spacer(Modifier.height(4.dp))
                    Text(resultado.descripcionSuelo, fontSize = 12.sp, color = GrisHumo, lineHeight = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    NutrientBar("Humedad", resultado.humedadSuelo / 100f,
                        "${resultado.humedadSuelo.toInt()}%",
                        if (resultado.humedadSuelo < 20f) RojoAlerta else VerdeEsmeralda)
                }
                "plagas" -> {
                    Text(if (resultado.plagasDetectadas) "⚠ Plagas detectadas" else "✓ Sin plagas",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (resultado.plagasDetectadas) RojoAlerta else VerdeEsmeralda)
                    Spacer(Modifier.height(4.dp))
                    Text(resultado.descripcionPlagas, fontSize = 12.sp, color = GrisHumo, lineHeight = 18.sp)
                }
                else -> {
                    Text(resultado.descripcionNutrientes, fontSize = 12.sp, color = GrisHumo, lineHeight = 18.sp)
                    if (resultado.recomendacionNutrientes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(resultado.recomendacionNutrientes, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, color = VerdeBosque, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    NutrientBar("Nitrógeno (N)", resultado.nivelNitrogenio / 100f,
                        "${resultado.nivelNitrogenio.toInt()}%",
                        if (resultado.nivelNitrogenio < 40f) RojoAlerta else VerdeEsmeralda)
                    Spacer(Modifier.height(4.dp))
                    NutrientBar("Fósforo (P)", resultado.nivelFosforo / 100f,
                        "${resultado.nivelFosforo.toInt()}%",
                        if (resultado.nivelFosforo < 45f) Color(0xFFFFA000) else VerdeEsmeralda)
                    Spacer(Modifier.height(4.dp))
                    NutrientBar("Potasio (K)", resultado.nivelPotasio / 100f,
                        "${resultado.nivelPotasio.toInt()}%", VerdeEsmeralda)
                }
            }
        }
    }
}

@Composable
private fun NutrientBar(label: String, progress: Float, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, color = GrisMedio, modifier = Modifier.width(110.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color, trackColor = Color(0xFFE8ECE8)
        )
        Spacer(Modifier.width(6.dp))
        Text(value, fontSize = 11.sp, color = GrisHumo, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun ResumenResultados(resultado: ResultadoEscaneo, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Resumen del escaneo", fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = VerdeBosque)
            Spacer(Modifier.height(8.dp))
            ResultadoRow(Icons.Filled.Terrain, "Suelo",
                resultado.estresSuelo.ifBlank { "Sin datos" },
                if (resultado.humedadSuelo < 20f && resultado.humedadSuelo > 0f) RojoAlerta else VerdeEsmeralda)
            ResultadoRow(Icons.Filled.BugReport, "Plagas",
                if (resultado.plagasDetectadas) "Detectadas" else "No detectadas",
                if (resultado.plagasDetectadas) RojoAlerta else VerdeEsmeralda)
            ResultadoRow(Icons.Filled.Science, "Nutrientes",
                if (resultado.nivelNitrogenio < 40f && resultado.nivelNitrogenio > 0f)
                    "Nitrógeno bajo" else "Niveles normales",
                if (resultado.nivelNitrogenio < 40f && resultado.nivelNitrogenio > 0f)
                    Color(0xFFFFA000) else VerdeEsmeralda)
        }
    }
}

@Composable
private fun ResultadoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String, color: Color
) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontSize = 12.sp, color = GrisHumo, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ScanLineAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scanLine"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopStart)
                .offset(y = (offsetY * 254).dp)
                .background(
                    Brush.horizontalGradient(colors = listOf(
                        Color.Transparent, VerdeEsmeralda.copy(alpha = 0.8f),
                        VerdeEsmeralda, VerdeEsmeralda.copy(alpha = 0.8f), Color.Transparent
                    ))
                )
        )
    }
}

@Composable
private fun DroneIconAnimated(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "drone")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "droneFloat"
    )
    Box(
        modifier = modifier.size(44.dp).clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f)).offset(y = offsetY.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.FlightTakeoff, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun DroneConnectionDialog(
    onDismiss: () -> Unit,
    onConectarWifi: () -> Unit,
    onSimular: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
            Column(modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.FlightTakeoff, null, tint = VerdeEsmeralda,
                        modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Conectar dron", fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, color = VerdeBosque)
                Spacer(Modifier.height(8.dp))
                Text("Selecciona cómo conectar el dron para realizar el escaneo.",
                    fontSize = 13.sp, color = GrisMedio,
                    textAlign = TextAlign.Center, lineHeight = 19.sp)
                Spacer(Modifier.height(20.dp))
                ConnectionOptionButton(
                    icon = Icons.Filled.Wifi,
                    title = "Conectar por WiFi (MAVLink)",
                    subtitle = "Conecta con drones DJI, ArduPilot, PX4. Asegúrate de estar en el WiFi del dron.",
                    onClick = onConectarWifi
                )
                Spacer(Modifier.height(10.dp))
                ConnectionOptionButton(
                    icon = Icons.Filled.Science,
                    title = "Modo simulación",
                    subtitle = "Prueba la app sin dron físico. Los datos serán generados por IA.",
                    onClick = onSimular, secondary = true
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = GrisMedio, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("Compatible con: DJI Phantom, Mavic, Mini, Tello · ArduPilot · PX4",
                    fontSize = 10.sp, color = GrisMedio, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ConnectionOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String,
    onClick: () -> Unit, secondary: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (secondary) Color(0xFFF5F5F5) else Color(0xFFE8F5E9)
        ),
        border = if (!secondary)
            androidx.compose.foundation.BorderStroke(1.dp, VerdeEsmeralda.copy(alpha = 0.4f))
        else null
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(if (secondary) Color(0xFFE0E0E0) else VerdeEsmeralda.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (secondary) GrisMedio else VerdeEsmeralda,
                    modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (secondary) GrisHumo else VerdeBosque)
                Text(subtitle, fontSize = 11.sp, color = GrisMedio, lineHeight = 16.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = GrisMedio, modifier = Modifier.size(20.dp))
        }
    }
}
