package com.example.agroscanai.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AgroScanLightColorScheme = lightColorScheme(
    primary             = VerdeBosque,
    onPrimary           = BlancoNieve,
    primaryContainer    = VerdeLima,
    onPrimaryContainer  = VerdeBosque,
    secondary           = VerdeEsmeralda,
    onSecondary         = BlancoNieve,
    secondaryContainer  = VerdeLima,
    onSecondaryContainer = VerdeBosqueOscuro,
    background          = BlancoNieve,
    onBackground        = GrisHumo,
    surface             = BlancoNieve,
    onSurface           = GrisHumo,
    surfaceVariant      = Color(0xFFF0F4F0),
    onSurfaceVariant    = GrisHumo,
    error               = RojoAlerta,
    onError             = BlancoNieve
)

private val AgroScanDarkColorScheme = darkColorScheme(
    primary             = VerdePrimarioDark,
    onPrimary           = Color(0xFF1A3A18),
    primaryContainer    = Color(0xFF1B5E20),
    onPrimaryContainer  = VerdeLima,
    secondary           = VerdeSecundarioDark,
    onSecondary         = Color(0xFF1A3A18),
    secondaryContainer  = Color(0xFF1B5E20),
    onSecondaryContainer = VerdeLima,
    background          = FondoOscuro,
    onBackground        = TextoPrincipalOscuro,
    surface             = SuperficieOscura,
    onSurface           = TextoPrincipalOscuro,
    surfaceVariant      = SuperficieOscura2,
    onSurfaceVariant    = TextoSecOscuro,
    error               = Color(0xFFCF6679),
    onError             = Color(0xFF370B1E)
)

@Composable
fun AgroScanAITheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AgroScanDarkColorScheme else AgroScanLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) FondoOscuro.toArgb() else BlancoNieve.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
