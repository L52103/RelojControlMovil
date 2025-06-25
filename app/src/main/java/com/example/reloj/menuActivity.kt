package com.example.reloj

import android.content.Intent
import android.os.Bundle
import com.google.android.material.chip.Chip
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class menuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // === Manejo de los Chips ===
        val chipRegistrar = findViewById<Chip>(R.id.chip)
        val chipAsistenciaGeneral = findViewById<Chip>(R.id.chip3)
        val chipTareas = findViewById<Chip>(R.id.chip4)
        val chipAyudas = findViewById<Chip>(R.id.chip5)


        chipRegistrar.setOnClickListener {
            startActivity(Intent(this, RegistroAsistenciaActivity::class.java))
        }

        chipAsistenciaGeneral.setOnClickListener {
            startActivity(Intent(this, AsistenciaGeneralActivity::class.java))
        }

        chipTareas.setOnClickListener {
            startActivity(Intent(this, TareasActivity::class.java))
        }

        chipAyudas.setOnClickListener {
            startActivity(Intent(this, AyudaActivity::class.java))
        }




        // === Manejo del BottomNavigationView ===
        val navView = findViewById<BottomNavigationView>(R.id.nav_view)

        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_asistencia -> {
                    startActivity(Intent(this, AsistenciaGeneralActivity::class.java))
                    true
                }
                R.id.navigation_tareas -> {
                    startActivity(Intent(this, TareasActivity::class.java))
                    true
                }
                R.id.navigation_ayuda -> {
                    startActivity(Intent(this, AyudaActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
