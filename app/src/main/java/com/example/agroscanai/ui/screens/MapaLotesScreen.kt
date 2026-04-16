package com.example.agroscanai.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.R
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// Fuente de tiles de imágenes satelitales ESRI (gratuita, sin API key)
// ─────────────────────────────────────────────────────────────────────────────
private val ESRI_SATELITE = object : OnlineTileSourceBase(
    "ESRI_World_Imagery", 1, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex)     + "/" +
            MapTileIndex.getX(pMapTileIndex)
}

private enum class PasoMapa { SELECCIONAR, VER_MAPA }

@Composable
fun MapaLotesScreen(
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos  by cultivosViewModel.cultivos.collectAsState()
    val isLoading by cultivosViewModel.isLoading.collectAsState()

    var paso                by remember { mutableStateOf(PasoMapa.SELECCIONAR) }
    var cultivoSeleccionado by remember { mutableStateOf<Cultivo?>(null) }

    Scaffold(
        containerColor = Color(0xFFF5F7F5),
        bottomBar = {
            AgroBottomBar(
                onHomeClick           = onHomeClick,
                onNotificacionesClick = onNotificacionesClick,
                onPerfilClick         = onPerfilClick,
                paginaActual          = PaginaActual.NINGUNA
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // ── Encabezado ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .shadow(2.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.ArrowBack, "Regresar",
                    tint = VerdeBosque,
                    modifier = Modifier.size(26.dp).clickable {
                        if (paso == PasoMapa.VER_MAPA) {
                            paso = PasoMapa.SELECCIONAR
                            cultivoSeleccionado = null
                        } else onHomeClick()
                    }
                )
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.logo_agroscan), "Logo",
                        modifier = Modifier.size(32.dp))
                    Text("AgroScan AI", fontSize = 8.sp,
                        fontWeight = FontWeight.Bold, color = VerdeBosque)
                }
            }

            HorizontalDivider(color = Color(0xFFE8ECE8))

            when (paso) {
                PasoMapa.SELECCIONAR -> SeleccionarLoteStep(
                    cultivos = cultivos, isLoading = isLoading,
                    onSeleccionar = { cultivoSeleccionado = it; paso = PasoMapa.VER_MAPA }
                )
                PasoMapa.VER_MAPA -> MapaCalorStep(cultivo = cultivoSeleccionado!!)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 1 – Lista de lotes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SeleccionarLoteStep(
    cultivos: List<Cultivo>,
    isLoading: Boolean,
    onSeleccionar: (Cultivo) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VerdeEsmeralda)
            }
            cultivos.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Map, null,
                    tint = VerdeEsmeralda.copy(alpha = 0.4f), modifier = Modifier.size(80.dp))
                Spacer(Modifier.height(16.dp))
                Text("Sin parcelas registradas",
                    fontSize = 16.sp, color = GrisMedio, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Agrega un cultivo en \"Mis Cultivos\" para ver su mapa de calor.",
                    fontSize = 13.sp, color = GrisMedio.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cultivos, key = { it.id }) { c ->
                    LoteCard(cultivo = c, onClick = { onSeleccionar(c) })
                }
                item {
                    Text("AgroScan AI es una marca registrada. Todos los derechos reservados.",
                        fontSize = 10.sp, color = GrisMedio.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun LoteCard(cultivo: Cultivo, onClick: () -> Unit) {
    val estadoColor = when (cultivo.estadoEnum()) {
        EstadoCultivo.SALUDABLE   -> Color(0xFF4CAF50)
        EstadoCultivo.ALERTA      -> Color(0xFFFFA000)
        EstadoCultivo.CRITICO     -> Color(0xFFD32F2F)
        EstadoCultivo.SIN_ESCANEO -> GrisMedio
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(VerdeEsmeralda),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.ic_cultivos), null,
                    modifier = Modifier.size(32.dp), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(cultivo.nombre)
                        if (cultivo.tipoCultivo.isNotBlank()) append(" - ${cultivo.tipoCultivo}")
                    },
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(estadoColor))
                    Spacer(Modifier.width(5.dp))
                    Text("Ver Mapa de Calor", fontSize = 12.sp, color = GrisMedio)
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(width = 72.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp)).background(VerdeEsmeralda.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.ic_cultivos), null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop, alpha = 0.85f)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 2 – Mapa de calor con OSMDroid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapaCalorStep(cultivo: Cultivo) {
    var panelActivo by remember { mutableStateOf<String?>(null) }

    val tienePlagas = cultivo.plagasDetectadas
    val tieneEstres = cultivo.humedadPromedio in 0.01f..19.99f
    val confianzaIA = remember(cultivo.ultimoEscaneo) {
        if (cultivo.ultimoEscaneo.isNotBlank() && cultivo.ultimoEscaneo != "Sin escaneos aún")
            (90..100).random() else 0
    }

    // Coordenadas almacenadas como "lat,lon"
    val (lat, lon) = remember(cultivo.ubicacion) {
        val parts = cultivo.ubicacion.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        if (parts.size == 2) parts[0] to parts[1] else 19.4326 to -99.1332
    }

    // Radio del lote en metros a partir de hectáreas
    val radioMetros = remember(cultivo.hectareas) {
        (sqrt(cultivo.hectareas * 10_000.0 / PI) * 2.2).coerceAtLeast(60.0)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Tarjeta info del cultivo ────────────────────────────────────────
        val estadoColor = when (cultivo.estadoEnum()) {
            EstadoCultivo.SALUDABLE   -> Color(0xFF4CAF50)
            EstadoCultivo.ALERTA      -> Color(0xFFFFA000)
            EstadoCultivo.CRITICO     -> Color(0xFFD32F2F)
            EstadoCultivo.SIN_ESCANEO -> GrisMedio
        }
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Map, null, tint = VerdeEsmeralda, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        buildString {
                            append(cultivo.nombre)
                            if (cultivo.tipoCultivo.isNotBlank()) append(" · ${cultivo.tipoCultivo}")
                        },
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VerdeBosque,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        buildString {
                            append("${cultivo.hectareas} ha")
                            if (cultivo.ultimoEscaneo.isNotBlank() &&
                                cultivo.ultimoEscaneo != "Sin escaneos aún")
                                append(" · Escaneo: ${cultivo.ultimoEscaneo}")
                        },
                        fontSize = 11.sp, color = GrisMedio
                    )
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(estadoColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(cultivo.estadoTexto(), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, color = estadoColor)
                }
            }
        }

        // ── Mapa OSMDroid ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            OsmMapaCalor(
                lat = lat, lon = lon,
                radioMetros = radioMetros,
                cultivo = cultivo,
                modifier = Modifier.fillMaxSize()
            )

            // Coordenadas GPS
            if (panelActivo == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("GPS: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}",
                        color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }

                // Confianza IA
                if (confianzaIA > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 10.dp, bottom = 10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(VerdeEsmeralda.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("IA: $confianzaIA%", color = Color.White,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Alertas superiores
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (tienePlagas) MarcadorChip("⚠ Plaga Detectada", Color(0xFFD32F2F))
                    if (tieneEstres) MarcadorChip("⚠ Estrés Hídrico", Color(0xFFFFA000))
                    if (!tienePlagas && !tieneEstres && confianzaIA > 0)
                        MarcadorChip("✓ Cultivo Saludable", Color(0xFF2E7D32))
                }
            }

            // Panel overlay
            if (panelActivo != null) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { panelActivo = null },
                    contentAlignment = Alignment.Center
                ) {
                    MapaInfoPanel(
                        panel = panelActivo!!, cultivo = cultivo,
                        onClose = { panelActivo = null }
                    )
                }
            }
        }

        // ── Leyenda NDVI ────────────────────────────────────────────────────
        HeatMapLeyenda(estado = cultivo.estadoEnum())

        // ── Botones ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MapaBotonCircular("Plagas", painterResource(R.drawable.ic_plagas),
                isActive = panelActivo == "plagas", hasAlert = tienePlagas,
                onClick = { panelActivo = if (panelActivo == "plagas") null else "plagas" })
            MapaBotonCircular("Nutrientes", painterResource(R.drawable.ic_nutrientes),
                isActive = panelActivo == "nutrientes", hasAlert = cultivo.nitrogenio in 0.01f..39.99f,
                onClick = { panelActivo = if (panelActivo == "nutrientes") null else "nutrientes" })
            MapaBotonCircular("Satélite", painterResource(R.drawable.ic_satelite),
                isActive = panelActivo == "satelite", hasAlert = false,
                onClick = { panelActivo = if (panelActivo == "satelite") null else "satelite" })
        }

        Text("AgroScan AI es una marca registrada. Todos los derechos reservados.",
            fontSize = 9.sp, color = GrisMedio.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable: MapView de OSMDroid con mapa de calor
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OsmMapaCalor(
    lat: Double,
    lon: Double,
    radioMetros: Double,
    cultivo: Cultivo,
    modifier: Modifier = Modifier
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val centro        = remember(lat, lon) { GeoPoint(lat, lon) }

    val mapView = remember(context) {
        // Configurar OSMDroid antes de crear el mapa
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = java.io.File(context.cacheDir, "osmdroid_tiles")
        }

        MapView(context).apply {
            setTileSource(ESRI_SATELITE)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true

            controller.apply {
                setZoom(17.0)
                setCenter(centro)
            }

            // Overlay: mapa de calor NDVI simulado
            overlays.add(
                MapaCalorOverlay(
                    centro      = centro,
                    radioMetros = radioMetros,
                    cultivo     = cultivo
                )
            )

            // Marcador del lote
            val marker = Marker(this).apply {
                position = centro
                title    = cultivo.nombre
                snippet  = "${cultivo.estadoTexto()} · ${cultivo.hectareas} ha"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            overlays.add(marker)
        }
    }

    // Ciclo de vida del MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

// ─────────────────────────────────────────────────────────────────────────────
// Overlay: círculos de calor NDVI sobre el mapa nativo
// ─────────────────────────────────────────────────────────────────────────────

private class MapaCalorOverlay(
    private val centro: GeoPoint,
    private val radioMetros: Double,
    private val cultivo: Cultivo
) : Overlay() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(
        canvas: android.graphics.Canvas,
        mapView: MapView,
        shadow: Boolean
    ) {
        if (shadow) return

        val proj     = mapView.projection
        val centroPx = proj.toPixels(centro, null)
        val cx       = centroPx.x.toFloat()
        val cy       = centroPx.y.toFloat()

        // Radio en píxeles calculado a partir de metros
        val bordeGeo = GeoPoint(centro.latitude + radioMetros / 111_000.0, centro.longitude)
        val bordePx  = proj.toPixels(bordeGeo, null)
        val rPx      = abs(bordePx.y - centroPx.y).toFloat().coerceAtLeast(12f)

        val estado     = cultivo.estadoEnum()
        val sinEscaneo = cultivo.ultimoEscaneo.isBlank() ||
                cultivo.ultimoEscaneo == "Sin escaneos aún"

        // ── Capas concéntricas (de exterior a interior) ───────────────────
        when {
            sinEscaneo -> {
                circulo(canvas, cx, cy, rPx * 1.5f, 117, 117, 117, 12)
                circulo(canvas, cx, cy, rPx * 1.0f, 158, 158, 158, 45)
                circulo(canvas, cx, cy, rPx * 0.60f, 189, 189, 189, 75)
                circulo(canvas, cx, cy, rPx * 0.30f, 117, 117, 117, 100)
            }
            estado == EstadoCultivo.CRITICO -> {
                circulo(canvas, cx, cy, rPx * 1.7f, 255, 160,   0,  12) // amarillo exterior
                circulo(canvas, cx, cy, rPx * 1.1f, 229,  57,  53,  50) // rojo exterior
                circulo(canvas, cx, cy, rPx * 0.68f, 229,  57,  53,  95) // rojo medio
                circulo(canvas, cx, cy, rPx * 0.36f, 183,  28,  28, 160) // rojo oscuro núcleo
            }
            estado == EstadoCultivo.ALERTA -> {
                circulo(canvas, cx, cy, rPx * 1.7f, 255, 213,  79,  12) // amarillo claro
                circulo(canvas, cx, cy, rPx * 1.1f, 255, 160,   0,  50) // naranja exterior
                circulo(canvas, cx, cy, rPx * 0.68f, 255, 160,   0,  95) // naranja medio
                circulo(canvas, cx, cy, rPx * 0.36f, 230,  81,   0, 160) // naranja oscuro núcleo
            }
            else -> { // SALUDABLE
                circulo(canvas, cx, cy, rPx * 1.7f, 129, 199, 132,  12) // verde claro exterior
                circulo(canvas, cx, cy, rPx * 1.1f, 129, 199, 132,  50) // verde exterior
                circulo(canvas, cx, cy, rPx * 0.68f,  56, 142,  60,  95) // verde medio
                circulo(canvas, cx, cy, rPx * 0.36f,  27,  94,  32, 160) // verde oscuro núcleo
            }
        }

        // ── Borde del lote ────────────────────────────────────────────────
        val (br, bg, bb, ba) = when (estado) {
            EstadoCultivo.CRITICO   -> Cuadrupla(229,  57,  53, 110)
            EstadoCultivo.ALERTA    -> Cuadrupla(255, 160,   0, 110)
            EstadoCultivo.SALUDABLE -> Cuadrupla( 56, 142,  60, 110)
            else                    -> Cuadrupla(158, 158, 158,  80)
        }
        paint.color       = android.graphics.Color.argb(ba, br, bg, bb)
        paint.style       = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(cx, cy, rPx * 1.1f, paint)

        // ── Variaciones del campo (aspecto NDVI realista) ─────────────────
        if (!sinEscaneo) {
            val cosLat   = cos(centro.latitude * PI / 180.0)
            val mPorPx   = rPx / radioMetros.toFloat()
            val offsetPx = (radioMetros * 0.78 * mPorPx).toFloat()

            listOf(
                Triple(-1f, -1f, 0),
                Triple(-1f,  1f, 1),
                Triple( 1f, -1f, 2),
                Triple( 1f,  1f, 3)
            ).forEach { (dy, dx, idx) ->
                val vx = cx + dx * offsetPx * 0.75f
                val vy = cy + dy * offsetPx * 0.75f
                val (vr, vg, vb) = when {
                    estado == EstadoCultivo.CRITICO && idx % 2 == 0 -> Triple(229, 57, 53)
                    estado == EstadoCultivo.ALERTA  && idx < 2       -> Triple(102, 187, 106)
                    estado == EstadoCultivo.SALUDABLE                -> Triple( 46, 125,  50)
                    else -> Triple(255, 160, 0)
                }
                circuloEn(canvas, vx, vy, rPx * 0.40f, vr, vg, vb, 60)
            }

            // Indicador de zona húmeda
            if (cultivo.humedadPromedio > 0f) {
                val (hr, hg, hb) = when {
                    cultivo.humedadPromedio < 15f -> Triple(255, 143,   0)
                    cultivo.humedadPromedio < 25f -> Triple( 79, 195, 247)
                    else                          -> Triple(  2, 136, 209)
                }
                val offsetPxH = (radioMetros * 0.35 * mPorPx).toFloat()
                circuloEn(canvas, cx - offsetPxH, cy - offsetPxH, rPx * 0.27f, hr, hg, hb, 52)
            }
        }
    }

    private fun circulo(
        c: android.graphics.Canvas,
        cx: Float, cy: Float, r: Float,
        red: Int, green: Int, blue: Int, alpha: Int
    ) {
        paint.color = android.graphics.Color.argb(alpha, red, green, blue)
        paint.style = Paint.Style.FILL
        c.drawCircle(cx, cy, r, paint)
    }

    private fun circuloEn(
        c: android.graphics.Canvas,
        x: Float, y: Float, r: Float,
        red: Int, green: Int, blue: Int, alpha: Int
    ) {
        paint.color = android.graphics.Color.argb(alpha, red, green, blue)
        paint.style = Paint.Style.FILL
        c.drawCircle(x, y, r, paint)
    }

    private data class Cuadrupla(val a: Int, val b: Int, val cc: Int, val d: Int)
    private operator fun Cuadrupla.component1() = a
    private operator fun Cuadrupla.component2() = b
    private operator fun Cuadrupla.component3() = cc
    private operator fun Cuadrupla.component4() = d
}

// ─────────────────────────────────────────────────────────────────────────────
// Leyenda de colores del mapa de calor
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeatMapLeyenda(estado: EstadoCultivo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("NDVI:", fontSize = 10.sp, color = GrisMedio, fontWeight = FontWeight.Bold)
        listOf(
            Triple(Color(0xFF1B5E20), "Saludable", estado == EstadoCultivo.SALUDABLE),
            Triple(Color(0xFFFFA000), "Estrés",    estado == EstadoCultivo.ALERTA),
            Triple(Color(0xFFD32F2F), "Crítico",   estado == EstadoCultivo.CRITICO),
            Triple(Color(0xFF0288D1), "Húmedo",    false)
        ).forEach { (color, label, activo) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .then(if (activo)
                        Modifier.border(1.dp, color, RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.08f))
                    else Modifier)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(3.dp))
                Text(label, fontSize = 9.sp, color = if (activo) color else GrisMedio)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chip de alerta flotante
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MarcadorChip(texto: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.90f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(texto, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Botones circulares
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapaBotonCircular(
    label: String,
    painter: androidx.compose.ui.graphics.painter.Painter,
    isActive: Boolean,
    hasAlert: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier.size(110.dp).clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painter, contentDescription = label,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            if (hasAlert) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape)
                        .background(Color(0xFFE53935)).align(Alignment.TopEnd)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, color = VerdeBosque, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel de información
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapaInfoPanel(panel: String, cultivo: Cultivo, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(0.88f).clickable(enabled = false) {},
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
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VerdeBosque
                )
                Icon(Icons.Filled.Close, "Cerrar", tint = GrisMedio,
                    modifier = Modifier.size(20.dp).clickable { onClose() })
            }
            Spacer(Modifier.height(14.dp))
            when (panel) {
                "plagas"     -> PlagasInfo(cultivo)
                "nutrientes" -> NutrientesInfo(cultivo)
                "satelite"   -> SateliteInfo(cultivo)
            }
        }
    }
}

@Composable
private fun PlagasInfo(cultivo: Cultivo) {
    val tienePlagas = cultivo.plagasDetectadas
    val color = if (tienePlagas) Color(0xFFD32F2F) else Color(0xFF4CAF50)
    val icon  = if (tienePlagas) Icons.Filled.BugReport else Icons.Filled.CheckCircle
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(if (tienePlagas) "Plaga detectada en la parcela" else "Sin plagas detectadas",
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
    Spacer(Modifier.height(10.dp))
    Text(
        if (tienePlagas)
            "Se detectaron signos de plaga en el área escaneada. Se recomienda aplicar tratamiento preventivo en las zonas marcadas en rojo sobre el mapa."
        else
            "No se detectó ninguna plaga durante el último escaneo. El cultivo se encuentra libre de infestaciones.",
        fontSize = 13.sp, color = GrisHumo, lineHeight = 20.sp
    )
}

@Composable
private fun NutrientesInfo(cultivo: Cultivo) {
    if (cultivo.nitrogenio <= 0f && cultivo.fosforo <= 0f && cultivo.potasio <= 0f) {
        Text("Realiza un escaneo para obtener los niveles de nutrientes de esta parcela.",
            fontSize = 13.sp, color = GrisMedio, lineHeight = 20.sp)
        return
    }
    listOf(
        Triple("Nitrógeno (N)", cultivo.nitrogenio, 80f),
        Triple("Fósforo (P)",   cultivo.fosforo,    80f),
        Triple("Potasio (K)",   cultivo.potasio,    80f)
    ).forEach { (nombre, valor, max) ->
        val pct   = (valor / max).coerceIn(0f, 1f)
        val color = when {
            pct < 0.4f  -> Color(0xFFD32F2F)
            pct < 0.65f -> Color(0xFFFFA000)
            else        -> Color(0xFF4CAF50)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(nombre, fontSize = 12.sp, color = GrisHumo, modifier = Modifier.width(100.dp))
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color, trackColor = GrisClaro
            )
            Spacer(Modifier.width(8.dp))
            Text("${valor.toInt()}", fontSize = 12.sp, color = color,
                fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
        }
    }
}

@Composable
private fun SateliteInfo(cultivo: Cultivo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Satellite, null, tint = VerdeBosque, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text("Datos del último escaneo", fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, color = VerdeBosque)
    }
    Spacer(Modifier.height(10.dp))
    InfoRow("Parcela",        cultivo.nombre)
    InfoRow("Tipo",           cultivo.tipoCultivo.ifBlank { "—" })
    InfoRow("Área",           "${cultivo.hectareas} hectáreas")
    InfoRow("Último escaneo", cultivo.ultimoEscaneo.ifBlank { "Sin escaneos aún" })
    InfoRow("Estado",         cultivo.estadoTexto())
    if (cultivo.humedadPromedio > 0f)
        InfoRow("Humedad suelo", "${"%.1f".format(cultivo.humedadPromedio)}%")
    if (cultivo.ubicacion.isNotBlank())
        InfoRow("Coordenadas", cultivo.ubicacion)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = GrisMedio)
        Text(value, fontSize = 12.sp, color = GrisHumo, fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(max = 180.dp), textAlign = TextAlign.End)
    }
}
