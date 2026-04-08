package com.example.agroscanai.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AgroScanColorScheme = lightColorScheme(
    primary = VerdeBosque,
    onPrimary = BlancoNieve,
    primaryContainer = VerdeLima,
    onPrimaryContainer = VerdeBosque,
    secondary = VerdeEsmeralda,
    onSecondary = BlancoNieve,
    secondaryContainer = VerdeLima,
    onSecondaryContainer = VerdeBosqueOscuro,
    background = BlancoNieve,
    onBackground = GrisHumo,
    surface = BlancoNieve,
    onSurface = GrisHumo,
    error = RojoAlerta,
    onError = BlancoNieve
)

@Composable
fun AgroScanAITheme(
    content: @Composable () -> Unit
) {
    val colorScheme = AgroScanColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BlancoNieve.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}