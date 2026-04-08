package com.example.agroscanai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.viewmodel.ResultadoEscaneo
import com.google.firebase.auth.ktx.auth
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

class CultivosViewModel : ViewModel() {

    private val db   = Firebase.firestore
    private val auth = Firebase.auth

    private val _cultivos   = MutableStateFlow<List<Cultivo>>(emptyList())
    val cultivos: StateFlow<List<Cultivo>> = _cultivos.asStateFlow()

    private val _isLoading  = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error      = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private fun userId() = auth.currentUser?.uid ?: ""

    private fun cultivosRef() =
        db.collection("users").document(userId()).collection("cultivos")

    init { cargarCultivos() }

    fun cargarCultivos() {
        val uid = userId()
        if (uid.isEmpty()) return
        _isLoading.value = true
        cultivosRef().addSnapshotListener { snapshot, e ->
            _isLoading.value = false
            if (e != null) { _error.value = "Error al cargar cultivos"; return@addSnapshotListener }
            _cultivos.value = snapshot?.documents?.map { doc ->
                Cultivo(
                    id             = doc.id,
                    nombre         = doc.getString("nombre")         ?: "",
                    tipoCultivo    = doc.getString("tipoCultivo")    ?: "",
                    variedadSemilla= doc.getString("variedadSemilla")?: "",
                    hectareas      = doc.getDouble("hectareas")      ?: 0.0,
                    fechaSiembra   = doc.getString("fechaSiembra")   ?: "",
                    ubicacion      = doc.getString("ubicacion")      ?: "",
                    estado         = doc.getString("estado")         ?: EstadoCultivo.SIN_ESCANEO.name,
                    ultimoEscaneo  = doc.getString("ultimoEscaneo")  ?: ""
                )
            } ?: emptyList()
        }
    }

    fun agregarCultivo(
        nombre: String,
        tipoCultivo: String,
        variedadSemilla: String,
        fechaSiembra: String,
        hectareas: Double,
        ubicacion: String
    ) {
        val uid = userId()
        if (uid.isEmpty()) return
        viewModelScope.launch {
            try {
                cultivosRef().add(
                    hashMapOf(
                        "nombre"          to nombre,
                        "tipoCultivo"     to tipoCultivo,
                        "variedadSemilla" to variedadSemilla,
                        "fechaSiembra"    to fechaSiembra,
                        "hectareas"       to hectareas,
                        "ubicacion"       to ubicacion,
                        "estado"          to EstadoCultivo.SIN_ESCANEO.name,
                        "ultimoEscaneo"   to "Sin escaneos aún"
                    )
                ).await()
            } catch (e: Exception) {
                _error.value = "No se pudo guardar el cultivo. Intenta de nuevo."
            }
        }
    }

    fun eliminarCultivo(cultivoId: String) {
        viewModelScope.launch {
            try {
                cultivosRef().document(cultivoId).delete().await()
            } catch (e: Exception) {
                _error.value = "No se pudo eliminar el cultivo."
            }
        }
    }

    fun guardarResultadoEscaneo(cultivoId: String, resultado: ResultadoEscaneo) {
        val uid = userId()
        if (uid.isEmpty() || cultivoId.isEmpty()) return
        viewModelScope.launch {
            try {
                val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

                val nuevoEstado = when {
                    resultado.plagasDetectadas || resultado.humedadSuelo < 15f ->
                        EstadoCultivo.CRITICO.name
                    resultado.humedadSuelo < 20f || resultado.nivelNitrogenio < 40f ->
                        EstadoCultivo.ALERTA.name
                    else -> EstadoCultivo.SALUDABLE.name
                }

                cultivosRef().document(cultivoId).collection("escaneos").add(
                    hashMapOf(
                        "fecha"                   to fecha,
                        "confianzaIA"             to resultado.confianzaIA,
                        "gpsLat"                  to resultado.gpsLat,
                        "gpsLon"                  to resultado.gpsLon,
                        "humedadSuelo"            to resultado.humedadSuelo,
                        "estresSuelo"             to resultado.estresSuelo,
                        "descripcionSuelo"        to resultado.descripcionSuelo,
                        "plagasDetectadas"        to resultado.plagasDetectadas,
                        "descripcionPlagas"       to resultado.descripcionPlagas,
                        "nivelNitrogenio"         to resultado.nivelNitrogenio,
                        "nivelFosforo"            to resultado.nivelFosforo,
                        "nivelPotasio"            to resultado.nivelPotasio,
                        "descripcionNutrientes"   to resultado.descripcionNutrientes,
                        "recomendacionNutrientes" to resultado.recomendacionNutrientes
                    )
                ).await()

                cultivosRef().document(cultivoId).update(
                    mapOf(
                        "estado"         to nuevoEstado,
                        "ultimoEscaneo"  to fecha,
                        "humedadPromedio" to resultado.humedadSuelo,
                        "nitrogenio"     to resultado.nivelNitrogenio,
                        "fosforo"        to resultado.nivelFosforo,
                        "potasio"        to resultado.nivelPotasio
                    )
                ).await()

            } catch (e: Exception) {
                _error.value = "No se pudo guardar el escaneo."
            }
        }
    }

    fun clearError() { _error.value = null }
}
