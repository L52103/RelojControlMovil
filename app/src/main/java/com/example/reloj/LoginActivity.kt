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

// Data class para la petición de login
data class LoginRequest(
    val email: String,
    val password: String
)

// Data class para la respuesta del login
data class LoginResponse(
    val token: String
)

// Interfaz del servicio API para definir el endpoint de login
interface ApiService {
    @POST("Login")  // Asegúrate de que el endpoint sea correcto (concatenado a la URL base)
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
}

// Objeto singleton para configurar Retrofit y brindar la instancia de ApiService
object ApiClient {
    // La URL base de tu API en Azure (debe terminar en "/" para que Retrofit concatene el endpoint correctamente)
    private const val BASE_URL = "https://apilogin.azurewebsites.net/api/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

// Activity para el login
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establece el layout del login (asegúrate de que tu recurso R.layout.login existe y tenga los IDs correctos)
        setContentView(R.layout.login)

        // Referencias a los elementos de la UI
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

            // Ejecuta la petición de login de forma asíncrona usando coroutine
            lifecycleScope.launch {
                try {
                    // Crea la petición con los datos ingresados
                    val loginRequest = LoginRequest(email, password)
                    // Llama al endpoint de login definido en la instancia de Retrofit
                    val response = ApiClient.apiService.login(loginRequest)

                    if (response.isSuccessful) {
                        // Si la respuesta es exitosa, se obtiene el objeto LoginResponse
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Login exitoso. Token: ${loginResponse.token}",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Guarda el token (por ejemplo, en SharedPreferences) y navega a la siguiente Activity
                            startActivity(Intent(this@LoginActivity, menuActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Respuesta vacía", Toast.LENGTH_LONG)
                                .show()
                        }
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Credenciales inválidas o error en la respuesta",
                            Toast.LENGTH_LONG
                        ).show()
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
