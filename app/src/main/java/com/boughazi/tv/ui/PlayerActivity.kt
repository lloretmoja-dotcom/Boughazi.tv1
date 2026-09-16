package com.boughazi.tv.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.boughazi.tv.R
import com.boughazi.tv.data.Channel
import com.boughazi.tv.data.ChannelRepository
import com.boughazi.tv.data.PlaybackDecision
import com.boughazi.tv.databinding.ActivityPlayerBinding
import com.boughazi.tv.payments.SubscriptionRepository
import com.boughazi.tv.player.PlayerManager
import com.boughazi.tv.remote.RemoteInputController
import kotlinx.coroutines.launch

/**
 * Pantalla de reproducción a pantalla completa: conecta ChannelRepository (playlist en
 * memoria + verificación premium), PlayerManager (ExoPlayer) y RemoteInputController
 * (zapping, numérico) con el OSD.
 */
class PlayerActivity : AppCompatActivity(), RemoteInputController.Listener {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var playerManager: PlayerManager
    private lateinit var osd: OsdOverlay
    private lateinit var remoteController: RemoteInputController

    private val channelRepository = ChannelRepository()
    private val subscriptionRepository = SubscriptionRepository()

    private var currentChannel: Channel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerManager = PlayerManager(this, binding.playerView)
        osd = OsdOverlay(binding.osdRoot)
        remoteController = RemoteInputController(channelRepository, this)

        val startChannelId = intent.getStringExtra(EXTRA_CHANNEL_ID)

        lifecycleScope.launch {
            channelRepository.loadChannels()
            val start = channelRepository.channels.firstOrNull { it.id == startChannelId }
                ?: channelRepository.channels.firstOrNull()
            start?.let { tuneTo(it) }
        }
    }

    private fun tuneTo(channel: Channel) {
        lifecycleScope.launch {
            val profile = subscriptionRepository.currentProfile()
            when (val decision = channelRepository.resolvePlayback(channel, profile)) {
                is PlaybackDecision.Play -> {
                    currentChannel = channel
                    playerManager.play(channel.streamUrl)
                    osd.show(channel, isLocked = false)
                }
                is PlaybackDecision.RequiresSubscription -> {
                    currentChannel = channel
                    playerManager.player.pause()
                    osd.show(channel, isLocked = true)
                    // La pantalla de suscripción (Visa/PayPal) se abre desde aquí,
                    // ver SubscriptionActivity + StripePaymentProvider/PayPalPaymentProvider.
                    showSubscriptionPrompt(decision)
                }
            }
        }
    }

    private fun showSubscriptionPrompt(decision: PlaybackDecision.RequiresSubscription) {
        startActivity(
            Intent(this, SubscriptionActivity::class.java)
                .putExtra(SubscriptionActivity.EXTRA_CHANNEL_ID, decision.channel.id)
        )
    }

    override fun onChannelChange(channel: Channel) = tuneTo(channel)

    override fun onNumberEntryUpdated(buffer: String) {
        binding.numberEntryOverlay.text = buffer
        binding.numberEntryOverlay.visibility =
            if (buffer.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    override fun onToggleInfo() {
        currentChannel?.let { osd.show(it, isLocked = it.isPremium) }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (remoteController.handleKeyDown(keyCode, currentChannel)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
    }
}
