package com.boughazi.tv.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.boughazi.tv.data.AuthRepository
import com.boughazi.tv.data.AuthResult
import com.boughazi.tv.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

/**
 * Registro, inicio de sesión y recuperación de contraseña por Gmail (Supabase Auth).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener { submit(isRegister = false) }
        binding.registerButton.setOnClickListener { submit(isRegister = true) }
        binding.forgotPasswordText.setOnClickListener { sendReset() }
    }

    private fun submit(isRegister: Boolean) {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            binding.statusText.text = "Rellena correo y contraseña"
            return
        }

        lifecycleScope.launch {
            val result = if (isRegister) {
                authRepository.signUp(email, password)
            } else {
                authRepository.signIn(email, password)
            }
            when (result) {
                is AuthResult.Success -> goToChannels()
                is AuthResult.Error -> binding.statusText.text = result.message
            }
        }
    }

    private fun sendReset() {
        val email = binding.emailInput.text.toString().trim()
        if (email.isEmpty()) {
            binding.statusText.text = "Escribe tu correo primero"
            return
        }
        lifecycleScope.launch {
            when (authRepository.sendPasswordReset(email)) {
                is AuthResult.Success -> binding.statusText.text =
                    getString(com.boughazi.tv.R.string.reset_email_sent)
                is AuthResult.Error -> binding.statusText.text =
                    getString(com.boughazi.tv.R.string.error_login_generic)
            }
        }
    }

    private fun goToChannels() {
        startActivity(Intent(this, ChannelListActivity::class.java))
        finish()
    }
}
