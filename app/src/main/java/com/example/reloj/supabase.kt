package com.example.reloj

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://qubqttdztpvwuzismrpa.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InF1YnF0dGR6dHB2d3V6aXNtcnBhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDk0MTE0OTksImV4cCI6MjA2NDk4NzQ5OX0.UKMk_TyQBWuOC561pO1xOaacakBGM0gSDhwC7gL3xNc"
) {
    install(Postgrest)
}