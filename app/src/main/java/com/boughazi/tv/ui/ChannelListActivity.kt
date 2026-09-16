package com.boughazi.tv.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.boughazi.tv.data.ChannelRepository
import com.boughazi.tv.databinding.ActivityChannelListBinding
import kotlinx.coroutines.launch

/**
 * Parrilla de canales en directo (estilo decodificador). Descarga la playlist una
 * vez (ChannelRepository la deja en memoria) y arranca PlayerActivity al elegir uno.
 */
class ChannelListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChannelListBinding
    private val channelRepository = ChannelRepository()
    private lateinit var adapter: ChannelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChannelAdapter { channel ->
            startActivity(
                Intent(this, PlayerActivity::class.java)
                    .putExtra(PlayerActivity.EXTRA_CHANNEL_ID, channel.id)
            )
        }
        binding.channelGrid.layoutManager = GridLayoutManager(this, 5)
        binding.channelGrid.adapter = adapter

        lifecycleScope.launch {
            val channels = channelRepository.loadChannels()
            adapter.submitList(channels)
        }
    }
}
