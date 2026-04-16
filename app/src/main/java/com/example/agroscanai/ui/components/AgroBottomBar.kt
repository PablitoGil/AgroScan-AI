package com.example.agroscanai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agroscanai.ui.theme.GrisMedio
import com.example.agroscanai.ui.theme.VerdeBosque

enum class PaginaActual { HOME, NOTIFICACIONES, PERFIL, NINGUNA }

@Composable
fun AgroBottomBar(
    onHomeClick: () -> Unit,
    onNotificacionesClick: () -> Unit,
    onPerfilClick: () -> Unit,
    paginaActual: PaginaActual = PaginaActual.NINGUNA
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor   = VerdeBosque,
        selectedTextColor   = VerdeBosque,
        indicatorColor      = VerdeBosque.copy(alpha = 0.12f),
        unselectedIconColor = GrisMedio,
        unselectedTextColor = GrisMedio
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4f.dp
    ) {
        NavigationBarItem(
            selected = paginaActual == PaginaActual.HOME,
            onClick  = onHomeClick,
            icon     = { Icon(Icons.Filled.Home, contentDescription = "Inicio") },
            label    = { Text("Inicio",  fontSize = 11.sp) },
            colors   = itemColors
        )
        NavigationBarItem(
            selected = paginaActual == PaginaActual.NOTIFICACIONES,
            onClick  = onNotificacionesClick,
            icon     = { Icon(Icons.Filled.Notifications, contentDescription = "Alertas") },
            label    = { Text("Alertas", fontSize = 11.sp) },
            colors   = itemColors
        )
        NavigationBarItem(
            selected = paginaActual == PaginaActual.PERFIL,
            onClick  = onPerfilClick,
            icon     = { Icon(Icons.Filled.Person, contentDescription = "Perfil") },
            label    = { Text("Perfil",  fontSize = 11.sp) },
            colors   = itemColors
        )
    }
}
