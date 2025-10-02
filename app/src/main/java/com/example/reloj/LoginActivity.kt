package com.example.reloj

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// --------- MODELOS ---------

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val rut: String,
    val nombre: String,
    val email: String
    // Si luego tu API devuelve trabajador_id, añádelo aquí: val trabajador_id: Int?
)

// --------- API ---------

interface ApiService {
    @POST("login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
}

object ApiClient {
    private const val BASE_URL =
        "https://miapi-eng9f6fkcbbfcudk.brazilsouth-01.azurewebsites.net/api/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

// --------- ACTIVITY ---------

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        val emailField = findViewById<EditText>(R.id.editTextTextEmailAddress)
        val passwordField = findViewById<EditText>(R.id.editTextTextPassword)
        val btnLogin = findViewById<Button>(R.id.button)

        btnLogin.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.login(LoginRequest(email, password))

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null) {

                            // 🔐 Guardar datos para el resto de la app
                            val sharedPref = getSharedPreferences("MiAppPrefs", MODE_PRIVATE)
                            sharedPref.edit().apply {
                                putString("rut", loginResponse.rut)
                                putString("nombre", loginResponse.nombre)
                                // Guarda ambas claves por compatibilidad:
                                putString("EMAIL", loginResponse.email) // <- en MAYÚSCULAS (lo que usa RegistroAsistencia)
                                putString("email", loginResponse.email) // <- por si otras pantallas usan minúsculas
                                // Si en el futuro agregas trabajador_id:
                                // putInt("trabajador_id", loginResponse.trabajador_id ?: -1)
                                apply()
                            }

                            Toast.makeText(
                                this@LoginActivity,
                                "Login exitoso. Bienvenido ${loginResponse.nombre}",
                                Toast.LENGTH_SHORT
                            ).show()

                            // ➜ Navega a tu menú principal como ya lo hacías
                            startActivity(Intent(this@LoginActivity, menuActivity::class.java))
                            finish()

                            // (Opcional) Si quisieras abrir directo RegistroAsistenciaActivity y pasar el email por Intent:
                            /*
                            val i = Intent(this@LoginActivity, RegistroAsistenciaActivity::class.java)
                            i.putExtra("EMAIL", loginResponse.email)
                            startActivity(i)
                            finish()
                            */
                        } else {
                            Toast.makeText(this@LoginActivity, "Respuesta vacía", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Credenciales inválidas", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error al iniciar sesión: ${e.message ?: "desconocido"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
