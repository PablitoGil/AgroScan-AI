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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.PerfilViewModel

@Composable
fun ConfiguracionScreen(
    onBackClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    perfilViewModel: PerfilViewModel = viewModel()
) {
    val usuario    by perfilViewModel.usuario.collectAsState()
    val snackbar   = remember { SnackbarHostState() }

    var notifPush      by remember(usuario) { mutableStateOf(usuario?.notificacionesPush ?: true) }
    var sonidosAlerta  by remember(usuario) { mutableStateOf(usuario?.sonidosAlerta      ?: true) }
    var unidMetricas   by remember(usuario) { mutableStateOf(usuario?.unidadesMetricas   ?: true) }
    var modoOscuro     by remember(usuario) { mutableStateOf(usuario?.modoOscuro         ?: false) }

    // Persist changes as they toggle
    fun guardarConfig() {
        perfilViewModel.actualizarConfiguracion(notifPush, sonidosAlerta, unidMetricas, modoOscuro)
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
                IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Configuración", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Personaliza tu experiencia", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Notificaciones ────────────────────────────────────────────────
            ConfSeccionLabel("NOTIFICACIONES")
            ConfCard {
                ToggleItem(
                    icon      = Icons.Filled.Notifications,
                    iconBg    = Color(0xFFE8F5E9),
                    iconColor = VerdeBosque,
                    titulo    = "Notificaciones push",
                    subtitulo = "Alertas de parcelas en estado crítico",
                    checked   = notifPush,
                    onChecked = { notifPush = it; guardarConfig() }
                )
                ConfDivider()
                ToggleItem(
                    icon      = Icons.Filled.VolumeUp,
                    iconBg    = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF1565C0),
                    titulo    = "Sonidos de alerta",
                    subtitulo = "Reproducir sonido en notificaciones",
                    checked   = sonidosAlerta,
                    onChecked = { sonidosAlerta = it; guardarConfig() }
                )
            }

            // ── Unidades ──────────────────────────────────────────────────────
            ConfSeccionLabel("UNIDADES DE MEDIDA")
            ConfCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFFFFF3E0), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Straighten, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sistema de unidades", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                            Text("Selecciona el sistema de medición", fontSize = 12.sp, color = GrisMedio)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UnidadButton(
                            etiqueta  = "Métrico",
                            desc      = "kg, ha, °C",
                            selected  = unidMetricas,
                            onClick   = { unidMetricas = true; guardarConfig() },
                            modifier  = Modifier.weight(1f)
                        )
                        UnidadButton(
                            etiqueta  = "Imperial",
                            desc      = "lb, ac, °F",
                            selected  = !unidMetricas,
                            onClick   = { unidMetricas = false; guardarConfig() },
                            modifier  = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Idioma ────────────────────────────────────────────────────────
            ConfSeccionLabel("IDIOMA")
            ConfCard {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(Color(0xFFE8F5E9), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = VerdeBosque, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Idioma de la aplicación", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                        Text("Español (predeterminado)", fontSize = 12.sp, color = GrisMedio)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("ES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
                    }
                }
            }

            // ── Apariencia ────────────────────────────────────────────────────
            ConfSeccionLabel("APARIENCIA")
            ConfCard {
                ToggleItem(
                    icon      = Icons.Filled.Brightness4,
                    iconBg    = Color(0xFF2C2C4A),
                    iconColor = Color(0xFFBBBBEE),
                    titulo    = "Modo oscuro",
                    subtitulo = "Cambiar al tema oscuro de la aplicación",
                    checked   = modoOscuro,
                    onChecked = { modoOscuro = it; guardarConfig() }
                )
                ConfDivider()
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(Color(0xFFF3E5F5), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tema de color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                        Text("Verde agrícola (predeterminado)", fontSize = 12.sp, color = GrisMedio)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3E5F5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Pronto", fontSize = 11.sp, color = Color(0xFF6A1B9A), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Datos ─────────────────────────────────────────────────────────
            ConfSeccionLabel("DATOS Y PRIVACIDAD")
            ConfCard {
                InfoRow(
                    icon      = Icons.Filled.CloudSync,
                    iconBg    = Color(0xFFE0F7FA),
                    iconColor = Color(0xFF00695C),
                    titulo    = "Sincronización en la nube",
                    subtitulo = "Tus datos se guardan automáticamente en Firebase"
                )
                ConfDivider()
                InfoRow(
                    icon      = Icons.Filled.Security,
                    iconBg    = Color(0xFFE8F5E9),
                    iconColor = VerdeBosque,
                    titulo    = "Cifrado de datos",
                    subtitulo = "Tus datos agrícolas están protegidos con cifrado SSL"
                )
                ConfDivider()
                InfoRow(
                    icon      = Icons.Filled.Storage,
                    iconBg    = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFE65100),
                    titulo    = "Almacenamiento",
                    subtitulo = "Firebase Firestore · Google Cloud"
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun ConfSeccionLabel(texto: String) {
    Text(
        text       = texto,
        fontSize   = 11.sp,
        fontWeight = FontWeight.Bold,
        color      = GrisMedio,
        modifier   = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun ConfCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun ToggleItem(
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    titulo: String,
    subtitulo: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Text(subtitulo, fontSize = 12.sp, color = GrisMedio)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onChecked,
            colors          = SwitchDefaults.colors(
                checkedThumbColor        = Color.White,
                checkedTrackColor        = VerdeBosque,
                uncheckedThumbColor      = Color.White,
                uncheckedTrackColor      = Color(0xFFCCCCCC)
            )
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    titulo: String,
    subtitulo: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Text(subtitulo, fontSize = 12.sp, color = GrisMedio)
        }
    }
}

@Composable
private fun UnidadButton(
    etiqueta: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg     = if (selected) VerdeBosque else Color(0xFFF0F4F0)
    val fg     = if (selected) Color.White else GrisHumo
    val fgSub  = if (selected) Color.White.copy(alpha = 0.8f) else GrisMedio

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = fg),
        border   = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (selected) VerdeBosque else Color(0xFFCCCCCC)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(etiqueta, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
            Text(desc, fontSize = 10.sp, color = fgSub)
        }
    }
}

@Composable
private fun ConfDivider() {
    Divider(modifier = Modifier.padding(start = 70.dp, end = 16.dp), color = Color(0xFFF0F0F0), thickness = 0.8.dp)
}
