package com.example.reloj.ui

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Define las clases de datos para la petición y respuesta
@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String)  // Ajusta según la respuesta de tu API

object ApiClient {
    private const val BASE_URL = "https://miapi-eng9f6fkcbbfcudk.brazilsouth-01.azurewebsites.net/api"

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    suspend fun login(email: String, password: String): LoginResponse {
        return client.post("$BASE_URL/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body<LoginResponse>()
    }
}
