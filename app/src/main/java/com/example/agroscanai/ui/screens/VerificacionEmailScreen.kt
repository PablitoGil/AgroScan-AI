package com.example.agroscanai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.AuthState
import com.example.agroscanai.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun VerificacionEmailScreen(
    email: String,
    onBackClick: () -> Unit,
    onVerified: (nombre: String) -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var secondsLeft by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        canResend = true
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                authViewModel.resetState()
                onVerified(state.displayName ?: "")
            }
            is AuthState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                authViewModel.resetState()
            }
            else -> Unit
        }
    }

    val isLoading = authState is AuthState.Loading

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = VerdeBosque,
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
                    IconButton(
                        onClick = {
                            authViewModel.signOutUnverified()
                            onBackClick()
                        },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBackIosNew,
                            contentDescription = "Regresar",
                            tint = VerdeBosque,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verifica tu correo",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBosque
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            color = VerdeEsmeralda.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MarkEmailUnread,
                        contentDescription = null,
                        tint = VerdeEsmeralda,
                        modifier = Modifier.size(58.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Revisa tu bandeja de entrada",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerdeBosque,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Enviamos un enlace de verificación a:",
                    fontSize = 14.sp,
                    color = GrisMedio,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = email,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VerdeEsmeralda,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Haz clic en el enlace del correo y luego regresa aquí para continuar.",
                    fontSize = 14.sp,
                    color = GrisMedio,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { authViewModel.checkEmailVerified() },
                    enabled = !isLoading,
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
                            text = "Ya verifiqué mi correo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (canResend) {
                    TextButton(
                        onClick = {
                            canResend = false
                            secondsLeft = 60
                            authViewModel.resendVerificationEmail()
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Reenviar enlace de verificación",
                            color = VerdeEsmeralda,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "Reenviar enlace en ${secondsLeft}s",
                        fontSize = 13.sp,
                        color = GrisMedio
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            Text(
                text = "AgroScan AI es una marca registrada. Todos los derechos reservados.",
                fontSize = 11.sp,
                color = GrisMedio.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 20.dp)
            )
        }
    }
}
