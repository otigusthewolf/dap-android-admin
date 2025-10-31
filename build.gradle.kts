// File: build.gradle.kts (Livello di Progetto)
// Questo file definisce DOVE trovare i plugin
plugins {
    // Registra il plugin per l'applicazione Android
    id("com.android.application") version "8.13.0" apply false
    // Registra il plugin per Kotlin su Android
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

