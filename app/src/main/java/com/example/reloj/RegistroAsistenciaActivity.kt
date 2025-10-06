package com.example.reloj

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executor

class RegistroAsistenciaActivity : AppCompatActivity() {

    private val TAG = "Asistencia"

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfoIngreso: BiometricPrompt.PromptInfo
    private lateinit var promptInfoSalida: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor

    private enum class Tipo { INGRESO, SALIDA }
    private var currentAction: Tipo? = null

    // API
    private val asistenciaApiBase = // Api base
        "https://miapi-eng9f6fkcbbfcudk.brazilsouth-01.azurewebsites.net/api"

    private val client = OkHttpClient()

    private var emailUsuario: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.registro_asistencia)

        // EMAIL desde SharedPreferences
        val sharedPref = getSharedPreferences("MiAppPrefs", MODE_PRIVATE)
        val emailFromPrefs = sharedPref.getString("EMAIL", null)
        val emailFromIntent = intent.getStringExtra("EMAIL")
        emailUsuario = emailFromPrefs ?: emailFromIntent

        if (emailUsuario.isNullOrBlank()) {
            Toast.makeText(this, "Falta email del trabajador. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
            finish(); return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val s = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(s.left, s.top, s.right, s.bottom); insets
        }

        executor = ContextCompat.getMainExecutor(this)

        // Prompts separados
        promptInfoIngreso = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verifica tu identidad")
            .setSubtitle("Usa tu huella para registrar tu ingreso")
            .setNegativeButtonText("Cancelar")
            .build()

        promptInfoSalida = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verifica tu identidad")
            .setSubtitle("Usa tu huella para registrar tu salida")
            .setNegativeButtonText("Cancelar")
            .build()

        // BiometricPrompt con callback que decide por currentAction
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val email = emailUsuario!!.trim()
                    val action = currentAction
                    Log.d(TAG, " AUTH OK (${action?.name ?: "SIN ACCIÓN"}) para $email")

                    if (action == null) {
                        Toast.makeText(applicationContext, " Acción no determinada.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    when (action) {
                        Tipo.INGRESO -> {
                            Toast.makeText(applicationContext, " Huella verificada (Ingreso)", Toast.LENGTH_SHORT).show()
                            marcarIngresoEnApi(email)
                        }
                        Tipo.SALIDA -> {
                            Toast.makeText(applicationContext, "Huella verificada (Salida)", Toast.LENGTH_SHORT).show()
                            marcarSalidaEnApi(email)
                        }
                    }

                    // limpiar acción al terminar
                    currentAction = null
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    val action = currentAction
                    val texto = if (action == Tipo.SALIDA) "Salida" else "Ingreso"
                    Toast.makeText(applicationContext, " Huella no reconocida ($texto)", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(code: Int, errString: CharSequence) {
                    super.onAuthenticationError(code, errString)
                    val action = currentAction
                    val texto = if (action == Tipo.SALIDA) "Salida" else "Ingreso"
                    Toast.makeText(applicationContext, "️ Error $texto: $errString", Toast.LENGTH_SHORT).show()
                    currentAction = null
                }
            })

        // Botón INGRESO
        val buttonIngreso: Button = findViewById(R.id.button2)

        // fuerza el texto layout
        buttonIngreso.text = "Ingreso"
        buttonIngreso.setOnClickListener {
            currentAction = Tipo.INGRESO
            Log.d(TAG, " CLICK BOTÓN INGRESO → currentAction=INGRESO")
            verificarBiometriaIngreso()
        }

        // Botón SALIDA
        val buttonSalida: Button = findViewById(R.id.button3)
        buttonSalida.text = "Salida"
        buttonSalida.isEnabled = true
        buttonSalida.alpha = 1f
        buttonSalida.setOnClickListener {
            currentAction = Tipo.SALIDA
            Log.d(TAG, " CLICK BOTÓN SALIDA → currentAction=SALIDA")
            verificarBiometriaSalida()
        }
    }

    //  Biometría
    private fun verificarBiometriaIngreso() {
        val bm = BiometricManager.from(this)
        val can = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        when (can) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "Biometría disponible → autenticando (INGRESO)")
                biometricPrompt.authenticate(promptInfoIngreso)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(this, "Sin sensor de huella", Toast.LENGTH_LONG).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(this, "Sensor no disponible", Toast.LENGTH_LONG).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Toast.makeText(this, "No hay huellas registradas", Toast.LENGTH_LONG).show()
            else ->
                Toast.makeText(this, "Biometría no disponible (Ingreso)", Toast.LENGTH_LONG).show()
        }
    }

    private fun verificarBiometriaSalida() {
        val bm = BiometricManager.from(this)
        val can = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        when (can) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "Biometría disponible → autenticando (SALIDA)")
                biometricPrompt.authenticate(promptInfoSalida)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(this, "Sin sensor de huella", Toast.LENGTH_LONG).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(this, "Sensor no disponible", Toast.LENGTH_LONG).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Toast.makeText(this, "No hay huellas registradas", Toast.LENGTH_LONG).show()
            else ->
                Toast.makeText(this, "Biometría no disponible (Salida)", Toast.LENGTH_LONG).show()
        }
    }

    // API (Ingreso)
    private fun marcarIngresoEnApi(email: String) {
        val jsonBody = JSONObject().apply { put("email", email) }
        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "$asistenciaApiBase/asistencia/ingreso"

        Log.d(TAG, " POST $url  body=$jsonBody")

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = runOnUiThread {
                Log.e(TAG, " FALLÓ ingreso: ${e.message}", e)
                Toast.makeText(applicationContext, " No se pudo marcar el ingreso: ${e.message}", Toast.LENGTH_LONG).show()
            }
            override fun onResponse(call: Call, response: Response) {
                val status = response.code
                val resp = response.body?.string()
                Log.d(TAG, " RESPUESTA ingreso: HTTP $status, body=$resp")
                runOnUiThread {
                    if (response.isSuccessful) {
                        val msg = if (status == 201) " Ingreso registrado (nuevo)" else "Ingreso actualizado"
                        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                    } else {
                        val msg = if (status == 409)
                            "️ La asistencia de hoy ya tiene hora de entrada."
                        else " Error al marcar ingreso ($status)"
                        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                    }
                }
                response.close()
            }
        })
    }

    //  API (Salida)
    private fun marcarSalidaEnApi(email: String) {
        val jsonBody = JSONObject().apply { put("email", email) }
        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "$asistenciaApiBase/asistencia/salida"

        Log.d(TAG, " POST $url  body=$jsonBody")

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = runOnUiThread {
                Log.e(TAG, " FALLÓ salida: ${e.message}", e)
                Toast.makeText(applicationContext, " No se pudo marcar la salida: ${e.message}", Toast.LENGTH_LONG).show()
            }
            override fun onResponse(call: Call, response: Response) {
                val status = response.code
                val resp = response.body?.string()
                Log.d(TAG, "RESPUESTA salida: HTTP $status, body=$resp")
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Salida marcada", Toast.LENGTH_LONG).show()
                    } else {
                        val msg = when (status) {
                            404 -> "No existe asistencia de hoy para marcar salida."
                            409 -> "La asistencia de hoy ya tiene hora de salida."
                            else -> "Error al marcar salida ($status)"
                        }
                        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                    }
                }
                response.close()
            }
        })
    }
}

