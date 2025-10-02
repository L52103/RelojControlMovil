package com.example.reloj

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reloj.ui.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AyudaActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var option1Button: Button
    private lateinit var option2Button: Button
    private lateinit var optionsLayout: View

    private val messages = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter

    // Usaremos SOLO el email (como en el curl)
    private var emailIntent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ayuda)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        inputMessage = findViewById(R.id.inputMessage)
        sendButton = findViewById(R.id.sendButton)
        option1Button = findViewById(R.id.option1Button)
        option2Button = findViewById(R.id.option2Button)
        optionsLayout = findViewById(R.id.optionsLayout)

        adapter = ChatAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = adapter

        // 1) Leer email por Intent (si lo enviaron)
        emailIntent = intent.getStringExtra("EMAIL")

        // 2) Fallback: leer email desde SharedPreferences guardado en LoginActivity
        if (emailIntent.isNullOrBlank()) {
            val prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE)
            emailIntent = prefs.getString("email", null)
        }

        // Deshabilitar input hasta que el usuario elija opción inicial
        sendButton.isEnabled = false
        inputMessage.isEnabled = false

        simularBotEscribiendo {
            mostrarOpciones()
        }

        option1Button.setOnClickListener {
            enviarMensajeUsuario(option1Button.text.toString())
        }

        option2Button.setOnClickListener {
            enviarMensajeUsuario(option2Button.text.toString())
        }

        sendButton.setOnClickListener {
            val texto = inputMessage.text.toString().trim()
            if (!TextUtils.isEmpty(texto)) {
                enviarMensajeUsuario(texto)
                inputMessage.setText("")
            }
        }
    }

    private fun simularBotEscribiendo(onFinish: () -> Unit) {
        agregarMensaje(Message("Bot está escribiendo...", isBot = true, isTyping = true))
        chatRecyclerView.postDelayed({
            quitarUltimoTyping()
            onFinish()
        }, 1200)
    }

    private fun mostrarOpciones() {
        optionsLayout.visibility = View.VISIBLE
    }

    private fun enviarMensajeUsuario(texto: String) {
        agregarMensaje(Message(texto, isBot = false))

        if (optionsLayout.visibility == View.VISIBLE) {
            // Primera interacción: habilita el input libre
            optionsLayout.visibility = View.GONE
            setInputEnabled(true)
            simularBotRespuesta("Por favor, escribe tu mensaje")
        } else {
            // Validación: debemos tener EMAIL sí o sí
            if (emailIntent.isNullOrBlank()) {
                agregarMensaje(
                    Message(
                        "⚠️ No hay email del trabajador. Inicia sesión nuevamente.",
                        isBot = true
                    )
                )
                return
            }

            val typingIdx = agregarTyping()
            setInputEnabled(false)

            lifecycleScope.launch {
                try {
                    // Llamada real al backend usando SOLO email (como en el curl)
                    val resp = withContext(Dispatchers.IO) {
                        ApiClient.enviarMensajeAsistencia(
                            mensaje = texto,
                            trabajadorId = null,
                            email = emailIntent,
                            rut = null
                        )
                    }

                    quitarTyping(typingIdx)
                    val msgOk = resp.message.ifBlank {
                        "Mensaje registrado con éxito. El administrador revisará tu caso."
                    }
                    agregarMensaje(Message("✅ $msgOk", isBot = true))
                } catch (e: Exception) {
                    quitarTyping(typingIdx)
                    agregarMensaje(
                        Message(
                            "⚠️ Error de red. ${e.message.orEmpty()}",
                            isBot = true
                        )
                    )
                } finally {
                    setInputEnabled(true)
                }
            }
        }
    }

    private fun simularBotRespuesta(texto: String) {
        val typingIdx = agregarTyping()
        chatRecyclerView.postDelayed({
            quitarTyping(typingIdx)
            agregarMensaje(Message(texto, isBot = true))
        }, 1200)
    }

    private fun agregarMensaje(mensaje: Message) {
        messages.add(mensaje)
        adapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun agregarTyping(): Int {
        val msg = Message("Bot está escribiendo...", isBot = true, isTyping = true)
        messages.add(msg)
        val idx = messages.size - 1
        adapter.notifyItemInserted(idx)
        chatRecyclerView.scrollToPosition(idx)
        return idx
    }

    private fun quitarTyping(index: Int) {
        if (index in messages.indices && messages[index].isTyping) {
            messages.removeAt(index)
            adapter.notifyItemRemoved(index)
        } else {
            quitarUltimoTyping()
        }
    }

    private fun quitarUltimoTyping() {
        val last = messages.indexOfLast { it.isTyping }
        if (last != -1) {
            messages.removeAt(last)
            adapter.notifyItemRemoved(last)
        }
    }

    private fun setInputEnabled(enabled: Boolean) {
        inputMessage.isEnabled = enabled
        sendButton.isEnabled = enabled
        option1Button.isEnabled = enabled
        option2Button.isEnabled = enabled
    }
}

// Modelo simple para el chat
data class Message(
    val text: String,
    val isBot: Boolean,
    val isTyping: Boolean = false
)
