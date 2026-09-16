package com.boughazi.tv.remote

import android.view.KeyEvent
import com.boughazi.tv.data.Channel
import com.boughazi.tv.data.ChannelRepository

/**
 * Traduce las teclas del mando a distancia en acciones de la app:
 *  - CH+ / CH- (o D-pad arriba/abajo en pantalla completa): zapping secuencial.
 *  - Teclas numéricas 0-9: entrada directa de channel_number (con buffer y timeout).
 *  - El foco (Focus Management) de categorías/parrilla lo gestionan las propias vistas
 *    con nextFocusUp/Down/Left/Right; este controlador solo entra en juego durante
 *    la reproducción a pantalla completa.
 */
class RemoteInputController(
    private val channelRepository: ChannelRepository,
    private val listener: Listener
) {
    interface Listener {
        fun onChannelChange(channel: Channel)
        fun onNumberEntryUpdated(buffer: String)
        fun onToggleInfo()
    }

    private var numberBuffer: String = ""
    private var lastDigitAtMs: Long = 0L
    private val numberEntryTimeoutMs = 2500L

    fun handleKeyDown(keyCode: Int, currentChannel: Channel?): Boolean {
        val channel = currentChannel ?: return false

        return when (keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                channelRepository.next(channel)?.let { listener.onChannelChange(it) }
                true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                channelRepository.previous(channel)?.let { listener.onChannelChange(it) }
                true
            }
            KeyEvent.KEYCODE_INFO -> {
                listener.onToggleInfo()
                true
            }
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                onDigit(('0' + (keyCode - KeyEvent.KEYCODE_0)))
                true
            }
            else -> false
        }
    }

    private fun onDigit(digit: Char) {
        val now = System.currentTimeMillis()
        if (now - lastDigitAtMs > numberEntryTimeoutMs) numberBuffer = ""
        lastDigitAtMs = now

        numberBuffer += digit
        if (numberBuffer.length > 4) numberBuffer = numberBuffer.takeLast(4)
        listener.onNumberEntryUpdated(numberBuffer)

        val target = channelRepository.byNumber(numberBuffer.toIntOrNull() ?: -1)
        if (target != null) {
            listener.onChannelChange(target)
            numberBuffer = ""
        }
    }
}
