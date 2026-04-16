package com.example.agroscanai.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.data.model.PrecioMercado
import com.example.agroscanai.data.model.TendenciaPrecio
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.PreciosUiState
import com.example.agroscanai.ui.viewmodel.PreciosViewModel

@Composable
fun PreciosScreen(
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    viewModel: PreciosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.TrendingUp, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Precios de Mercado", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        when (uiState) {
                            is PreciosUiState.Exito  -> "Futuros agrícolas · Tiempo real"
                            is PreciosUiState.Cargando -> "Actualizando..."
                            else                     -> "Sin conexión"
                        },
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            when (val state = uiState) {
                is PreciosUiState.Cargando -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = VerdeBosque)
                            Spacer(Modifier.height(12.dp))
                            Text("Obteniendo cotizaciones...", color = GrisMedio, fontSize = 14.sp)
                        }
                    }
                }
                is PreciosUiState.Error -> {
                    Box(
                        Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.SignalWifiOff, null,
                                tint = GrisMedio, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(state.mensaje, color = GrisMedio, fontSize = 14.sp,
                                textAlign = TextAlign.Center, lineHeight = 20.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.cargarPrecios() },
                                colors  = ButtonDefaults.buttonColors(containerColor = VerdeBosque)
                            ) { Text("Reintentar") }
                        }
                    }
                }
                is PreciosUiState.Exito -> {
                    PreciosContenido(state.precios, onReintentar = { viewModel.cargarPrecios() })
                }
            }
        }
    }
}

// ── Contenido principal ───────────────────────────────────────────────────────

@Composable
private fun PreciosContenido(precios: List<PrecioMercado>, onReintentar: () -> Unit) {
    val subiendo = precios.count { it.tendencia == TendenciaPrecio.SUBIENDO }
    val estable  = precios.count { it.tendencia == TendenciaPrecio.ESTABLE  }
    val bajando  = precios.count { it.tendencia == TendenciaPrecio.BAJANDO  }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Resumen del mercado ────────────────────────────────────────────
        Text("RESUMEN DEL MERCADO", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = GrisMedio, modifier = Modifier.padding(start = 24.dp, bottom = 6.dp))
        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChipResumen(subiendo.toString(), "Subiendo", Color(0xFF2E7D32), Icons.Filled.TrendingUp)
                ChipResumen(estable.toString(),  "Estables", Color(0xFF1565C0), Icons.Filled.TrendingFlat)
                ChipResumen(bajando.toString(),  "Bajando",  Color(0xFFC62828), Icons.Filled.TrendingDown)
            }
        }

        // ── Alertas automáticas ────────────────────────────────────────────
        val mejores = precios.filter { it.tendencia == TendenciaPrecio.SUBIENDO }
        val enBaja  = precios.filter { it.tendencia == TendenciaPrecio.BAJANDO  }

        if (mejores.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            AlertaBanner(
                texto = "Mejor momento para vender: ${mejores.joinToString(", ") { it.grano }}",
                fondo = Color(0xFFE8F5E9),
                color = Color(0xFF1B5E20),
                icon  = Icons.Filled.Star
            )
        }
        if (enBaja.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            AlertaBanner(
                texto = "Precios en baja: ${enBaja.joinToString(", ") { it.grano }} — evalúa almacenar.",
                fondo = Color(0xFFFFEBEE),
                color = Color(0xFFB71C1C),
                icon  = Icons.Filled.Warning
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Cotizaciones ─────────────────────────────────────────────────
        Text("COTIZACIONES EN TIEMPO REAL", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = GrisMedio, modifier = Modifier.padding(start = 24.dp, bottom = 6.dp))

        precios.forEach { precio ->
            TarjetaCotizacion(precio)
            Spacer(Modifier.height(10.dp))
        }

        // ── Fuente y actualizar ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onReintentar) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp), tint = GrisMedio)
                Spacer(Modifier.width(4.dp))
                Text("Actualizar", fontSize = 12.sp, color = GrisMedio)
            }
        }
        Text(
            "Fuente: Yahoo Finance (futuros CBOT/ICE) · Precios en USD/tonelada · Referencia internacional",
            fontSize = 11.sp, color = GrisMedio, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ── Composables internos ──────────────────────────────────────────────────────

@Composable
private fun ChipResumen(
    valor: String, etiqueta: String, color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(valor,    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(etiqueta, fontSize = 11.sp, color = GrisMedio)
    }
}

@Composable
private fun AlertaBanner(
    texto: String, fondo: Color, color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors    = CardDefaults.cardColors(containerColor = fondo),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(texto, fontSize = 13.sp, color = color, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun TarjetaCotizacion(precio: PrecioMercado) {
    val tendenciaColor = tendenciaColor(precio.tendencia)
    val tendenciaIcon  = tendenciaIcon(precio.tendencia)
    val tendenciaLabel = tendenciaLabel(precio.tendencia)
    val tendenciaBg    = tendenciaBg(precio.tendencia)

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Cabecera
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(VerdeBosque.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Grass, null, tint = VerdeBosque, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(precio.grano, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Text("USD / ${precio.unidad}", fontSize = 12.sp, color = GrisMedio)
                }
                Box(
                    modifier = Modifier
                        .background(tendenciaBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(tendenciaIcon, null, tint = tendenciaColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tendenciaLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tendenciaColor)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Precios
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ValorPrecio("Actual", "$${String.format("%.1f", precio.precioActual)}", tendenciaColor, grande = true)
                ValorPrecio("Mínimo", "$${String.format("%.1f", precio.precioMinimo)}", GrisMedio,      grande = false)
                ValorPrecio("Máximo", "$${String.format("%.1f", precio.precioMaximo)}", GrisMedio,      grande = false)
            }

            Spacer(Modifier.height(10.dp))

            // Barra de rango
            val rango = (precio.precioMaximo - precio.precioMinimo).toFloat()
            val progreso = if (rango > 0)
                ((precio.precioActual - precio.precioMinimo) / rango).toFloat().coerceIn(0f, 1f)
            else 0.5f
            LinearProgressIndicator(
                progress   = { progreso },
                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = tendenciaColor,
                trackColor = Color(0xFFEEEEEE)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$${precio.precioMinimo.toInt()}", fontSize = 10.sp, color = GrisMedio)
                Text("$${precio.precioMaximo.toInt()}", fontSize = 10.sp, color = GrisMedio)
            }

            Spacer(Modifier.height(10.dp))

            // Recomendación
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tendenciaBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Filled.Lightbulb, null, tint = tendenciaColor,
                    modifier = Modifier.size(15.dp).padding(top = 1.dp))
                Spacer(Modifier.width(6.dp))
                Text(precio.recomendacion, fontSize = 12.sp, color = tendenciaColor, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun ValorPrecio(label: String, valor: String, color: Color, grande: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = GrisMedio)
        Text(
            valor,
            fontSize   = if (grande) 20.sp else 14.sp,
            fontWeight = if (grande) FontWeight.Bold else FontWeight.SemiBold,
            color      = color
        )
    }
}

private fun tendenciaColor(t: TendenciaPrecio) = when (t) {
    TendenciaPrecio.SUBIENDO -> Color(0xFF2E7D32)
    TendenciaPrecio.BAJANDO  -> Color(0xFFC62828)
    TendenciaPrecio.ESTABLE  -> Color(0xFF1565C0)
}
private fun tendenciaIcon(t: TendenciaPrecio) = when (t) {
    TendenciaPrecio.SUBIENDO -> Icons.Filled.TrendingUp
    TendenciaPrecio.BAJANDO  -> Icons.Filled.TrendingDown
    TendenciaPrecio.ESTABLE  -> Icons.Filled.TrendingFlat
}
private fun tendenciaLabel(t: TendenciaPrecio) = when (t) {
    TendenciaPrecio.SUBIENDO -> "SUBIENDO"
    TendenciaPrecio.BAJANDO  -> "BAJANDO"
    TendenciaPrecio.ESTABLE  -> "ESTABLE"
}
private fun tendenciaBg(t: TendenciaPrecio) = when (t) {
    TendenciaPrecio.SUBIENDO -> Color(0xFFE8F5E9)
    TendenciaPrecio.BAJANDO  -> Color(0xFFFFEBEE)
    TendenciaPrecio.ESTABLE  -> Color(0xFFE3F2FD)
}
