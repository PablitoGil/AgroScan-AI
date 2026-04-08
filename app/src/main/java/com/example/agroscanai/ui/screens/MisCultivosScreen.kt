package com.example.agroscanai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisCultivosScreen(
    nombreUsuario: String = "",
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos      by cultivosViewModel.cultivos.collectAsState()
    val isLoading     by cultivosViewModel.isLoading.collectAsState()
    val error         by cultivosViewModel.error.collectAsState()
    val snackbarHost  = remember { SnackbarHostState() }

    var selectedCultivo  by remember { mutableStateOf<Cultivo?>(null) }
    var showAddSheet     by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHost.showSnackbar(it)
            cultivosViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(snackbarData = data, containerColor = RojoAlerta, contentColor = Color.White)
            }
        },
        containerColor = Color(0xFFF5F7F5)
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

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
                            .clickable { onHomeClick() }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_agroscan),
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Text("AgroScan AI", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buscar cultivo...", fontSize = 13.sp, color = GrisMedio)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(VerdeBosque)
                            .clickable { onPerfilClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (nombreUsuario.isNotBlank()) {
                            Text(
                                nombreUsuario.first().uppercaseChar().toString(),
                                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Home, "Inicio",
                        tint = GrisMedio,
                        modifier = Modifier.size(28.dp).clickable { onHomeClick() }
                    )
                    Box {
                        Icon(
                            Icons.Filled.Notifications, "Notificaciones",
                            tint = GrisMedio,
                            modifier = Modifier.size(28.dp).clickable { onNotificacionesClick() }
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE8ECE8))

                Text(
                    text = "Mis Cultivos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerdeBosque,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )

                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = VerdeEsmeralda)
                        }
                    }
                    cultivos.isEmpty() -> {
                        Box(
                            Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Grass, null,
                                    tint = VerdeEsmeralda.copy(alpha = 0.4f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Aún no tienes cultivos registrados",
                                    fontSize = 16.sp, color = GrisMedio,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Toca el botón + para agregar tu primer cultivo",
                                    fontSize = 13.sp, color = GrisMedio.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = 4.dp, bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(cultivos, key = { it.id }) { cultivo ->
                                CultivoCard(
                                    cultivo = cultivo,
                                    onClick = { selectedCultivo = cultivo }
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(VerdeEsmeralda)
                        .clickable { showAddSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, "Añadir", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text("Añadir cultivo", fontSize = 11.sp, color = VerdeBosque, fontWeight = FontWeight.Medium)
            }

            Text(
                text = "AgroScan AI es una marca registrada. Todos los derechos reservados.",
                fontSize = 10.sp, color = GrisMedio.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 6.dp)
            )
        }
    }

    selectedCultivo?.let { cultivo ->
        ModalBottomSheet(
            onDismissRequest = { selectedCultivo = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            DetalleCultivoSheet(
                cultivo = cultivo,
                onEliminar = {
                    cultivosViewModel.eliminarCultivo(cultivo.id)
                    selectedCultivo = null
                },
                onDismiss = { selectedCultivo = null }
            )
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            AnadirCultivoSheet(
                onGuardar = { nombre, tipo, variedad, fecha, hectareas, ubicacion ->
                    cultivosViewModel.agregarCultivo(nombre, tipo, variedad, fecha, hectareas, ubicacion)
                    showAddSheet = false
                },
                onDismiss = { showAddSheet = false }
            )
        }
    }
}

@Composable
private fun CultivoCard(cultivo: Cultivo, onClick: () -> Unit) {
    val estadoColor = when (cultivo.estadoEnum()) {
        EstadoCultivo.SALUDABLE   -> Color(0xFF4CAF50)
        EstadoCultivo.ALERTA      -> Color(0xFFFFA000)
        EstadoCultivo.CRITICO     -> Color(0xFFD32F2F)
        EstadoCultivo.SIN_ESCANEO -> GrisMedio
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(VerdeEsmeralda.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_cultivos),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cultivo.nombre,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(estadoColor)
                )
                Spacer(Modifier.width(4.dp))
                Text("Salud - ${cultivo.estadoTexto()}", fontSize = 12.sp, color = GrisMedio)
            }
            Text("Último escaneo - ${cultivo.ultimoEscaneo}", fontSize = 12.sp, color = GrisMedio)
            Text("Área - ${cultivo.hectareas} hectáreas", fontSize = 12.sp, color = GrisMedio)
        }

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(width = 70.dp, height = 60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VerdeEsmeralda.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Grass, null, tint = VerdeEsmeralda, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun DetalleCultivoSheet(
    cultivo: Cultivo,
    onEliminar: () -> Unit,
    onDismiss: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = cultivo.nombre,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VerdeBosque
        )
        if (cultivo.tipoCultivo.isNotBlank()) {
            Text(cultivo.tipoCultivo, fontSize = 13.sp, color = GrisMedio)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE8ECE8))
        Spacer(Modifier.height(16.dp))

        DetalleSeccion(titulo = "Salud general") {
            Text(cultivo.descripcionSalud(), fontSize = 14.sp, color = GrisHumo, lineHeight = 22.sp)
        }

        DetalleSeccion(titulo = "Último escaneo") {
            Text(cultivo.ultimoEscaneo, fontSize = 14.sp, color = GrisHumo)
        }

        DetalleSeccion(titulo = "Área - ${cultivo.hectareas} hectáreas") {
            val descripcion = if (cultivo.variedadSemilla.isNotBlank())
                "Esta parcela cubre un total de ${cultivo.hectareas} hectáreas y está compuesta de ${cultivo.tipoCultivo}${if (cultivo.variedadSemilla.isNotBlank()) " (${cultivo.variedadSemilla})" else ""}."
            else
                "Esta parcela cubre un total de ${cultivo.hectareas} hectáreas."
            Text(descripcion, fontSize = 14.sp, color = GrisHumo, lineHeight = 22.sp)
        }

        if (cultivo.ubicacion.isNotBlank()) {
            DetalleSeccion(titulo = "Ubicación") {
                Text(cultivo.ubicacion, fontSize = 14.sp, color = GrisHumo)
            }
        }

        if (cultivo.fechaSiembra.isNotBlank()) {
            DetalleSeccion(titulo = "Fecha de siembra") {
                Text(cultivo.fechaSiembra, fontSize = 14.sp, color = GrisHumo)
            }
        }

        Spacer(Modifier.height(20.dp))

        if (showConfirmDelete) {
            Text(
                "¿Estás seguro de que deseas eliminar este cultivo?",
                fontSize = 14.sp, color = RojoAlerta, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showConfirmDelete = false },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancelar", color = GrisHumo) }
                Button(
                    onClick = onEliminar,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RojoAlerta)
                ) { Text("Eliminar", color = Color.White) }
            }
        } else {
            OutlinedButton(
                onClick = { showConfirmDelete = true },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RojoAlerta),
                border = androidx.compose.foundation.BorderStroke(1.dp, RojoAlerta)
            ) {
                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Eliminar cultivo")
            }
        }
    }
}

@Composable
private fun DetalleSeccion(titulo: String, content: @Composable () -> Unit) {
    Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
    Spacer(Modifier.height(4.dp))
    content()
    Spacer(Modifier.height(14.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnadirCultivoSheet(
    onGuardar: (String, String, String, String, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre          by remember { mutableStateOf("") }
    var tipoCultivo     by remember { mutableStateOf("") }
    var variedadSemilla by remember { mutableStateOf("") }
    var fechaSiembra    by remember { mutableStateOf("") }
    var hectareasStr    by remember { mutableStateOf("") }
    var ubicacion       by remember { mutableStateOf("") }

    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor   = GrisClaro,
        unfocusedContainerColor = GrisClaro,
        focusedTextColor        = GrisHumo,
        unfocusedTextColor      = GrisHumo,
        focusedLabelColor       = VerdeBosque,
        unfocusedLabelColor     = GrisMedio,
        focusedIndicatorColor   = VerdeEsmeralda,
        unfocusedIndicatorColor = GrisMedio,
        cursorColor             = VerdeEsmeralda
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Añadir Cultivo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
        Text("Llena los campos, por favor", fontSize = 13.sp, color = GrisMedio)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre, onValueChange = { nombre = it },
            label = { Text("Nombre del Cultivo") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = fieldColors
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = tipoCultivo, onValueChange = { tipoCultivo = it },
            label = { Text("Tipo de cultivo") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = fieldColors
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = variedadSemilla, onValueChange = { variedadSemilla = it },
            label = { Text("Variedad de semilla") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = fieldColors
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = fechaSiembra, onValueChange = { fechaSiembra = it },
            label = { Text("Fecha de siembra (dd/mm/aaaa)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = hectareasStr, onValueChange = { hectareasStr = it },
            label = { Text("Extensión (hectáreas)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ubicacion, onValueChange = { ubicacion = it },
            label = { Text("Ubicación (Coordenadas)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = fieldColors
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val hectareas = hectareasStr.toDoubleOrNull() ?: 0.0
                onGuardar(nombre, tipoCultivo, variedadSemilla, fechaSiembra, hectareas, ubicacion)
            },
            enabled = nombre.isNotBlank() && tipoCultivo.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VerdeEsmeralda,
                disabledContainerColor = VerdeEsmeralda.copy(alpha = 0.4f)
            )
        ) {
            Text("Guardar cultivo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar", color = GrisMedio, fontSize = 14.sp)
        }
    }
}
