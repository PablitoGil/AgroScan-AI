package com.example.agroscanai.data.model

data class Usuario(
    val id: Int = 0,
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val telefono: String = "",
    val region: String = "",
    val tipoCuenta: TipoCuenta = TipoCuenta.BASICO,
    val fechaRegistro: String = "",
    val fotoPerfil: String = ""
)

enum class TipoCuenta {
    BASICO,
    PREMIUM,
    EMPRESARIAL
}