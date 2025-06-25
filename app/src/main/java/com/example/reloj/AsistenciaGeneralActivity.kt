package com.example.reloj

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.example.reloj.databinding.AsistenciaGeneralBinding

class AsistenciaGeneralActivity : AppCompatActivity() { // Llave de APERTURA de la clase


    private lateinit var binding: AsistenciaGeneralBinding

    override fun onCreate(savedInstanceState: Bundle?) { // Llave de APERTURA del método onCreate
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        binding = AsistenciaGeneralBinding.inflate(layoutInflater)

        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding.imageButton.setOnClickListener {

            finish()
        }

    }

}