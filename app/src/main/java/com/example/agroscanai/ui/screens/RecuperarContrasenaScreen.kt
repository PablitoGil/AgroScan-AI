package com.example.agroscanai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.AuthState
import com.example.agroscanai.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecuperarContrasenaScreen(
    onBackClick: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var correo by remember { mutableStateOf("") }
    var emailEnviado by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.PasswordResetSent -> {
                emailEnviado = true
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                snackbarHostState.showSnackbar((authState as AuthState.Error).message)
                authViewModel.resetState()
            }
            else -> Unit
        }
    }

    val isLoading = authState is AuthState.Loading

    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = GrisClaro,
        unfocusedContainerColor = GrisClaro,
        focusedTextColor = GrisHumo,
        unfocusedTextColor = GrisHumo,
        focusedLabelColor = VerdeBosque,
        unfocusedLabelColor = GrisMedio,
        focusedIndicatorColor = VerdeEsmeralda,
        unfocusedIndicatorColor = GrisMedio,
        cursorColor = VerdeEsmeralda
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = RojoAlerta,
                    contentColor = Color.White
                )
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(52.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBackIosNew,
                            contentDescription = "Regresar",
                            tint = VerdeBosque,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Recuperar contraseña",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBosque
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!emailEnviado) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = VerdeEsmeralda.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(26.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockReset,
                            contentDescription = null,
                            tint = VerdeEsmeralda,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBosque,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ingresa tu correo y te enviaremos un enlace para que puedas crear una nueva contraseña.",
                        fontSize = 14.sp,
                        color = GrisMedio,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { authViewModel.sendPasswordResetEmail(correo) },
                        enabled = correo.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VerdeEsmeralda,
                            disabledContainerColor = VerdeEsmeralda.copy(alpha = 0.4f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Enviar enlace de recuperación",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = VerdeEsmeralda.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(26.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MarkEmailUnread,
                            contentDescription = null,
                            tint = VerdeEsmeralda,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "¡Correo enviado!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBosque,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Enviamos un enlace de recuperación a:",
                        fontSize = 14.sp,
                        color = GrisMedio,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = correo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VerdeEsmeralda,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Haz clic en el enlace del correo para crear tu nueva contraseña. Revisa también tu carpeta de spam.",
                        fontSize = 14.sp,
                        color = GrisMedio,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onBackClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeEsmeralda)
                    ) {
                        Text(
                            text = "Volver al inicio de sesión",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "AgroScan AI es una marca registrada. Todos los derechos reservados.",
                    fontSize = 11.sp,
                    color = GrisMedio.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}
