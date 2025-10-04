package com.example.reloj

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reloj.data.ApiAsistenciaClient
import com.example.reloj.data.AsistenciaRegistro
import com.example.reloj.data.ListadoAsistenciaResponse
import com.example.reloj.databinding.AsistenciaGeneralBinding
import com.example.reloj.ui.AsistenciaAdapter
import kotlinx.coroutines.launch

class AsistenciaGeneralActivity : AppCompatActivity() {

    private lateinit var binding: AsistenciaGeneralBinding
    private lateinit var adapter: AsistenciaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = AsistenciaGeneralBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón back
        binding.imageButton.setOnClickListener { finish() }

        // RecyclerView
        adapter = AsistenciaAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AsistenciaGeneralActivity)
            adapter = this@AsistenciaGeneralActivity.adapter
        }

        // Cargar email desde SharedPreferences
        val email = getSharedPreferences("MiAppPrefs", MODE_PRIVATE)
            .getString("EMAIL", null)

        if (email.isNullOrBlank()) {
            Toast.makeText(this, "Falta email. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
            return
        }

        cargarAsistencias(email)
    }

    private fun cargarAsistencias(email: String) {
        lifecycleScope.launch {
            try {
                val resp = ApiAsistenciaClient.service.listarAsistenciaPorEmail(email)
                if (resp.isSuccessful) {
                    val data: ListadoAsistenciaResponse? = resp.body()
                    val registros: List<AsistenciaRegistro> = data?.registros ?: emptyList()
                    adapter.submitList(registros)
                    if (registros.isEmpty()) {
                        Toast.makeText(this@AsistenciaGeneralActivity, "Sin asistencias registradas.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AsistenciaGeneralActivity, "No se pudo cargar asistencias (${resp.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AsistenciaGeneralActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
