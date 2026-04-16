package com.example.agroscanai.data.model

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val telefono: String = "",
    val region: String = "",
    val tipoCuenta: String = TipoCuenta.BASICO.name,
    val fechaRegistro: String = "",
    val fotoPerfil: String = "",
    val notificacionesPush: Boolean = true,
    val sonidosAlerta: Boolean = true,
    val unidadesMetricas: Boolean = true,
    val modoOscuro: Boolean = false
) {
    fun tipoCuentaEnum(): TipoCuenta = try {
        TipoCuenta.valueOf(tipoCuenta)
    } catch (e: Exception) {
        TipoCuenta.BASICO
    }

    fun nombreCompleto(): String = when {
        nombre.isNotBlank() && apellido.isNotBlank() -> "$nombre $apellido"
        nombre.isNotBlank() -> nombre
        email.isNotBlank()  -> email.substringBefore("@").replaceFirstChar { it.uppercase() }
        else -> "Usuario"
    }

    fun iniciales(): String {
        val n = nombre.trim()
        val a = apellido.trim()
        return when {
            n.isNotBlank() && a.isNotBlank() -> "${n.first().uppercaseChar()}${a.first().uppercaseChar()}"
            n.isNotBlank() && n.contains(" ") -> "${n.split(" ")[0].first().uppercaseChar()}${n.split(" ")[1].first().uppercaseChar()}"
            n.isNotBlank() -> n.take(2).uppercase()
            email.isNotBlank() -> email.take(2).uppercase()
            else -> "US"
        }
    }
}

enum class TipoCuenta {
    BASICO,
    PREMIUM,
    EMPRESARIAL
}