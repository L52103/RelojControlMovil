plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp) // <-- AÑADE ESTA LÍNEA
    kotlin("plugin.serialization") version "2.1.21" // Ajusta esta versión si es necesario
}

android {
    namespace = "com.example.reloj"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.reloj"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)



    dependencies {
        // Otras dependencias...

        // Retrofit para manejar las peticiones HTTP
        implementation ("com.squareup.retrofit2:retrofit:2.9.0")

        // Convertidor JSON con Gson
        implementation ("com.squareup.retrofit2:converter-gson:2.9.0")


            // Dependencias de Ktor Client
            implementation ("io.ktor:ktor-client-core:3.1.3")
            implementation ("io.ktor:ktor-client-cio:2.3.2")
            implementation ("io.ktor:ktor-client-content-negotiation:2.3.2")
            implementation ("io.ktor:ktor-serialization-kotlinx-json:2.3.2")

            // Dependencia para kotlinx.serialization JSON
            implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

            // Dependencia para corrutinas en Android (si aún no la tienes)
            implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

            // Otras dependencias de tu proyecto (por ejemplo, androidx, material design, etc.)
            implementation ("androidx.core:core-ktx:1.10.1")
            implementation ("androidx.appcompat:appcompat:1.6.1")
            implementation ("com.google.android.material:material:1.9.0")

    }



}