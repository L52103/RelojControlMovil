package com.example.reloj // Asegúrate que este sea tu paquete correcto

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
// Importa la clase de View Binding generada para tu layout asistencia_general.xml
import com.example.reloj.databinding.AsistenciaGeneralBinding // Asegúrate que el paquete y nombre de binding sean correctos

class AsistenciaGeneralActivity : AppCompatActivity() { // Llave de APERTURA de la clase

    // Declara una variable para el binding
    private lateinit var binding: AsistenciaGeneralBinding

    override fun onCreate(savedInstanceState: Bundle?) { // Llave de APERTURA del método onCreate
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Mantienes esto si lo necesitas para el edge-to-edge

        // Infla el layout usando View Binding
        binding = AsistenciaGeneralBinding.inflate(layoutInflater)
        // Establece la vista raíz del binding como el contenido de la actividad
        setContentView(binding.root)

        // Aplica los window insets al 'main' ConstraintLayout (o la vista raíz de tu binding)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets -> // Llave de APERTURA de la lambda
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        } // Llave de CIERRE de la lambda para setOnApplyWindowInsetsListener

        // Configurar el OnClickListener para el ImageButton
        // Asumiendo que el ID de tu ImageButton en asistencia_general.xml es 'imageButton'
        binding.imageButton.setOnClickListener { // Llave de APERTURA de la lambda para setOnClickListener
            // Finaliza la actividad actual y regresa a la anterior en la pila
            finish()
        }

    }

}