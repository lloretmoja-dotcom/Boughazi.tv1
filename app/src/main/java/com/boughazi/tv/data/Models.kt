package com.boughazi.tv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa una fila de la tabla `channels` en Supabase.
 * channel_number se usa para el salto directo por teclado numérico del mando.
 */
@Serializable
data class Channel(
    val id: String,
    @SerialName("channel_number") val channelNumber: Int,
    val name: String,
    val category: String,
    @SerialName("logo_url") val logoUrl: String?,
    @SerialName("stream_url") val streamUrl: String,
    @SerialName("is_premium") val isPremium: Boolean = false
)

/**
 * Representa una fila de la tabla `profiles`, vinculada 1:1 con el usuario de Supabase Auth.
 */
@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    @SerialName("is_subscribed") val isSubscribed: Boolean = false,
    @SerialName("subscription_provider") val subscriptionProvider: String? = null, // "stripe" | "paypal"
    @SerialName("subscription_expires_at") val subscriptionExpiresAt: String? = null
)

sealed class PlaybackDecision {
    data class Play(val channel: Channel) : PlaybackDecision()
    data class RequiresSubscription(val channel: Channel) : PlaybackDecision()
}

sealed class AuthResult {
    data class Success(val userId: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
