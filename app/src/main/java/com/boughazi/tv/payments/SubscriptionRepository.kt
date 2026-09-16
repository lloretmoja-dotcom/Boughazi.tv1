package com.boughazi.tv.payments

import com.boughazi.tv.data.SupabaseModule
import com.boughazi.tv.data.UserProfile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from

/**
 * Punto único donde confirmamos, ante Supabase, que un pago (Visa o PayPal) se completó.
 * En producción esto debería verificarse también en el servidor (Edge Function /
 * webhook de Stripe y PayPal) para que nadie pueda "desbloquearse" manipulando la app;
 * este método cliente sirve para reflejar el estado al instante en la UI mientras
 * el webhook confirma en segundo plano.
 */
class SubscriptionRepository(
    private val client = SupabaseModule.client
) {

    suspend fun activateSubscription(provider: String, reference: String): Result<UserProfile> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("No hay sesión activa")

        client.from("profiles").update(
            {
                set("is_subscribed", true)
                set("subscription_provider", provider)
            }
        ) {
            filter { eq("id", userId) }
        }

        client.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeSingle()
    }

    suspend fun currentProfile(): UserProfile? {
        val userId = client.auth.currentUserOrNull()?.id ?: return null
        return client.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull()
    }
}
