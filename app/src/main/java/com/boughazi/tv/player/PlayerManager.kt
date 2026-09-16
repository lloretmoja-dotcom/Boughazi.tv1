package com.boughazi.tv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView

/**
 * Envuelve ExoPlayer/Media3 con un buffer pensado para directo (streams HLS) y
 * reconexión automática ante microcortes, típicos de IPTV.
 */
class PlayerManager(private val context: Context, private val playerView: PlayerView) {

    private var retryCount = 0
    private val maxRetries = 5
    private var currentUrl: String? = null

    // Buffer generoso para absorber microcortes sin que se note, sin introducir
    // demasiado retraso respecto al directo.
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 15_000,
            /* maxBufferMs = */ 50_000,
            /* bufferForPlaybackMs = */ 2_500,
            /* bufferForPlaybackAfterRebufferMs = */ 5_000
        )
        .build()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(loadControl)
        .setBandwidthMeter(DefaultBandwidthMeter.Builder(context).build())
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context).setDataSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setConnectTimeoutMs(8_000)
                    .setReadTimeoutMs(8_000)
                    .setAllowCrossProtocolRedirects(true)
            )
        )
        .build()
        .also { exo ->
            playerView.player = exo
            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    attemptReconnect()
                }
            })
        }

    fun play(streamUrl: String) {
        retryCount = 0
        currentUrl = streamUrl
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    /** Reconexión automática con backoff simple ante cortes de red o del stream. */
    private fun attemptReconnect() {
        val url = currentUrl ?: return
        if (retryCount >= maxRetries) return
        retryCount++

        val delayMs = 1000L * retryCount
        playerView.postDelayed({
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }, delayMs)
    }

    fun release() {
        player.release()
    }
}
