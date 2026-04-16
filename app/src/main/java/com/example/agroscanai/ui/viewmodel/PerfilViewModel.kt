package com.example.agroscanai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agroscanai.data.model.TipoCuenta
import com.example.agroscanai.data.model.Usuario
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PerfilViewModel : ViewModel() {

    private val db   = Firebase.firestore
    private val auth = Firebase.auth

    private val _usuario       = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private val _isLoading     = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error         = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _mensajeExito  = MutableStateFlow<String?>(null)
    val mensajeExito: StateFlow<String?> = _mensajeExito.asStateFlow()

    // Modo oscuro expuesto directo para MainActivity
    val modoOscuro: StateFlow<Boolean> get() = _modoOscuro
    private val _modoOscuro = MutableStateFlow(false)

    val emailActual: String  get() = auth.currentUser?.email ?: ""
    val uidActual: String    get() = auth.currentUser?.uid   ?: ""

    private fun userDoc() = db.collection("users").document(uidActual)

    init { cargarPerfil() }

    fun cargarPerfil() {
        val uid = uidActual
        if (uid.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val doc = userDoc().get().await()
                if (doc.exists()) {
                    val u = Usuario(
                        id                 = uid,
                        nombre             = doc.getString("nombre")       ?: inferirNombre(),
                        apellido           = doc.getString("apellido")     ?: "",
                        email              = emailActual,
                        telefono           = doc.getString("telefono")     ?: "",
                        region             = doc.getString("region")       ?: "",
                        tipoCuenta         = doc.getString("tipoCuenta")   ?: TipoCuenta.BASICO.name,
                        fechaRegistro      = doc.getString("fechaRegistro") ?: "",
                        notificacionesPush = doc.getBoolean("notificacionesPush") ?: true,
                        sonidosAlerta      = doc.getBoolean("sonidosAlerta")      ?: true,
                        unidadesMetricas   = doc.getBoolean("unidadesMetricas")   ?: true,
                        modoOscuro         = doc.getBoolean("modoOscuro")         ?: false
                    )
                    _usuario.value = u
                    _modoOscuro.value = u.modoOscuro
                } else {
                    val hoy   = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    val nuevo = Usuario(
                        id            = uid,
                        nombre        = inferirNombre(),
                        email         = emailActual,
                        tipoCuenta    = TipoCuenta.BASICO.name,
                        fechaRegistro = hoy
                    )
                    userDoc().set(
                        hashMapOf(
                            "nombre"             to nuevo.nombre,
                            "apellido"           to nuevo.apellido,
                            "email"              to nuevo.email,
                            "telefono"           to nuevo.telefono,
                            "region"             to nuevo.region,
                            "tipoCuenta"         to nuevo.tipoCuenta,
                            "fechaRegistro"      to nuevo.fechaRegistro,
                            "notificacionesPush" to true,
                            "sonidosAlerta"      to true,
                            "unidadesMetricas"   to true,
                            "modoOscuro"         to false
                        )
                    ).await()
                    _usuario.value = nuevo
                }
            } catch (e: Exception) {
                _error.value = "No se pudo cargar el perfil."
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Guarda todos los datos del perfil. Usa set+merge para funcionar aunque
     *  el documento no exista aún. Actualiza email en Auth sólo si cambió. */
    fun actualizarTodoPerfil(
        nombre: String,
        apellido: String,
        telefono: String,
        region: String,
        tipoCuenta: String,
        nuevoEmail: String
    ) {
        val uid = uidActual
        if (uid.isEmpty()) {
            _error.value = "No hay sesión activa. Por favor inicia sesión de nuevo."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val emailFinal = nuevoEmail.trim().ifBlank { emailActual }

                // 1. set+merge: funciona tanto si el documento existe como si no
                val datos = mapOf(
                    "nombre"     to nombre.trim(),
                    "apellido"   to apellido.trim(),
                    "telefono"   to telefono.trim(),
                    "region"     to region.trim(),
                    "tipoCuenta" to tipoCuenta,
                    "email"      to emailFinal
                )
                userDoc().set(datos, SetOptions.merge()).await()

                // 2. Actualizar email en Firebase Auth sólo si cambió
                val emailAuth = auth.currentUser?.email ?: ""
                if (emailFinal != emailAuth && emailFinal.isNotBlank()) {
                    try {
                        auth.currentUser?.updateEmail(emailFinal)?.await()
                    } catch (emailEx: Exception) {
                        val msg = emailEx.message ?: ""
                        _error.value = when {
                            msg.contains("CREDENTIAL_TOO_OLD", true) ||
                            msg.contains("requires-recent-login", true) ->
                                "Para cambiar el correo cierra sesión, vuelve a entrar y repite el cambio."
                            msg.contains("EMAIL_EXISTS", true) ||
                            msg.contains("email-already-in-use", true) ->
                                "Ese correo ya está en uso por otra cuenta."
                            msg.contains("INVALID_EMAIL", true) ||
                            msg.contains("invalid-email", true) ->
                                "El formato del correo no es válido."
                            else -> "No se pudo cambiar el correo: $msg"
                        }
                        // Los demás campos se guardaron correctamente, continuamos
                    }
                }

                // 3. Actualizar estado local
                _usuario.value = _usuario.value?.copy(
                    nombre     = nombre.trim(),
                    apellido   = apellido.trim(),
                    telefono   = telefono.trim(),
                    region     = region.trim(),
                    tipoCuenta = tipoCuenta,
                    email      = auth.currentUser?.email ?: emailFinal
                )

                if (_error.value == null) {
                    _mensajeExito.value = "Perfil actualizado correctamente"
                }
            } catch (e: Exception) {
                _error.value = "No se pudo guardar el perfil: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun enviarResetContrasena() {
        val email = auth.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _mensajeExito.value = "Se envió un enlace de restablecimiento a $email"
            } catch (e: Exception) {
                _error.value = "No se pudo enviar el correo de restablecimiento."
            }
        }
    }

    fun actualizarConfiguracion(
        notificacionesPush: Boolean,
        sonidosAlerta: Boolean,
        unidadesMetricas: Boolean,
        modoOscuro: Boolean
    ) {
        if (uidActual.isEmpty()) return
        viewModelScope.launch {
            try {
                userDoc().update(
                    mapOf(
                        "notificacionesPush" to notificacionesPush,
                        "sonidosAlerta"      to sonidosAlerta,
                        "unidadesMetricas"   to unidadesMetricas,
                        "modoOscuro"         to modoOscuro
                    )
                ).await()
                _usuario.value = _usuario.value?.copy(
                    notificacionesPush = notificacionesPush,
                    sonidosAlerta      = sonidosAlerta,
                    unidadesMetricas   = unidadesMetricas,
                    modoOscuro         = modoOscuro
                )
                _modoOscuro.value = modoOscuro
            } catch (_: Exception) { }
        }
    }

    fun signOut() { auth.signOut() }

    fun clearMensajeExito() { _mensajeExito.value = null }
    fun clearError()        { _error.value = null }

    private fun inferirNombre(): String {
        val display = auth.currentUser?.displayName
        if (!display.isNullOrBlank()) return display.split(" ").firstOrNull() ?: display
        return emailActual.substringBefore("@").replaceFirstChar { it.uppercase() }
    }
}
