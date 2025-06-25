package com.example.reloj  // Cambia esto por tu paquete real
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_BOT = 0
        private const val TYPE_USER = 1
        private const val TYPE_TYPING = 2
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when {
            msg.isTyping -> TYPE_TYPING
            msg.isBot -> TYPE_BOT
            else -> TYPE_USER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_BOT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_bot, parent, false)
                BotViewHolder(view)
            }
            TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_user, parent, false)
                UserViewHolder(view)
            }
            TYPE_TYPING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_typing, parent, false)
                TypingViewHolder(view)
            }
            else -> throw IllegalArgumentException("Tipo desconocido")
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is BotViewHolder -> holder.bind(msg)
            is UserViewHolder -> holder.bind(msg)
            is TypingViewHolder -> holder.bind()
        }
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textBotMessage)
        fun bind(msg: Message) {
            textView.text = msg.text
        }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textUserMessage)
        fun bind(msg: Message) {
            textView.text = msg.text
        }
    }

    class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // animacion "escribiendo..."
        fun bind() {

        }
    }
}
