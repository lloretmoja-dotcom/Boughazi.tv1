package com.boughazi.tv.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/**
 * Motor de playlist: descarga los canales UNA vez al iniciar sesión y los mantiene
 * en un array en memoria RAM (`cachedChannels`) para que el zapping y la navegación
 * por categorías sean instantáneos, sin volver a golpear la red en cada cambio de canal.
 */
class ChannelRepository(
    private val client = SupabaseModule.client
) {
    // Array en memoria: fuente de verdad mientras la app está abierta.
    private var cachedChannels: List<Channel> = emptyList()

    val channels: List<Channel>
        get() = cachedChannels

    suspend fun loadChannels(): List<Channel> {
        val result = client.from("channels")
            .select() {
                order("channel_number", Order.ASCENDING)
            }
            .decodeList<Channel>()
        cachedChannels = result
        return result
    }

    fun byCategory(category: String): List<Channel> =
        cachedChannels.filter { it.category == category }

    fun byNumber(number: Int): Channel? =
        cachedChannels.firstOrNull { it.channelNumber == number }

    fun next(current: Channel): Channel? {
        val sorted = cachedChannels.sortedBy { it.channelNumber }
        val idx = sorted.indexOfFirst { it.id == current.id }
        if (idx == -1 || sorted.isEmpty()) return null
        return sorted[(idx + 1) % sorted.size]
    }

    fun previous(current: Channel): Channel? {
        val sorted = cachedChannels.sortedBy { it.channelNumber }
        val idx = sorted.indexOfFirst { it.id == current.id }
        if (idx == -1 || sorted.isEmpty()) return null
        return sorted[(idx - 1 + sorted.size) % sorted.size]
    }

    /**
     * Decide si un canal se puede reproducir o si hay que mostrar el aviso de pago.
     * Es la única puerta de entrada a la reproducción: todo pasa por aquí.
     */
    fun resolvePlayback(channel: Channel, profile: UserProfile?): PlaybackDecision {
        val subscribed = profile?.isSubscribed == true
        return if (channel.isPremium && !subscribed) {
            PlaybackDecision.RequiresSubscription(channel)
        } else {
            PlaybackDecision.Play(channel)
        }
    }
}
