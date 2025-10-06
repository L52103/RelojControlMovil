package com.example.reloj.data

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class AsistenciaRegistro( // clase de cada fila de la tabla asistencia de
    val id: Int?,
    val fecha: String?,
    val hora_entrada: String?,
    val hora_salida: String?,
    val geolocalizacion: String?,
    val trabajador_id: Int?,
    val numero_asistencia: Int?,
    val is_asistencia: Boolean?,
    val justificado: Boolean?,
    val procesado_ia: Boolean?,
    val mensaje: String?,
    val categoria: String?,
    val fecha_inicio_inasistencia: String?,
    val fecha_fin_inasistencia: String?,
    val duracion_dias: Int?
)

data class ListadoAsistenciaResponse(
    val registros: List<AsistenciaRegistro> = emptyList()
)

interface ApiAsistenciaService { // peticion GET
    @GET("asistencia/listar")
    suspend fun listarAsistenciaPorEmail(
        @Query("email") email: String
    ): Response<ListadoAsistenciaResponse>
}

object ApiAsistenciaClient {
    private const val BASE_URL = // url API
        "https://miapi-eng9f6fkcbbfcudk.brazilsouth-01.azurewebsites.net/api/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: ApiAsistenciaService = retrofit.create(ApiAsistenciaService::class.java)
}
