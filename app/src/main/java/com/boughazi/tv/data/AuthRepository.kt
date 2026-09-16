package com.boughazi.tv.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo

/**
 * Autenticación por Gmail (correo + contraseña) y recuperación de cuenta.
 * Todo el flujo se apoya en Supabase Auth: no guardamos contraseñas nosotros mismos.
 */
class AuthRepository(
    private val client = SupabaseModule.client
) {

    /** Sesión actual, o null si el usuario no ha iniciado sesión. */
    val currentUser: UserInfo?
        get() = client.auth.currentUserOrNull()

    suspend fun signUp(email: String, password: String): AuthResult = try {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = client.auth.currentUserOrNull()?.id
            ?: return AuthResult.Error("No se pudo crear la cuenta")
        AuthResult.Success(userId)
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Error al registrar la cuenta")
    }

    suspend fun signIn(email: String, password: String): AuthResult = try {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = client.auth.currentUserOrNull()?.id
            ?: return AuthResult.Error("Credenciales incorrectas")
        AuthResult.Success(userId)
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "No se pudo iniciar sesión")
    }

    /**
     * "Olvidé mi contraseña": Supabase envía un correo a la cuenta de Gmail del usuario
     * con un enlace de recuperación que abre la app vía deep link (redirectUrl).
     */
    suspend fun sendPasswordReset(email: String): AuthResult = try {
        client.auth.resetPasswordForEmail(
            email = email,
            redirectUrl = "boughazitv://reset-password"
        )
        AuthResult.Success(userId = "")
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "No se pudo enviar el correo de recuperación")
    }

    /** Se llama desde la pantalla que abre el deep link boughazitv://reset-password */
    suspend fun updatePassword(newPassword: String): AuthResult = try {
        client.auth.modifyUser {
            password = newPassword
        }
        AuthResult.Success(userId = currentUser?.id ?: "")
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "No se pudo actualizar la contraseña")
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}
