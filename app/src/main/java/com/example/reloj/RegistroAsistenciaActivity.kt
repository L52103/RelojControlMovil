package com.example.reloj

import android.os.Bundle
import android.util.Base64
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
import java.security.MessageDigest
import java.util.concurrent.Executor

class RegistroAsistenciaActivity : AppCompatActivity() {

    private lateinit var biometricPromptIngreso: BiometricPrompt
    private lateinit var biometricPromptSalida: BiometricPrompt
    private lateinit var promptInfoIngreso: BiometricPrompt.PromptInfo
    private lateinit var promptInfoSalida: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor
    private val apiBaseUrl = "https://apilogin.azurewebsites.net/api/ActualizarBiometria"
    private val client = OkHttpClient()

    private var rutUsuario: String? = null
    private var biometriaHuella: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.registro_asistencia)

        val sharedPref = getSharedPreferences("MiAppPrefs", MODE_PRIVATE)
        rutUsuario = sharedPref.getString("rut", null)
        biometriaHuella = sharedPref.getString("biometria_huella", null)

        if (rutUsuario.isNullOrEmpty()) {
            Toast.makeText(this, "Usuario no autenticado. Por favor, inicia sesión.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        executor = ContextCompat.getMainExecutor(this)

        // Prompt info para ingreso
        promptInfoIngreso = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verifica tu identidad")
            .setSubtitle("Usa tu huella para registrar tu ingreso")
            .setNegativeButtonText("Cancelar")
            .build()

        // Prompt info para salida
        promptInfoSalida = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verifica tu identidad")
            .setSubtitle("Usa tu huella para registrar tu salida")
            .setNegativeButtonText("Cancelar")
            .build()

        // BiometricPrompt para ingreso
        biometricPromptIngreso = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                runOnUiThread {
                    Toast.makeText(applicationContext, "✅ Huella reconocida para ingreso", Toast.LENGTH_SHORT).show()
                }
                rutUsuario?.let { rut ->
                    val tokenLocal = generarTokenDesdeRut(rut)
                    if (biometriaHuella.isNullOrEmpty()) {
                        guardarTokenEnApi(rut, tokenLocal)
                    } else if (biometriaHuella == tokenLocal) {
                        runOnUiThread {
                            Toast.makeText(applicationContext, "✅ Ingreso autorizado", Toast.LENGTH_LONG).show()
                            // Registrar asistencia ingreso aquí
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(applicationContext, "❌ Huella no coincide con la registrada", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                runOnUiThread {
                    Toast.makeText(applicationContext, "❌ Huella no reconocida para ingreso", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                runOnUiThread {
                    Toast.makeText(applicationContext, "⚠️ Error ingreso: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // BiometricPrompt para salida
        biometricPromptSalida = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                runOnUiThread {
                    Toast.makeText(applicationContext, "✅ Acción registrada con éxito", Toast.LENGTH_LONG).show()
                    // Registrar asistencia salida aquí
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                runOnUiThread {
                    Toast.makeText(applicationContext, "❌ Huella no reconocida para salida", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                runOnUiThread {
                    Toast.makeText(applicationContext, "⚠️ Error salida: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        })

        val buttonIngreso: Button = findViewById(R.id.button2)
        buttonIngreso.setOnClickListener {
            verificarBiometriaIngreso()
        }

        val buttonSalida: Button = findViewById(R.id.button3)
        buttonSalida.setOnClickListener {
            verificarBiometriaSalida()
        }
    }

    private fun verificarBiometriaIngreso() {
        val biometricManager = BiometricManager.from(this)
        val puedeAutenticar = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        when (puedeAutenticar) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricPromptIngreso.authenticate(promptInfoIngreso)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(this, "Tu dispositivo no tiene sensor de huella", Toast.LENGTH_LONG).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(this, "El sensor no está disponible", Toast.LENGTH_LONG).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Toast.makeText(this, "No hay huellas registradas", Toast.LENGTH_LONG).show()
            else ->
                Toast.makeText(this, "Autenticación biométrica no disponible", Toast.LENGTH_LONG).show()
        }
    }

    private fun verificarBiometriaSalida() {
        val biometricManager = BiometricManager.from(this)
        val puedeAutenticar = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        when (puedeAutenticar) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricPromptSalida.authenticate(promptInfoSalida)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(this, "Tu dispositivo no tiene sensor de huella", Toast.LENGTH_LONG).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(this, "El sensor no está disponible", Toast.LENGTH_LONG).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Toast.makeText(this, "No hay huellas registradas", Toast.LENGTH_LONG).show()
            else ->
                Toast.makeText(this, "Autenticación biométrica no disponible", Toast.LENGTH_LONG).show()
        }
    }

    private fun generarTokenDesdeRut(rut: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(rut.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun limpiarRut(rut: String): String {
        return rut.replace(".", "").replace("-", "").trim()
    }

    private fun guardarTokenEnApi(rutOriginal: String, token: String) {
        val rutLimpio = limpiarRut(rutOriginal)
        val jsonBody = JSONObject().apply {
            put("biometria_huella", token)
        }

        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "$apiBaseUrl?rut=$rutLimpio"

        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "❌ No se pudo guardar el token: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        val sharedPref = getSharedPreferences("MiAppPrefs", MODE_PRIVATE)
                        sharedPref.edit().putString("biometria_huella", token).apply()
                        Toast.makeText(applicationContext, "✅ Huella registrada por primera vez", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(applicationContext, "⚠️ Error al guardar token: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
