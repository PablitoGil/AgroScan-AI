package com.example.agroscanai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agroscanai.data.model.PrioridadTarea
import com.example.agroscanai.data.model.Tarea
import com.example.agroscanai.data.model.TipoTarea
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CalendarioViewModel : ViewModel() {

    private val db   = Firebase.firestore
    private val auth = Firebase.auth

    private val _tareas      = MutableStateFlow<List<Tarea>>(emptyList())
    val tareas: StateFlow<List<Tarea>> = _tareas.asStateFlow()

    private val _isLoading   = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error       = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _mensajeExito = MutableStateFlow<String?>(null)
    val mensajeExito: StateFlow<String?> = _mensajeExito.asStateFlow()

    private var snapshotListener: ListenerRegistration? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null) {
            if (snapshotListener == null) cargarTareas()
        } else {
            snapshotListener?.remove()
            snapshotListener = null
            _tareas.value = emptyList()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    private fun userId() = auth.currentUser?.uid ?: ""

    private fun tareasRef() =
        db.collection("users").document(userId()).collection("tareas")

    fun cargarTareas() {
        val uid = userId()
        if (uid.isEmpty()) return
        snapshotListener?.remove()
        _isLoading.value = true
        snapshotListener = tareasRef().addSnapshotListener { snapshot, e ->
            _isLoading.value = false
            if (e != null) {
                _error.value = "Error al cargar tareas"
                return@addSnapshotListener
            }
            _tareas.value = snapshot?.documents?.map { doc ->
                Tarea(
                    id             = doc.id,
                    cultivoId      = doc.getString("cultivoId")      ?: "",
                    cultivoNombre  = doc.getString("cultivoNombre")  ?: "",
                    titulo         = doc.getString("titulo")         ?: "",
                    descripcion    = doc.getString("descripcion")    ?: "",
                    fecha          = doc.getString("fecha")          ?: "",
                    hora           = doc.getString("hora")           ?: "",
                    tipo           = doc.getString("tipo")           ?: TipoTarea.MANUAL.name,
                    completada     = doc.getBoolean("completada")    ?: false,
                    prioridad      = doc.getString("prioridad")      ?: PrioridadTarea.MEDIA.name
                )
            } ?: emptyList()
        }
    }

    fun agregarTarea(
        titulo: String,
        descripcion: String,
        fecha: String,
        hora: String,
        tipo: TipoTarea,
        prioridad: PrioridadTarea,
        cultivoId: String = "",
        cultivoNombre: String = ""
    ) {
        val uid = userId()
        if (uid.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                tareasRef().add(
                    hashMapOf(
                        "titulo"        to titulo.trim(),
                        "descripcion"   to descripcion.trim(),
                        "fecha"         to fecha,
                        "hora"          to hora,
                        "tipo"          to tipo.name,
                        "prioridad"     to prioridad.name,
                        "completada"    to false,
                        "cultivoId"     to cultivoId,
                        "cultivoNombre" to cultivoNombre
                    )
                ).await()
                _mensajeExito.value = "Tarea agregada correctamente"
            } catch (e: Exception) {
                _error.value = "No se pudo guardar la tarea"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleCompletada(tareaId: String, actual: Boolean) {
        val uid = userId()
        if (uid.isEmpty() || tareaId.isEmpty()) return
        viewModelScope.launch {
            try {
                tareasRef().document(tareaId)
                    .set(mapOf("completada" to !actual), SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                _error.value = "No se pudo actualizar la tarea"
            }
        }
    }

    fun eliminarTarea(tareaId: String) {
        val uid = userId()
        if (uid.isEmpty() || tareaId.isEmpty()) return
        viewModelScope.launch {
            try {
                tareasRef().document(tareaId).delete().await()
            } catch (e: Exception) {
                _error.value = "No se pudo eliminar la tarea"
            }
        }
    }

    fun clearError()        { _error.value = null }
    fun clearMensajeExito() { _mensajeExito.value = null }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
        auth.removeAuthStateListener(authStateListener)
    }
}
