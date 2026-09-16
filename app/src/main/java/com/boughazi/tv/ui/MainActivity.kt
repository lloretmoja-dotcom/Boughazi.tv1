package com.boughazi.tv.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.boughazi.tv.data.AuthRepository

/**
 * Punto de entrada: si ya hay sesión de Supabase guardada, va directo a la parrilla
 * de canales; si no, pide login. Así el usuario no tiene que iniciar sesión cada vez
 * que enciende la TV.
 */
class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destination = if (authRepository.currentUser != null) {
            ChannelListActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }
}
