package com.example.reloj

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
import java.time.LocalDate
import java.util.concurrent.Executor

class RegistroAsistenciaActivity : AppCompatActivity() {

    private val TAG = "Asistencia"

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfoIngreso: BiometricPrompt.PromptInfo
    private lateinit var promptInfoSalida: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor

    private enum class Tipo { INGRESO, SALIDA }
    private var currentAction: Tipo? = null
    private var ultimoIntentoFallido: Tipo? = null

    private lateinit var btnProblemasAsistencia: Button

    // API
    private val asistenciaApiBase =
        "https://miapi-eng9f6fkcbbfcudk.brazilsouth-01.azurewebsites.net/api"

    private val client = OkHttpClient()

    private var emailUsuario: String? = null

    // SharedPreferences para controlar 1 asistencia por día
    private lateinit var sharedPref: SharedPreferences
    private val KEY_LAST_INGRESO_DATE = "ULTIMO_INGRESO_FECHA"
    private val KEY_LAST_SALIDA_DATE = "ULTIMO_SALIDA_FECHA"

    private fun hoyString(): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return format.format(java.util.Date())
    }


    private fun yaMarcoAsistenciaHoy(tipo: Tipo): Boolean {
        val hoy = hoyString()
        val key = if (tipo == Tipo.INGRESO) KEY_LAST_INGRESO_DATE else KEY_LAST_SALIDA_DATE
        return sharedPref.getString(key, null) == hoy
    }

    private fun marcarHoy(tipo: Tipo) {
        val hoy = hoyString()
        val key = if (tipo == Tipo.INGRESO) KEY_LAST_INGRESO_DATE else KEY_LAST_SALIDA_DATE
        sharedPref.edit().putString(key, hoy).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.registro_asistencia)

        sharedPref = getSharedPreferences("MiAppPrefs", MODE_PRIVATE)

        // EMAIL desde SharedPreferences o Intent
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

        // Botón fallback "¿Problemas con registrar asistencia?"
        btnProblemasAsistencia = findViewById(R.id.btnProblemasAsistencia)
        btnProblemasAsistencia.visibility = View.GONE
        btnProblemasAsistencia.setOnClickListener {
            mostrarDialogoPassword()
        }

        // Prompts biometría
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

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val email = emailUsuario!!.trim()
                    val action = currentAction
                    Log.d(TAG, "AUTH OK (${action?.name ?: "SIN ACCIÓN"}) para $email")

                    if (action == null) {
                        Toast.makeText(applicationContext, "Acción no determinada.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    when (action) {
                        Tipo.INGRESO -> {
                            Toast.makeText(applicationContext, "Huella verificada (Ingreso)", Toast.LENGTH_SHORT).show()
                            marcarIngresoEnApi(email)
                        }
                        Tipo.SALIDA -> {
                            Toast.makeText(applicationContext, "Huella verificada (Salida)", Toast.LENGTH_SHORT).show()
                            marcarSalidaEnApi(email)
                        }
                    }

                    currentAction = null
                    ultimoIntentoFallido = null
                    btnProblemasAsistencia.visibility = View.GONE
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    val action = currentAction
                    val texto = if (action == Tipo.SALIDA) "Salida" else "Ingreso"
                    Toast.makeText(applicationContext, "Huella no reconocida ($texto)", Toast.LENGTH_SHORT).show()

                    if (action != null) {
                        habilitarFallback(action)
                    }
                }

                override fun onAuthenticationError(code: Int, errString: CharSequence) {
                    super.onAuthenticationError(code, errString)
                    val action = currentAction
                    val texto = if (action == Tipo.SALIDA) "Salida" else "Ingreso"
                    Toast.makeText(applicationContext, "Error $texto: $errString", Toast.LENGTH_SHORT).show()

                    if (action != null) {
                        habilitarFallback(action)
                    }

                    currentAction = null
                }
            })

        // Botón INGRESO
        val buttonIngreso: Button = findViewById(R.id.button2)
        buttonIngreso.text = "Ingreso"
        buttonIngreso.setOnClickListener {
            if (yaMarcoAsistenciaHoy(Tipo.INGRESO)) {
                Toast.makeText(this, "La asistencia de hoy (Ingreso) ya fue registrada.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            currentAction = Tipo.INGRESO
            Log.d(TAG, "CLICK BOTÓN INGRESO → currentAction=INGRESO")
            verificarBiometriaIngreso()
        }

        // Botón SALIDA
        val buttonSalida: Button = findViewById(R.id.button3)
        buttonSalida.text = "Salida"
        buttonSalida.isEnabled = true
        buttonSalida.alpha = 1f
        buttonSalida.setOnClickListener {
            if (yaMarcoAsistenciaHoy(Tipo.SALIDA)) {
                Toast.makeText(this, "La asistencia de hoy (Salida) ya fue registrada.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            currentAction = Tipo.SALIDA
            Log.d(TAG, "CLICK BOTÓN SALIDA → currentAction=SALIDA")
            verificarBiometriaSalida()
        }
    }

    // ================= Biometría =================
    private fun verificarBiometriaIngreso() {
        val bm = BiometricManager.from(this)
        val can = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        when (can) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "Biometría disponible → autenticando (INGRESO)")
                biometricPrompt.authenticate(promptInfoIngreso)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "Sin sensor de huella", Toast.LENGTH_LONG).show()
                habilitarFallback(Tipo.INGRESO)
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Sensor no disponible", Toast.LENGTH_LONG).show()
                habilitarFallback(Tipo.INGRESO)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No hay huellas registradas", Toast.LENGTH_LONG).show()
                habilitarFallback(Tipo.INGRESO)
            }
            else -> {
                Toast.makeText(this, "Biometría no disponible (Ingreso)", Toast.LENGTH_LONG).show()
                habilitarFallback(Tipo.INGRESO)
            }
        }
    }

    private fun verificarBiometriaSalida() {
        val bm = BiometricManager.from(this)
        val can = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        when (can) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "Biometría disponible → autenticando (SALIDA)")
                biometricPrompt.authenticate(promptInfoSalida)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "Sin sensor de huella", Toast.LENGTH_LONG).show()
                habilitarFallback(Tipo.SALIDA)
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Sensor no disponible", Toast.LENGTH_LONG).show()
                habilitarFallback(Tipo.SALIDA)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No hay huellas registradas", Toast.LENGTH_LONG).show()
                habilitarFallback(Tipo.SALIDA)
            }
            else -> {
                Toast.makeText(this, "Biometría no disponible (Salida)", Toast.LENGTH_LONG).show()
                habilitarFallback(Tipo.SALIDA)
            }
        }
    }

    private fun habilitarFallback(tipo: Tipo) {
        ultimoIntentoFallido = tipo
        btnProblemasAsistencia.visibility = View.VISIBLE
        Log.d(TAG, "Habilitando fallback con contraseña para $tipo")
    }

    // ============== Diálogo de contraseña (problemas con asistencia) ==============
    private fun mostrarDialogoPassword() {
        val action = ultimoIntentoFallido
        if (action == null) {
            Toast.makeText(this, "No hay intento de asistencia pendiente.", Toast.LENGTH_SHORT).show()
            return
        }

        // Misma lógica de 1 vez por día
        if (yaMarcoAsistenciaHoy(action)) {
            val texto = if (action == Tipo.INGRESO) "Ingreso" else "Salida"
            Toast.makeText(this, "La asistencia de hoy ($texto) ya fue registrada.", Toast.LENGTH_LONG).show()
            return
        }

        val input = EditText(this).apply {
            hint = "Contraseña"
            // Si quieres ocultar texto:
            // inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val textoAccion = if (action == Tipo.INGRESO) "ingreso" else "salida"

        AlertDialog.Builder(this)
            .setTitle("¿Problemas con registrar asistencia?")
            .setMessage("Ingresa tu contraseña para registrar tu $textoAccion.")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val pass = input.text.toString().trim()
                if (pass.isBlank()) {
                    Toast.makeText(this, "La contraseña no puede estar vacía.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val email = emailUsuario!!.trim()
                when (action) {
                    Tipo.INGRESO -> marcarIngresoManual(email, pass)
                    Tipo.SALIDA -> marcarSalidaManual(email, pass)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ================= API INGRESO (biométrico) =================
    private fun marcarIngresoEnApi(email: String) {
        if (yaMarcoAsistenciaHoy(Tipo.INGRESO)) {
            Toast.makeText(this, "La asistencia de hoy (Ingreso) ya fue registrada.", Toast.LENGTH_LONG).show()
            return
        }

        val jsonBody = JSONObject().apply { put("email", email) }
        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "$asistenciaApiBase/asistencia/ingreso"

        Log.d(TAG, "POST $url  body=$jsonBody")

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = runOnUiThread {
                Log.e(TAG, "FALLÓ ingreso: ${e.message}", e)
                Toast.makeText(applicationContext, "No se pudo marcar el ingreso: ${e.message}", Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val status = response.code
                val resp = response.body?.string()
                Log.d(TAG, "RESPUESTA ingreso: HTTP $status, body=$resp")
                runOnUiThread {
                    if (response.isSuccessful) {
                        val msg = if (status == 201) "Ingreso registrado (nuevo)" else "Ingreso actualizado"
                        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                        marcarHoy(Tipo.INGRESO)
                    } else {
                        val msg = if (status == 409)
                            "La asistencia de hoy ya tiene hora de entrada."
                        else "Error al marcar ingreso ($status)"
                        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                    }
                }
                response.close()
            }
        })
    }

    // ================= API SALIDA (biométrico) =================
    private fun marcarSalidaEnApi(email: String) {
        if (yaMarcoAsistenciaHoy(Tipo.SALIDA)) {
            Toast.makeText(this, "La asistencia de hoy (Salida) ya fue registrada.", Toast.LENGTH_LONG).show()
            return
        }

        val jsonBody = JSONObject().apply { put("email", email) }
        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "$asistenciaApiBase/asistencia/salida"

        Log.d(TAG, "POST $url  body=$jsonBody")

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = runOnUiThread {
                Log.e(TAG, "FALLÓ salida: ${e.message}", e)
                Toast.makeText(applicationContext, "No se pudo marcar la salida: ${e.message}", Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val status = response.code
                val resp = response.body?.string()
                Log.d(TAG, "RESPUESTA salida: HTTP $status, body=$resp")
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Salida marcada", Toast.LENGTH_LONG).show()
                        marcarHoy(Tipo.SALIDA)
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

    // ================= API INGRESO MANUAL (contraseña) =================
    private fun marcarIngresoManual(email: String, password: String) {
        if (yaMarcoAsistenciaHoy(Tipo.INGRESO)) {
            Toast.makeText(this, "La asistencia de hoy (Ingreso) ya fue registrada.", Toast.LENGTH_LONG).show()
            return
        }

        val jsonBody = JSONObject().apply {
            put("email", email)
            put("password", password)    // ajusta al nombre real del campo en tu API
            put("modo", "manual")
        }
        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "$asistenciaApiBase/asistencia/ingreso"

        Log.d(TAG, "POST (MANUAL) $url  body=$jsonBody")

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = runOnUiThread {
                Log.e(TAG, "FALLÓ ingreso manual: ${e.message}", e)
                Toast.makeText(applicationContext, "No se pudo marcar el ingreso manual: ${e.message}", Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val status = response.code
                val resp = response.body?.string()
                Log.d(TAG, "RESPUESTA ingreso manual: HTTP $status, body=$resp")
                runOnUiThread {
                    when (status) {
                        200, 201 -> {
                            Toast.makeText(applicationContext, "Ingreso registrado manualmente", Toast.LENGTH_LONG).show()
                            marcarHoy(Tipo.INGRESO)
                            btnProblemasAsistencia.visibility = View.GONE
                            ultimoIntentoFallido = null
                        }
                        409 -> {
                            Toast.makeText(applicationContext, "La asistencia de hoy ya tiene un ingreso registrado.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(applicationContext, "Error en ingreso manual ($status)", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                response.close()
            }
        })
    }

    // ================= API SALIDA MANUAL (contraseña) =================
    private fun marcarSalidaManual(email: String, password: String) {
        if (yaMarcoAsistenciaHoy(Tipo.SALIDA)) {
            Toast.makeText(this, "La asistencia de hoy (Salida) ya fue registrada.", Toast.LENGTH_LONG).show()
            return
        }

        val jsonBody = JSONObject().apply {
            put("email", email)
            put("password", password)    // ajusta al nombre real del campo
            put("modo", "manual")
        }
        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "$asistenciaApiBase/asistencia/salida"

        Log.d(TAG, "POST (MANUAL) $url  body=$jsonBody")

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = runOnUiThread {
                Log.e(TAG, "FALLÓ salida manual: ${e.message}", e)
                Toast.makeText(applicationContext, "No se pudo marcar la salida manual: ${e.message}", Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val status = response.code
                val resp = response.body?.string()
                Log.d(TAG, "RESPUESTA salida manual: HTTP $status, body=$resp")
                runOnUiThread {
                    when (status) {
                        200, 201 -> {
                            Toast.makeText(applicationContext, "Salida marcada manualmente", Toast.LENGTH_LONG).show()
                            marcarHoy(Tipo.SALIDA)
                            btnProblemasAsistencia.visibility = View.GONE
                            ultimoIntentoFallido = null
                        }
                        404 -> {
                            Toast.makeText(applicationContext, "No existe ingreso hoy para marcar la salida.", Toast.LENGTH_LONG).show()
                        }
                        409 -> {
                            Toast.makeText(applicationContext, "La asistencia de hoy ya tiene salida registrada.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(applicationContext, "Error en salida manual ($status)", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                response.close()
            }
        })
    }
}

