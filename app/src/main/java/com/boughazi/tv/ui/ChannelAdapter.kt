package com.boughazi.tv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.boughazi.tv.R
import com.boughazi.tv.data.Channel
import com.boughazi.tv.databinding.ItemChannelBinding

/**
 * Parrilla de canales. El foco (Focus Management) entre celdas lo resuelve
 * GridLayoutManager + el selector de fondo (channel_item_background) que marca
 * la celda enfocada con el mando; aquí solo pintamos el candado si es premium.
 */
class ChannelAdapter(
    private val onChannelSelected: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private var items: List<Channel> = emptyList()

    fun submitList(channels: List<Channel>) {
        items = channels
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(items[position], onChannelSelected)
    }

    override fun getItemCount(): Int = items.size

    class ChannelViewHolder(private val binding: ItemChannelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel, onChannelSelected: (Channel) -> Unit) {
            binding.channelNumber.text = channel.channelNumber.toString().padStart(3, '0')
            binding.channelName.text = channel.name
            binding.channelLock.visibility = if (channel.isPremium) View.VISIBLE else View.GONE

            Glide.with(binding.root)
                .load(channel.logoUrl)
                .placeholder(R.drawable.ic_channel_placeholder)
                .into(binding.channelLogo)

            binding.root.setOnClickListener { onChannelSelected(channel) }
        }
    }
}
