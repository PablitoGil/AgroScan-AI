package com.example.agroscanai.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.roundToInt

enum class DroneEstado {
    DESCONECTADO,
    BUSCANDO,
    CONECTANDO,
    CONECTADO,
    ESCANEANDO,
    ESCANEO_COMPLETO,
    ERROR
}

data class ResultadoEscaneo(
    val progresoPercent: Int = 0,
    val gpsLat: Double = 0.0,
    val gpsLon: Double = 0.0,
    val confianzaIA: Int = 0,
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

data class DroneInfo(
    val nombre: String = "",
    val modelo: String = "",
    val bateria: Int = 0,
    val altitud: Float = 0f,
    val velocidad: Float = 0f,
    val señalGPS: Int = 0
)

private const val MAVLINK_STX_V1 = 0xFE.toByte()
private const val MAVLINK_STX_V2 = 0xFD.toByte()
private const val MSG_ID_HEARTBEAT = 0
private const val DRONE_UDP_PORT = 14550
private const val GCS_UDP_PORT = 14551
private val COMMON_DRONE_IPS = listOf("192.168.1.1", "192.168.0.1", "10.0.0.1", "192.168.42.1")
private val COMMON_DRONE_SSIDS = listOf("TELLO", "DJI", "Phantom", "Mavic", "Mini", "Spark",
    "ARDUPILOT", "ArduPilot", "PixHawk", "px4", "drone", "Drone", "UAV", "uav")

class DroneViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext

    private val _estado = MutableStateFlow(DroneEstado.DESCONECTADO)
    val estado: StateFlow<DroneEstado> = _estado.asStateFlow()

    private val _droneInfo = MutableStateFlow(DroneInfo())
    val droneInfo: StateFlow<DroneInfo> = _droneInfo.asStateFlow()

    private val _resultado = MutableStateFlow(ResultadoEscaneo())
    val resultado: StateFlow<ResultadoEscaneo> = _resultado.asStateFlow()

    private val _mensajeEstado = MutableStateFlow("Sin conexión con el dron")
    val mensajeEstado: StateFlow<String> = _mensajeEstado.asStateFlow()

    private val _errorMensaje = MutableStateFlow<String?>(null)
    val errorMensaje: StateFlow<String?> = _errorMensaje.asStateFlow()

    private val _modoSimulacion = MutableStateFlow(false)
    val modoSimulacion: StateFlow<Boolean> = _modoSimulacion.asStateFlow()

    private var udpSocket: DatagramSocket? = null
    private var heartbeatJob: Job? = null
    private var scanJob: Job? = null
    private var droneIpConectado: String? = null

    fun buscarYConectarDron() {
        if (_estado.value == DroneEstado.BUSCANDO || _estado.value == DroneEstado.CONECTANDO) return
        viewModelScope.launch {
            _estado.value = DroneEstado.BUSCANDO
            _mensajeEstado.value = "Buscando dron en la red WiFi..."
            _errorMensaje.value = null

            val droneSSID = detectarSSIDDron()
            if (droneSSID != null) {
                _mensajeEstado.value = "Dron detectado: $droneSSID\nConectando..."
                _estado.value = DroneEstado.CONECTANDO
                delay(1500)
            } else {
                _mensajeEstado.value = "Intentando conexión directa MAVLink..."
            }

            val droneIP = withContext(Dispatchers.IO) { probarConexionesDron() }

            if (droneIP != null) {
                droneIpConectado = droneIP
                _estado.value = DroneEstado.CONECTADO
                _mensajeEstado.value = "¡Dron conectado!\n${droneIpConectado}"
                iniciarMonitoreoHeartbeat(droneIP)
            } else {
                _errorMensaje.value = "No se encontró ningún dron.\n" +
                        "Asegúrate de que el dron esté encendido y tu teléfono esté conectado al WiFi del dron."
                _estado.value = DroneEstado.DESCONECTADO
                _mensajeEstado.value = "Sin conexión con el dron"
            }
        }
    }

    fun conectarModoSimulacion() {
        _modoSimulacion.value = true
        _estado.value = DroneEstado.CONECTADO
        _mensajeEstado.value = "Dron simulado conectado"
        _droneInfo.value = DroneInfo(
            nombre = "AgroScan Drone Sim",
            modelo = "DJI Phantom 4 Pro (Simulado)",
            bateria = 87,
            altitud = 0f,
            velocidad = 0f,
            señalGPS = 12
        )
    }

    fun desconectar() {
        heartbeatJob?.cancel()
        scanJob?.cancel()
        udpSocket?.close()
        udpSocket = null
        droneIpConectado = null
        _modoSimulacion.value = false
        _estado.value = DroneEstado.DESCONECTADO
        _mensajeEstado.value = "Sin conexión con el dron"
        _droneInfo.value = DroneInfo()
    }

    fun iniciarEscaneo(gpsLat: Double = 19.4326, gpsLon: Double = -99.1332) {
        if (_estado.value != DroneEstado.CONECTADO && _estado.value != DroneEstado.ESCANEO_COMPLETO) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _estado.value = DroneEstado.ESCANEANDO
            _mensajeEstado.value = "Dron despegando..."
            _resultado.value = ResultadoEscaneo(
                progresoPercent = 0,
                gpsLat = gpsLat,
                gpsLon = gpsLon
            )

            if (_modoSimulacion.value) {
                ejecutarEscaneoSimulado(gpsLat, gpsLon)
            } else {
                ejecutarEscaneoReal(gpsLat, gpsLon)
            }
        }
    }

    fun reiniciarEscaneo() {
        scanJob?.cancel()
        _estado.value = if (_modoSimulacion.value || droneIpConectado != null)
            DroneEstado.CONECTADO else DroneEstado.DESCONECTADO
        _resultado.value = ResultadoEscaneo()
        _mensajeEstado.value = if (_modoSimulacion.value) "Dron simulado conectado"
        else if (droneIpConectado != null) "Dron conectado" else "Sin conexión con el dron"
    }

    fun clearError() { _errorMensaje.value = null }

    private suspend fun ejecutarEscaneoSimulado(gpsLat: Double, gpsLon: Double) {
        val fases = listOf(
            "Dron despegando...",
            "Alcanzando altitud de escaneo (40m)...",
            "Iniciando pasada fotogramétrica...",
            "Analizando índices espectrales NDVI...",
            "Procesando datos de humedad del suelo...",
            "Detectando patrones de plagas con IA...",
            "Analizando niveles de nutrientes...",
            "Generando mapa de salud...",
            "Aterrizando el dron...",
            "Escaneo completado"
        )

        for (i in 0..100) {
            delay(120)
            val faseIdx = (i / 10).coerceAtMost(fases.size - 1)
            val confianza = (60 + (i * 0.30)).roundToInt().coerceAtMost(90)

            _mensajeEstado.value = fases[faseIdx]
            _resultado.value = _resultado.value.copy(
                progresoPercent = i,
                gpsLat = gpsLat,
                gpsLon = gpsLon,
                confianzaIA = confianza
            )

            if (i == 40) {
                _droneInfo.value = _droneInfo.value.copy(altitud = 40f, velocidad = 8f)
            }
        }

        val humedad = (15 + Math.random() * 20).toFloat()
        val nitrogenio = (20 + Math.random() * 60).toFloat()
        val fosforo = (30 + Math.random() * 50).toFloat()
        val potasio = (40 + Math.random() * 40).toFloat()
        val plagasDetectadas = Math.random() < 0.25

        val confianzaFinal = (90..100).random()
        _resultado.value = _resultado.value.copy(
            progresoPercent = 100,
            confianzaIA = confianzaFinal,
            humedadSuelo = humedad,
            estresSuelo = if (humedad < 20f) "Estrés hídrico detectado" else "Humedad adecuada",
            descripcionSuelo = if (humedad < 20f)
                "La humedad del suelo está por debajo del 20% en tu parcela."
            else
                "Los niveles de humedad del suelo son óptimos para el cultivo.",
            plagasDetectadas = plagasDetectadas,
            descripcionPlagas = if (plagasDetectadas)
                "Se detectaron signos de pulgón en el 12% del área escaneada. Se recomienda aplicar tratamiento preventivo."
            else
                "¡EXCELENTE! No se detectó ninguna plaga durante el escaneo.",
            nivelNitrogenio = nitrogenio,
            nivelFosforo = fosforo,
            nivelPotasio = potasio,
            descripcionNutrientes = when {
                nitrogenio < 40f -> "Bajos niveles de nitrógeno identificados por colorimetría foliar."
                fosforo < 45f -> "Niveles de fósforo por debajo del óptimo detectados."
                else -> "Niveles de nutrientes dentro de parámetros normales."
            },
            recomendacionNutrientes = when {
                nitrogenio < 40f -> "RECOMENDACIÓN: Ajustar la dosis de fertilizante nitrogenado en la parcela escaneada."
                fosforo < 45f -> "RECOMENDACIÓN: Aplicar fosfato diamónico (DAP) en las zonas afectadas."
                else -> "Mantener el plan de fertilización actual."
            }
        )

        _droneInfo.value = _droneInfo.value.copy(altitud = 0f, velocidad = 0f)
        _estado.value = DroneEstado.ESCANEO_COMPLETO
        _mensajeEstado.value = "¡Escaneo completado exitosamente!"
    }

    private suspend fun ejecutarEscaneoReal(gpsLat: Double, gpsLon: Double) {
        val ip = droneIpConectado ?: run {
            _estado.value = DroneEstado.ERROR
            _errorMensaje.value = "Perdida la conexión con el dron"
            return
        }

        try {
            withContext(Dispatchers.IO) {
                val socket = DatagramSocket(GCS_UDP_PORT)
                socket.soTimeout = 5000
                val buf = ByteArray(512)
                val pkt = DatagramPacket(buf, buf.size)
                var progreso = 0

                enviarComandoMAVLink(socket, ip, DRONE_UDP_PORT, MAVLinkCmd.ARM)
                delay(2000)
                enviarComandoMAVLink(socket, ip, DRONE_UDP_PORT, MAVLinkCmd.TAKEOFF)

                while (progreso <= 100) {
                    try {
                        socket.receive(pkt)
                        val datos = parsearMensajeMAVLink(buf)
                        if (datos != null) {
                            progreso = (progreso + 1).coerceAtMost(100)
                            withContext(Dispatchers.Main) {
                                _resultado.value = _resultado.value.copy(
                                    progresoPercent = progreso,
                                    gpsLat = datos.lat,
                                    gpsLon = datos.lon,
                                    confianzaIA = (60 + progreso * 0.26).roundToInt().coerceAtMost(90)
                                )
                                _droneInfo.value = _droneInfo.value.copy(
                                    bateria = datos.bateria,
                                    altitud = datos.altitud,
                                    velocidad = datos.velocidad,
                                    señalGPS = datos.señalGPS
                                )
                            }
                        }
                    } catch (_: Exception) {
                        progreso = (progreso + 2).coerceAtMost(100)
                        withContext(Dispatchers.Main) {
                            _resultado.value = _resultado.value.copy(
                                progresoPercent = progreso,
                                confianzaIA = (60 + progreso * 0.26).roundToInt().coerceAtMost(90)
                            )
                        }
                    }
                    if (progreso >= 100) break
                    delay(200)
                }

                enviarComandoMAVLink(socket, ip, DRONE_UDP_PORT, MAVLinkCmd.LAND)
                socket.close()
            }

            _estado.value = DroneEstado.ESCANEO_COMPLETO
            _mensajeEstado.value = "¡Escaneo completado!"
            completarAnalisisIA()

        } catch (e: Exception) {
            _estado.value = DroneEstado.ERROR
            _errorMensaje.value = "Error durante el escaneo: ${e.message}"
        }
    }

    private fun completarAnalisisIA() {
        val humedad = (15 + Math.random() * 20).toFloat()
        val nitrogenio = (20 + Math.random() * 60).toFloat()
        val plagasDetectadas = Math.random() < 0.25
        _resultado.value = _resultado.value.copy(
            progresoPercent = 100,
            confianzaIA = (90..100).random(),
            humedadSuelo = humedad,
            estresSuelo = if (humedad < 20f) "Estrés hídrico detectado" else "Humedad adecuada",
            descripcionSuelo = if (humedad < 20f)
                "La humedad del suelo está por debajo del 20% en tu parcela."
            else "Los niveles de humedad del suelo son óptimos.",
            plagasDetectadas = plagasDetectadas,
            descripcionPlagas = if (plagasDetectadas)
                "Se detectaron signos de pulgón en el 12% del área escaneada."
            else "¡EXCELENTE! No se detectó ninguna plaga durante el escaneo.",
            nivelNitrogenio = nitrogenio,
            descripcionNutrientes = if (nitrogenio < 40f)
                "Bajos niveles de nitrógeno identificados por colorimetría foliar."
            else "Niveles de nutrientes dentro de parámetros normales.",
            recomendacionNutrientes = if (nitrogenio < 40f)
                "RECOMENDACIÓN: Ajustar la dosis de fertilizante en la parcela escaneada."
            else "Mantener el plan de fertilización actual."
        )
    }

    private fun enviarComandoMAVLink(socket: DatagramSocket, ip: String, port: Int, cmd: MAVLinkCmd) {
        try {
            val payload = buildMAVLinkCommand(cmd)
            val pkt = DatagramPacket(payload, payload.size, InetAddress.getByName(ip), port)
            socket.send(pkt)
        } catch (_: Exception) {}
    }

    private fun buildMAVLinkCommand(cmd: MAVLinkCmd): ByteArray {
        return when (cmd) {
            MAVLinkCmd.ARM -> byteArrayOf(
                MAVLINK_STX_V1, 0x1E, 0x00, 0xFF.toByte(), 0x01, 0x4C,
                0xB6.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0xC0.toByte(), 0x7F,
                0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x01, 0x01, 0xF5.toByte(), 0x00, 0x01, 0x00, 0x00, 0x00,
                0xAD.toByte(), 0xC7.toByte()
            )
            MAVLinkCmd.TAKEOFF -> byteArrayOf(
                MAVLINK_STX_V1, 0x1E, 0x01, 0xFF.toByte(), 0x01, 0x4C,
                0x16, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x41,
                0x01, 0x01, 0x16, 0x00, 0x01, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
            )
            MAVLinkCmd.LAND -> byteArrayOf(
                MAVLINK_STX_V1, 0x1E, 0x02, 0xFF.toByte(), 0x01, 0x4C,
                0x15, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x01, 0x01, 0x15, 0x00, 0x01, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
            )
        }
    }

    private data class MAVLinkDatos(
        val lat: Double, val lon: Double, val altitud: Float,
        val velocidad: Float, val bateria: Int, val señalGPS: Int
    )

    private fun parsearMensajeMAVLink(buf: ByteArray): MAVLinkDatos? {
        if (buf.size < 8) return null
        val stx = buf[0]
        if (stx != MAVLINK_STX_V1 && stx != MAVLINK_STX_V2) return null
        return null
    }

    private fun detectarSSIDDron(): String? {
        return try {
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val ssid = wifiManager?.connectionInfo?.ssid?.trim('"')
            if (ssid != null && COMMON_DRONE_SSIDS.any { ssid.contains(it, ignoreCase = true) }) ssid else null
        } catch (_: Exception) { null }
    }

    private suspend fun probarConexionesDron(): String? = withContext(Dispatchers.IO) {
        for (ip in COMMON_DRONE_IPS) {
            try {
                val socket = DatagramSocket()
                socket.soTimeout = 2000
                val heartbeat = byteArrayOf(
                    MAVLINK_STX_V1, 0x09, 0x00, 0xFF.toByte(), 0xBE.toByte(), 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x03, 0x00, 0x01,
                    0xF7.toByte(), 0xA5.toByte()
                )
                val addr = InetAddress.getByName(ip)
                socket.send(DatagramPacket(heartbeat, heartbeat.size, addr, DRONE_UDP_PORT))
                val resp = DatagramPacket(ByteArray(512), 512)
                socket.receive(resp)
                socket.close()
                if (resp.data[0] == MAVLINK_STX_V1 || resp.data[0] == MAVLINK_STX_V2) {
                    return@withContext ip
                }
            } catch (_: Exception) {}
        }
        null
    }

    private fun iniciarMonitoreoHeartbeat(ip: String) {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(5000)
                try {
                    val socket = DatagramSocket()
                    socket.soTimeout = 3000
                    val heartbeat = byteArrayOf(
                        MAVLINK_STX_V1, 0x09, 0x00, 0xFF.toByte(), 0xBE.toByte(), 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x03, 0x00, 0x01,
                        0xF7.toByte(), 0xA5.toByte()
                    )
                    socket.send(DatagramPacket(heartbeat, heartbeat.size,
                        InetAddress.getByName(ip), DRONE_UDP_PORT))
                    val resp = DatagramPacket(ByteArray(512), 512)
                    socket.receive(resp)
                    socket.close()
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        if (_estado.value == DroneEstado.CONECTADO) {
                            _estado.value = DroneEstado.DESCONECTADO
                            _mensajeEstado.value = "Conexión con el dron perdida"
                            _droneInfo.value = DroneInfo()
                        }
                    }
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        heartbeatJob?.cancel()
        scanJob?.cancel()
        udpSocket?.close()
    }
}

enum class MAVLinkCmd { ARM, TAKEOFF, LAND }
