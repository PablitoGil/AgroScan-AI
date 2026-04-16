package com.example.agroscanai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.data.model.TipoCuenta
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.PerfilViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(
    onBackClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    perfilViewModel: PerfilViewModel = viewModel()
) {
    val usuario       by perfilViewModel.usuario.collectAsState()
    val isLoading     by perfilViewModel.isLoading.collectAsState()
    val mensajeExito  by perfilViewModel.mensajeExito.collectAsState()
    val error         by perfilViewModel.error.collectAsState()
    val snackbarHost  = remember { SnackbarHostState() }

    var nombre      by remember(usuario) { mutableStateOf(usuario?.nombre    ?: "") }
    var apellido    by remember(usuario) { mutableStateOf(usuario?.apellido  ?: "") }
    var telefono    by remember(usuario) { mutableStateOf(usuario?.telefono  ?: "") }
    var region      by remember(usuario) { mutableStateOf(usuario?.region    ?: "") }
    var email       by remember(usuario) { mutableStateOf(usuario?.email     ?: "") }
    var tipoCuenta  by remember(usuario) { mutableStateOf(usuario?.tipoCuenta ?: TipoCuenta.BASICO.name) }

    LaunchedEffect(mensajeExito) {
        mensajeExito?.let {
            snackbarHost.showSnackbar(it)
            perfilViewModel.clearMensajeExito()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHost.showSnackbar(it)
            perfilViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(snackbarData = data, containerColor = VerdeBosque, contentColor = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
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
            // ── Header ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(VerdeBosqueOscuro, VerdeBosque)))
                    .padding(bottom = 32.dp)
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
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(6.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    when (usuario?.tipoCuentaEnum()) {
                                        TipoCuenta.PREMIUM     -> Color(0xFF1565C0)
                                        TipoCuenta.EMPRESARIAL -> Color(0xFF6A1B9A)
                                        else -> VerdeBosque
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = usuario?.iniciales() ?: "?",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Editar perfil", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(usuario?.email ?: "", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                }
            }

            Spacer(Modifier.height((-16).dp))

            // ── Información personal ─────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape     = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text(
                        "Información personal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBosque,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    CampoTexto(
                        valor    = nombre,
                        onValor  = { nombre = it },
                        etiqueta = "Nombre",
                        icono    = Icons.Filled.Person,
                        teclado  = KeyboardOptions(
                            keyboardType   = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    CampoTexto(
                        valor    = apellido,
                        onValor  = { apellido = it },
                        etiqueta = "Apellido",
                        icono    = Icons.Filled.Person,
                        teclado  = KeyboardOptions(
                            keyboardType   = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    CampoTexto(
                        valor    = telefono,
                        onValor  = { telefono = it },
                        etiqueta = "Teléfono",
                        icono    = Icons.Filled.Phone,
                        teclado  = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    Spacer(Modifier.height(12.dp))

                    CampoTexto(
                        valor    = region,
                        onValor  = { region = it },
                        etiqueta = "Región / Localidad",
                        icono    = Icons.Filled.LocationOn,
                        teclado  = KeyboardOptions(
                            keyboardType   = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Correo electrónico ───────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Correo electrónico",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBosque,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    CampoTexto(
                        valor    = email,
                        onValor  = { email = it },
                        etiqueta = "Correo electrónico",
                        icono    = Icons.Filled.Email,
                        teclado  = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Text(
                        "Si cambias el correo, deberás volver a iniciar sesión recientemente.",
                        fontSize = 11.sp,
                        color    = GrisMedio,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Contraseña ───────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Contraseña",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBosque,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedButton(
                        onClick = { perfilViewModel.enviarResetContrasena() },
                        modifier = Modifier.fillMaxWidth(),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdeBosque),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, VerdeBosque)
                    ) {
                        Icon(Icons.Filled.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar enlace de restablecimiento", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Se enviará un correo a tu dirección registrada para que puedas cambiar tu contraseña.",
                        fontSize = 11.sp,
                        color    = GrisMedio,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Tipo de cuenta ───────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Tipo de cuenta",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBosque,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TipoCuentaChip(
                            etiqueta  = "Básico",
                            selected  = tipoCuenta == TipoCuenta.BASICO.name,
                            color     = VerdeBosque,
                            bgColor   = Color(0xFFE8F5E9),
                            onClick   = { tipoCuenta = TipoCuenta.BASICO.name },
                            modifier  = Modifier.weight(1f)
                        )
                        TipoCuentaChip(
                            etiqueta  = "Premium",
                            selected  = tipoCuenta == TipoCuenta.PREMIUM.name,
                            color     = Color(0xFF1565C0),
                            bgColor   = Color(0xFFE3F2FD),
                            onClick   = { tipoCuenta = TipoCuenta.PREMIUM.name },
                            modifier  = Modifier.weight(1f)
                        )
                        TipoCuentaChip(
                            etiqueta  = "Empresa",
                            selected  = tipoCuenta == TipoCuenta.EMPRESARIAL.name,
                            color     = Color(0xFF6A1B9A),
                            bgColor   = Color(0xFFF3E5F5),
                            onClick   = { tipoCuenta = TipoCuenta.EMPRESARIAL.name },
                            modifier  = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Botón Guardar ────────────────────────────────────────────────
            Button(
                onClick  = {
                    perfilViewModel.actualizarTodoPerfil(
                        nombre     = nombre,
                        apellido   = apellido,
                        telefono   = telefono,
                        region     = region,
                        tipoCuenta = tipoCuenta,
                        nuevoEmail = email
                    )
                },
                enabled  = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = VerdeBosque),
                shape    = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar cambios", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoTexto(
    valor: String,
    onValor: (String) -> Unit,
    etiqueta: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    teclado: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value           = valor,
        onValueChange   = onValor,
        label           = { Text(etiqueta) },
        leadingIcon     = { Icon(icono, contentDescription = null, tint = VerdeBosque) },
        keyboardOptions = teclado,
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(12.dp),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = VerdeBosque,
            unfocusedBorderColor = Color(0xFFCCCCCC),
            focusedLabelColor    = VerdeBosque
        ),
        singleLine      = true
    )
}

@Composable
private fun TipoCuentaChip(
    etiqueta: String,
    selected: Boolean,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg     = if (selected) color  else bgColor
    val fg     = if (selected) Color.White else color

    Box(
        modifier = modifier
            .height(42.dp)
            .background(bg, RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = color.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = etiqueta,
            fontSize   = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color      = fg
        )
    }
}
