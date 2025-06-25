package com.example.reloj

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AyudaActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var option1Button: Button
    private lateinit var option2Button: Button
    private lateinit var optionsLayout: View

    private val messages = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter

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
        Handler(Looper.getMainLooper()).postDelayed({
            val index = messages.indexOfLast { it.isTyping }
            if (index != -1) {
                messages.removeAt(index)
                adapter.notifyItemRemoved(index)
            }
            onFinish()
        }, 1500)
    }

    private fun mostrarOpciones() {
        optionsLayout.visibility = View.VISIBLE
    }

    private fun enviarMensajeUsuario(texto: String) {
        agregarMensaje(Message(texto, isBot = false))

        if (optionsLayout.visibility == View.VISIBLE) {
            optionsLayout.visibility = View.GONE
            inputMessage.isEnabled = true
            sendButton.isEnabled = true


            simularBotRespuesta("Por favor, escribe tu mensaje")
        } else {
            // Despues de escribir el mensaje, bot confirma recepción
            simularBotRespuesta("✅ Respuesta registrada con éxito, se revisará su caso en la brevedad.")
        }
    }


    private fun simularBotRespuesta(texto: String) {
        agregarMensaje(Message("Bot está escribiendo...", isBot = true, isTyping = true))
        Handler(Looper.getMainLooper()).postDelayed({
            val index = messages.indexOfLast { it.isTyping }
            if (index != -1) {
                messages.removeAt(index)
                adapter.notifyItemRemoved(index)
            }
            agregarMensaje(Message(texto, isBot = true))
        }, 1500)
    }

    private fun agregarMensaje(mensaje: Message) {
        messages.add(mensaje)
        adapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)
    }
}

data class Message(
    val text: String,
    val isBot: Boolean,
    val isTyping: Boolean = false
)
