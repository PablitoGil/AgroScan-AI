package com.example.agroscanai.ui.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class VerificationEmailSent(val email: String, val displayName: String?) : AuthState()
    data class Success(val uid: String, val displayName: String?) : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun registerWithEmail(
        email: String,
        password: String,
        nombres: String,
        apellidos: String
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val displayName = "$nombres $apellidos".trim()
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                result.user?.updateProfile(profileUpdates)?.await()
                result.user?.sendEmailVerification()?.await()
                _authState.value = AuthState.VerificationEmailSent(email, displayName)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun checkEmailVerified() {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.currentUser?.reload()?.await()
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    _authState.value = AuthState.Success(
                        uid = user.uid,
                        displayName = user.displayName
                    )
                } else {
                    _authState.value = AuthState.Error(
                        "Tu correo aún no ha sido verificado. Revisa tu bandeja de entrada y haz clic en el enlace."
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                auth.currentUser?.sendEmailVerification()?.await()
                _authState.value = AuthState.Error("✓ Correo reenviado. Revisa tu bandeja.")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("No se pudo reenviar el correo. Intenta de nuevo.")
            }
        }
    }

    fun signOutUnverified() {
        auth.signOut()
    }

    fun loginWithEmail(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user?.isEmailVerified == true) {
                    _authState.value = AuthState.Success(
                        uid = user.uid,
                        displayName = user.displayName
                    )
                } else {
                    auth.signOut()
                    _authState.value = AuthState.Error(
                        "Debes verificar tu correo antes de iniciar sesión. Revisa tu bandeja de entrada."
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.PasswordResetSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(request = request, context = context)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential =
                        GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    _authState.value = AuthState.Success(
                        uid = authResult.user?.uid ?: "",
                        displayName = authResult.user?.displayName
                    )
                } else {
                    _authState.value = AuthState.Error("Tipo de credencial no reconocido")
                }
            } catch (e: GetCredentialException) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    private fun friendlyError(raw: String?): String = when {
        raw == null -> "Ocurrió un error inesperado"
        raw.contains("email address is already in use", ignoreCase = true) ->
            "Este correo ya está registrado"
        raw.contains("badly formatted", ignoreCase = true) ->
            "El correo no tiene un formato válido"
        raw.contains("password is invalid", ignoreCase = true) ||
                raw.contains("least 6 characters", ignoreCase = true) ->
            "La contraseña debe tener al menos 6 caracteres"
        raw.contains("network", ignoreCase = true) ->
            "Sin conexión a internet"
        raw.contains("canceled", ignoreCase = true) ||
                raw.contains("cancelled", ignoreCase = true) ->
            "Inicio de sesión cancelado"
        raw.contains("No credentials available", ignoreCase = true) ||
                raw.contains("no credentials", ignoreCase = true) ->
            "No se encontró una cuenta Google en el dispositivo. Agrega una cuenta en Ajustes."
        raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
            "Servicio no configurado. Contacta al soporte."
        raw.contains("interrupted", ignoreCase = true) ->
            "Inicio de sesión interrumpido. Intenta de nuevo."
        raw.contains("no user record", ignoreCase = true) ||
                raw.contains("user not found", ignoreCase = true) ->
            "No existe una cuenta con este correo"
        raw.contains("password is invalid", ignoreCase = true) ||
                raw.contains("wrong password", ignoreCase = true) ||
                raw.contains("invalid credential", ignoreCase = true) ->
            "Correo o contraseña incorrectos"
        raw.contains("too many requests", ignoreCase = true) ->
            "Demasiados intentos fallidos. Intenta más tarde."
        else -> raw
    }

    companion object {
        const val WEB_CLIENT_ID =
            "323374255459-fq9bagbgbrh1ncg0t1q6n81t00eddabl.apps.googleusercontent.com"
    }
}
