package com.example.reloj.ui

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.contentType
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ApiClient {

    // 🔧 Pon aquí tu URL real de la Function App (SIN barra al final)
    private const val BASE_URL =
        "https://miapi-eng9f6fkcbbfcudk.brazilsouth-01.azurewebsites.net/api"

    // ========== MODELOS ==========
    @Serializable
    data class LoginRequest(val email: String, val password: String)

    @Serializable
    data class LoginResponse(
        val message: String,
        val rut: String? = null,
        val nombre: String? = null,
        val email: String? = null
    )

    @Serializable
    data class AsistenciaMensajeRequest(
        @SerialName("trabajador_id") val trabajadorId: Int? = null,
        val email: String? = null,
        val rut: String? = null,
        val mensaje: String
    )

    @Serializable
    data class AsistenciaRegistro(
        val id: Int? = null,
        val fecha: String? = null,
        @SerialName("hora_entrada") val horaEntrada: String? = null,
        @SerialName("hora_salida") val horaSalida: String? = null,
        val geolocalizacion: String? = null,
        @SerialName("trabajador_id") val trabajadorId: Int? = null,
        @SerialName("numero_asistencia") val numeroAsistencia: Int? = null,
        @SerialName("is_asistencia") val isAsistencia: Boolean? = null,
        val justificado: Boolean? = null,
        @SerialName("procesado_ia") val procesadoIa: Boolean? = null,
        val mensaje: String? = null,
        val categoria: String? = null,
        @SerialName("fecha_inicio_inasistencia") val fechaInicioInasistencia: String? = null,
        @SerialName("fecha_fin_inasistencia") val fechaFinInasistencia: String? = null,
        @SerialName("duracion_dias") val duracionDias: Int? = null
    )

    @Serializable
    data class AsistenciaMensajeResponse(
        val message: String,
        val registro: AsistenciaRegistro? = null
    )

    // ========== CLIENTE ==========
    val client = HttpClient(CIO) {
        expectSuccess = false // no lanzar excepción automática en 4xx/5xx

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                    encodeDefaults = true
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 1)
            exponentialDelay()
        }
    }

    // ========== ENDPOINTS ==========

    suspend fun login(email: String, password: String): LoginResponse {
        return client.post("$BASE_URL/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
    }

    /**
     * Envía el mensaje de justificación y actualiza la ÚLTIMA asistencia del trabajador.
     * Requiere al menos uno: trabajadorId, email o rut.
     * Si la respuesta no calza con el modelo, devuelve un AsistenciaMensajeResponse con el texto crudo.
     */
    suspend fun enviarMensajeAsistencia(
        mensaje: String,
        trabajadorId: Int? = null,
        email: String? = null,
        rut: String? = null
    ): AsistenciaMensajeResponse {
        val req = when {
            trabajadorId != null -> AsistenciaMensajeRequest(trabajadorId = trabajadorId, mensaje = mensaje)
            !email.isNullOrBlank() -> AsistenciaMensajeRequest(email = email, mensaje = mensaje)
            !rut.isNullOrBlank() -> AsistenciaMensajeRequest(rut = rut, mensaje = mensaje)
            else -> return AsistenciaMensajeResponse(
                message = "Falta identificador (trabajador_id, email o rut).",
                registro = null
            )
        }

        val response = client.post("$BASE_URL/asistencia/mensaje") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        val raw = response.bodyAsText()

        // Intenta parsear; si falla, retorna algo legible en 'message' sin crashear
        return try {
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }.decodeFromString(AsistenciaMensajeResponse.serializer(), raw)
        } catch (_: Throwable) {
            AsistenciaMensajeResponse(
                message = if (raw.isNotBlank()) raw else "Respuesta inesperada del servidor (status ${response.status}).",
                registro = null
            )
        }
    }
}

