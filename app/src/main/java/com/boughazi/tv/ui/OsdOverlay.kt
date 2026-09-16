package com.boughazi.tv.ui

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.boughazi.tv.R
import com.boughazi.tv.data.Channel

/**
 * Capa flotante inferior (OSD) que aparece 3 segundos al cambiar de canal:
 * número, logo, nombre y candado si el canal es premium y no está desbloqueado.
 * `rootView` es el layout del OSD ya incluido en activity_player.xml (incluye
 * ImageView de logo, TextView de número/nombre, e ImageView de candado).
 */
class OsdOverlay(private val rootView: View) {

    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    private val logo: ImageView = rootView.findViewById(R.id.osdLogo)
    private val number: TextView = rootView.findViewById(R.id.osdNumber)
    private val name: TextView = rootView.findViewById(R.id.osdName)
    private val lock: ImageView = rootView.findViewById(R.id.osdLock)

    fun show(channel: Channel, isLocked: Boolean, autoHideMs: Long = 3000L) {
        number.text = channel.channelNumber.toString().padStart(3, '0')
        name.text = channel.name
        lock.visibility = if (isLocked) View.VISIBLE else View.GONE

        Glide.with(rootView)
            .load(channel.logoUrl)
            .placeholder(R.drawable.ic_channel_placeholder)
            .into(logo)

        rootView.visibility = View.VISIBLE
        rootView.animate().alpha(1f).setDuration(150).start()

        hideRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            rootView.animate().alpha(0f).setDuration(200).withEndAction {
                rootView.visibility = View.GONE
            }.start()
        }
        hideRunnable = runnable
        handler.postDelayed(runnable, autoHideMs)
    }

    fun hideImmediately() {
        hideRunnable?.let { handler.removeCallbacks(it) }
        rootView.visibility = View.GONE
    }
}
