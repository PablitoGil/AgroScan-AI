package com.example.agroscanai.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agroscanai.R
import com.example.agroscanai.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun BienvenidaScreen(
    nombreUsuario: String,
    onFinished: () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val alphaSubtitle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(900))
        delay(300)
        alphaSubtitle.animateTo(1f, animationSpec = tween(700))
        delay(2000)
        alpha.animateTo(0f, animationSpec = tween(600))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .alpha(alpha.value)
                .padding(horizontal = 40.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_agroscan),
                contentDescription = "AgroScan AI Logo",
                modifier = Modifier.size(110.dp)
            )

            Text(
                text = "¡Bienvenido a AgroScan AI!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (nombreUsuario.isNotBlank()) "Hola, $nombreUsuario" else "Hola",
                fontSize = 18.sp,
                color = VerdeEsmeralda,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alphaSubtitle.value)
            )

            Text(
                text = "Tu plataforma inteligente para\nmonitorear y proteger tus cultivos",
                fontSize = 14.sp,
                color = GrisMedio,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.alpha(alphaSubtitle.value)
            )
        }

        Text(
            text = "AgroScan AI es una marca registrada. Todos los derechos reservados.",
            fontSize = 11.sp,
            color = GrisMedio.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 20.dp)
                .alpha(alpha.value)
        )
    }
}
