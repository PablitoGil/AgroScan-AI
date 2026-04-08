package com.example.agroscanai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.agroscanai.ui.screens.BienvenidaScreen
import com.example.agroscanai.ui.screens.EscanearParcelaScreen
import com.example.agroscanai.ui.screens.HomeScreen
import com.example.agroscanai.ui.screens.LoginScreen
import com.example.agroscanai.ui.screens.MisCultivosScreen
import com.example.agroscanai.ui.screens.OnboardingScreen
import com.example.agroscanai.ui.screens.RecuperarContrasenaScreen
import com.example.agroscanai.ui.screens.RegisterScreen
import com.example.agroscanai.ui.screens.SplashScreen
import com.example.agroscanai.ui.screens.VerificacionEmailScreen
import com.example.agroscanai.ui.screens.WelcomeScreen
import com.example.agroscanai.ui.viewmodel.AuthViewModel
import com.example.agroscanai.ui.viewmodel.DroneViewModel

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val RECUPERAR_CONTRASENA = "recuperar_contrasena"
    const val REGISTER = "register"
    const val VERIFICACION_EMAIL = "verificacion_email/{email}"
    const val BIENVENIDA = "bienvenida/{nombre}"
    const val HOME = "home"
    const val MIS_CULTIVOS = "mis_cultivos"
    const val MAPA_LOTES = "mapa_lotes"
    const val DASHBOARD_SALUD = "dashboard_salud"
    const val REPORTES_IA = "reportes_ia"
    const val CLIMA = "clima"
    const val CALENDARIO = "calendario"
    const val PRECIOS = "precios"
    const val PERFIL = "perfil"
    const val NOTIFICACIONES = "notificaciones"
    const val ESCANEO = "escaneo"
    const val DETALLE_CULTIVO = "detalle_cultivo/{cultivoId}"
    const val DETALLE_ESCANEO = "detalle_escaneo/{escaneoId}"

    fun verificacionEmail(email: String) = "verificacion_email/$email"
    fun bienvenida(nombre: String) = "bienvenida/${nombre.ifBlank { "Usuario" }}"
    fun detallesCultivo(cultivoId: Int) = "detalle_cultivo/$cultivoId"
    fun detallesEscaneo(escaneoId: Int) = "detalle_escaneo/$escaneoId"
}

@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()
    val droneViewModel: DroneViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(Routes.LOGIN) },
                onRegisterClick = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onLoginSuccess = { nombre ->
                    navController.navigate(Routes.bienvenida(nombre)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onForgotPassword = { navController.navigate(Routes.RECUPERAR_CONTRASENA) },
                onRegisterClick = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Routes.RECUPERAR_CONTRASENA) {
            RecuperarContrasenaScreen(
                onBackClick = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onVerificationSent = { email ->
                    navController.navigate(Routes.verificacionEmail(email))
                },
                onRegisterSuccess = { nombre ->
                    navController.navigate(Routes.bienvenida(nombre)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Routes.VERIFICACION_EMAIL) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            VerificacionEmailScreen(
                email = email,
                onBackClick = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onVerified = { nombre ->
                    navController.navigate(Routes.bienvenida(nombre)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Routes.BIENVENIDA) { backStackEntry ->
            val nombre = backStackEntry.arguments?.getString("nombre") ?: ""
            BienvenidaScreen(
                nombreUsuario = nombre,
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.BIENVENIDA) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) { backStackEntry ->
            val nombre = navController.previousBackStackEntry
                ?.arguments?.getString("nombre") ?: ""
            HomeScreen(
                nombreUsuario = nombre,
                onMenuItemClick = { route -> navController.navigate(route) },
                onNotificacionesClick = { navController.navigate(Routes.NOTIFICACIONES) },
                onPerfilClick = { navController.navigate(Routes.PERFIL) }
            )
        }

        composable(Routes.MIS_CULTIVOS) {
            MisCultivosScreen(
                onHomeClick = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { navController.navigate(Routes.NOTIFICACIONES) },
                onPerfilClick = { navController.navigate(Routes.PERFIL) }
            )
        }

        composable(Routes.ESCANEO) {
            EscanearParcelaScreen(
                onHomeClick = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { navController.navigate(Routes.NOTIFICACIONES) },
                onPerfilClick = { navController.navigate(Routes.PERFIL) },
                droneViewModel = droneViewModel
            )
        }
    }
}
