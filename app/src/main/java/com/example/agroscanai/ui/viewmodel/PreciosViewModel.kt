package com.example.agroscanai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agroscanai.data.model.PrecioMercado
import com.example.agroscanai.data.model.TendenciaPrecio
import com.example.agroscanai.data.remote.YahooFinanceApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class PreciosUiState {
    object Cargando : PreciosUiState()
    data class Exito(val precios: List<PrecioMercado>) : PreciosUiState()
    data class Error(val mensaje: String) : PreciosUiState()
}

private data class Commodity(
    val symbol: String,
    val nombre: String,
    // factor para convertir el precio crudo a USD/tonelada
    val factorUsdTon: Double
)

// Futuros agrícolas de CBOT/ICE vía Yahoo Finance
// Maíz/Trigo/Soja: cotizados en cents/bushel
// Azúcar/Café/Algodón: cotizados en cents/libra
// Arroz: cotizado en USD/cwt (quintal americano)
private val COMMODITIES = listOf(
    Commodity("ZC=F", "Maíz",    39.368 / 100.0),   // 1 t ≈ 39.37 bushels; ÷100 para cents→USD
    Commodity("ZW=F", "Trigo",   36.744 / 100.0),
    Commodity("ZS=F", "Soja",    36.744 / 100.0),
    Commodity("ZR=F", "Arroz",   22.046),            // USD/cwt → USD/t: × 22.046
    Commodity("SB=F", "Azúcar",  2204.62 / 100.0),  // cents/libra → USD/t
    Commodity("KC=F", "Café",    2204.62 / 100.0),
    Commodity("CT=F", "Algodón", 2204.62 / 100.0)
)

class PreciosViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PreciosUiState>(PreciosUiState.Cargando)
    val uiState: StateFlow<PreciosUiState> = _uiState

    private val api = YahooFinanceApi.instance

    init { cargarPrecios() }

    fun cargarPrecios() {
        viewModelScope.launch {
            _uiState.value = PreciosUiState.Cargando
            val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            // Llamadas en paralelo, una por commodity
            val deferreds = COMMODITIES.mapIndexed { idx, commodity ->
                async {
                    try {
                        val url = YahooFinanceApi.urlParaSimbolo(commodity.symbol)
                        val resp = api.getCotizacion(url)
                        val meta = resp.chart.result?.firstOrNull()?.meta
                            ?: return@async null

                        val precioUsd = meta.precioActual * commodity.factorUsdTon
                        val minUsd    = meta.minDia      * commodity.factorUsdTon
                        val maxUsd    = meta.maxDia      * commodity.factorUsdTon
                        val prevUsd   = meta.cierrePrevio * commodity.factorUsdTon

                        val cambio = precioUsd - prevUsd
                        val tendencia = when {
                            cambio >  prevUsd * 0.005 -> TendenciaPrecio.SUBIENDO
                            cambio < -prevUsd * 0.005 -> TendenciaPrecio.BAJANDO
                            else                      -> TendenciaPrecio.ESTABLE
                        }

                        PrecioMercado(
                            id           = idx + 1,
                            grano        = commodity.nombre,
                            precioActual = precioUsd,
                            precioMinimo = minUsd,
                            precioMaximo = maxUsd,
                            unidad       = "tonelada",
                            region       = "Mercado Internacional",
                            fecha        = hoy,
                            tendencia    = tendencia,
                            recomendacion = recomendacion(commodity.nombre, tendencia)
                        )
                    } catch (_: Exception) { null }
                }
            }

            val precios = deferreds.map { it.await() }.filterNotNull()

            _uiState.value = if (precios.isNotEmpty())
                PreciosUiState.Exito(precios)
            else
                PreciosUiState.Error("No se pudieron obtener las cotizaciones.\nVerifica tu conexión a internet.")
        }
    }

    private fun recomendacion(grano: String, tendencia: TendenciaPrecio) = when (tendencia) {
        TendenciaPrecio.SUBIENDO -> "Precio en alza — buen momento para vender $grano."
        TendenciaPrecio.BAJANDO  -> "Tendencia a la baja — evalúa almacenar $grano y esperar mejor precio."
        TendenciaPrecio.ESTABLE  -> "Mercado estable para $grano — sin urgencia de compra o venta."
    }
}
