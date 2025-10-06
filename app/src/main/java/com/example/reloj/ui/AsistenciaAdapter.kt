package com.example.reloj.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reloj.R
import com.example.reloj.data.AsistenciaRegistro

class AsistenciaAdapter( // conecta asistencia con recyclerview
    private var items: List<AsistenciaRegistro> = emptyList()
) : RecyclerView.Adapter<AsistenciaAdapter.VH>() {

    fun submitList(newItems: List<AsistenciaRegistro>) { // actualiza la lista de asistencia
        items = newItems
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) { // armado de vista
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvEntrada: TextView = view.findViewById(R.id.tvEntrada)
        val tvSalida: TextView = view.findViewById(R.id.tvSalida)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_asistencia, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) { // formato y visualizacion de la asistencia del trabajador
        val it = items[position]
        holder.tvFecha.text = it.fecha ?: "(sin fecha)"
        holder.tvEntrada.text = "Entrada: ${it.hora_entrada ?: "--:--:--"}"
        holder.tvSalida.text = "Salida: ${it.hora_salida ?: "--:--:--"}"

        val estado = buildString {
            append(if (it.is_asistencia == true) "Asistencia" else "Inasistencia")
            if (it.justificado == true) append(" · Justificado")
            if (!it.mensaje.isNullOrBlank()) append(" · Msg")
        }
        holder.tvEstado.text = estado
    }

    override fun getItemCount(): Int = items.size
}
