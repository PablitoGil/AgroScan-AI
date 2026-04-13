package com.example.agroscanai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.viewmodel.ResultadoEscaneo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
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

data class UltimoEscaneoData(
    val fecha: String = "",
    val confianzaIA: Int = 0,
    val gpsLat: Double = 0.0,
    val gpsLon: Double = 0.0,
    val humedadSuelo: Float = 0f,
    val estresSuelo: String = "",
    val descripcionSuelo: String = "",
    val plagasDetectadas: Boolean = false,
    val descripcionPlagas: String = "",
    val nivelNitrogenio: Float = 0f,
    val nivelFosforo: Float = 0f,
    val nivelPotasio: Float = 0f,
    val descripcionNutrientes: String = "",
    val recomendacionNutrientes: String = ""
)

class CultivosViewModel : ViewModel() {

    private val db   = Firebase.firestore
    private val auth = Firebase.auth

    private val _cultivos   = MutableStateFlow<List<Cultivo>>(emptyList())
    val cultivos: StateFlow<List<Cultivo>> = _cultivos.asStateFlow()

    private val _isLoading  = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error      = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _ultimoEscaneo = MutableStateFlow<UltimoEscaneoData?>(null)
    val ultimoEscaneo: StateFlow<UltimoEscaneoData?> = _ultimoEscaneo.asStateFlow()

    private val _cargandoEscaneo = MutableStateFlow(false)
    val cargandoEscaneo: StateFlow<Boolean> = _cargandoEscaneo.asStateFlow()

    private var snapshotListener: ListenerRegistration? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null) {
            if (snapshotListener == null) cargarCultivos()
        } else {
            snapshotListener?.remove()
            snapshotListener = null
            _cultivos.value = emptyList()
        }
    }

    private fun userId() = auth.currentUser?.uid ?: ""

    private fun cultivosRef() =
        db.collection("users").document(userId()).collection("cultivos")

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun cargarCultivos() {
        val uid = userId()
        if (uid.isEmpty()) return
        snapshotListener?.remove()
        _isLoading.value = true
        snapshotListener = cultivosRef().addSnapshotListener { snapshot, e ->
            _isLoading.value = false
            if (e != null) { _error.value = "Error al cargar cultivos"; return@addSnapshotListener }
            _cultivos.value = snapshot?.documents?.map { doc ->
                Cultivo(
                    id               = doc.id,
                    nombre           = doc.getString("nombre")          ?: "",
                    tipoCultivo      = doc.getString("tipoCultivo")     ?: "",
                    variedadSemilla  = doc.getString("variedadSemilla") ?: "",
                    hectareas        = doc.getDouble("hectareas")       ?: 0.0,
                    fechaSiembra     = doc.getString("fechaSiembra")    ?: "",
                    ubicacion        = doc.getString("ubicacion")       ?: "",
                    estado           = doc.getString("estado")          ?: EstadoCultivo.SIN_ESCANEO.name,
                    humedadPromedio  = (doc.getDouble("humedadPromedio") ?: 0.0).toFloat(),
                    nitrogenio       = (doc.getDouble("nitrogenio")      ?: 0.0).toFloat(),
                    fosforo          = (doc.getDouble("fosforo")         ?: 0.0).toFloat(),
                    potasio          = (doc.getDouble("potasio")         ?: 0.0).toFloat(),
                    indiceSalud      = (doc.getDouble("indiceSalud")     ?: 0.0).toFloat(),
                    plagasDetectadas = doc.getBoolean("plagasDetectadas") ?: false,
                    ultimoEscaneo    = doc.getString("ultimoEscaneo")    ?: ""
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

                // Actualización inmediata en memoria para reflejar el estado al instante en la UI
                _cultivos.value = _cultivos.value.map { c ->
                    if (c.id == cultivoId) c.copy(
                        estado          = nuevoEstado,
                        ultimoEscaneo   = fecha,
                        humedadPromedio = resultado.humedadSuelo,
                        nitrogenio      = resultado.nivelNitrogenio,
                        fosforo         = resultado.nivelFosforo,
                        potasio         = resultado.nivelPotasio
                    ) else c
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

                val indiceSaludCalc = run {
                    var score = 100f
                    if (resultado.plagasDetectadas) score -= 40f
                    score -= when {
                        resultado.humedadSuelo < 10f -> 35f
                        resultado.humedadSuelo < 15f -> 25f
                        resultado.humedadSuelo < 20f -> 15f
                        resultado.humedadSuelo < 25f -> 5f
                        resultado.humedadSuelo > 70f -> 10f
                        else -> 0f
                    }
                    score -= when {
                        resultado.nivelNitrogenio < 30f -> 20f
                        resultado.nivelNitrogenio < 45f -> 12f
                        resultado.nivelNitrogenio < 60f -> 5f
                        else -> 0f
                    }
                    score -= when {
                        resultado.nivelFosforo < 40f -> 15f
                        resultado.nivelFosforo < 50f -> 8f
                        resultado.nivelFosforo < 60f -> 3f
                        else -> 0f
                    }
                    score -= when {
                        resultado.nivelPotasio < 50f -> 10f
                        resultado.nivelPotasio < 60f -> 5f
                        else -> 0f
                    }
                    score.coerceIn(5f, 100f)
                }

                cultivosRef().document(cultivoId).update(
                    mapOf(
                        "estado"            to nuevoEstado,
                        "ultimoEscaneo"     to fecha,
                        "humedadPromedio"   to resultado.humedadSuelo,
                        "nitrogenio"        to resultado.nivelNitrogenio,
                        "fosforo"           to resultado.nivelFosforo,
                        "potasio"           to resultado.nivelPotasio,
                        "plagasDetectadas"  to resultado.plagasDetectadas,
                        "indiceSalud"       to indiceSaludCalc
                    )
                ).await()

            } catch (e: Exception) {
                _error.value = "No se pudo guardar el escaneo."
            }
        }
    }

    fun cargarUltimoEscaneo(cultivoId: String) {
        val uid = userId()
        if (uid.isEmpty() || cultivoId.isEmpty()) return
        viewModelScope.launch {
            _cargandoEscaneo.value = true
            _ultimoEscaneo.value = null
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val snapshot = cultivosRef()
                    .document(cultivoId)
                    .collection("escaneos")
                    .get()
                    .await()
                val latestDoc = snapshot.documents
                    .sortedByDescending { doc ->
                        try { sdf.parse(doc.getString("fecha") ?: "") } catch (e: Exception) { null }
                    }
                    .firstOrNull()
                if (latestDoc != null) {
                    _ultimoEscaneo.value = UltimoEscaneoData(
                        fecha                   = latestDoc.getString("fecha")                    ?: "",
                        confianzaIA             = (latestDoc.getLong("confianzaIA")               ?: 0L).toInt(),
                        gpsLat                  = latestDoc.getDouble("gpsLat")                   ?: 0.0,
                        gpsLon                  = latestDoc.getDouble("gpsLon")                   ?: 0.0,
                        humedadSuelo            = (latestDoc.getDouble("humedadSuelo")            ?: 0.0).toFloat(),
                        estresSuelo             = latestDoc.getString("estresSuelo")              ?: "",
                        descripcionSuelo        = latestDoc.getString("descripcionSuelo")         ?: "",
                        plagasDetectadas        = latestDoc.getBoolean("plagasDetectadas")        ?: false,
                        descripcionPlagas       = latestDoc.getString("descripcionPlagas")        ?: "",
                        nivelNitrogenio         = (latestDoc.getDouble("nivelNitrogenio")         ?: 0.0).toFloat(),
                        nivelFosforo            = (latestDoc.getDouble("nivelFosforo")            ?: 0.0).toFloat(),
                        nivelPotasio            = (latestDoc.getDouble("nivelPotasio")            ?: 0.0).toFloat(),
                        descripcionNutrientes   = latestDoc.getString("descripcionNutrientes")    ?: "",
                        recomendacionNutrientes = latestDoc.getString("recomendacionNutrientes")  ?: ""
                    )
                }
            } catch (e: Exception) {
                _ultimoEscaneo.value = null
            } finally {
                _cargandoEscaneo.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
        auth.removeAuthStateListener(authStateListener)
    }
}
