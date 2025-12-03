package com.example.iptvcpruebadesdecero.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.iptvcpruebadesdecero.R
import com.example.iptvcpruebadesdecero.databinding.ItemChannelListBinding
import com.example.iptvcpruebadesdecero.model.Canal

/**
 * Adaptador para la lista lateral de canales en el reproductor.
 * Muestra número, nombre y logo de cada canal.
 */
class ChannelListAdapter(
    private val channels: List<Canal>,
    private val onChannelClick: (Canal, Int) -> Unit
) : RecyclerView.Adapter<ChannelListAdapter.ChannelViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION
    private var focusedPosition: Int = RecyclerView.NO_POSITION

    inner class ChannelViewHolder(private val binding: ItemChannelListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onChannelClick(channels[position], position)
                }
            }

            itemView.setOnFocusChangeListener { view, hasFocus ->
                val position = adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnFocusChangeListener
                
                if (hasFocus) {
                    val oldFocused = focusedPosition
                    focusedPosition = position
                    // Actualizar solo los items que cambiaron, sin animaciones
                    if (oldFocused != RecyclerView.NO_POSITION && oldFocused != position) {
                        // Actualizar el item anterior sin animación
                        notifyItemChanged(oldFocused, "focus")
                    }
                    // Actualizar el item actual sin animación
                    notifyItemChanged(position, "focus")
                } else {
                    if (focusedPosition == position) {
                        focusedPosition = RecyclerView.NO_POSITION
                        // Actualizar sin animación
                        notifyItemChanged(position, "focus")
                    }
                }
            }

            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER -> {
                            val position = adapterPosition
                            if (position != RecyclerView.NO_POSITION) {
                                onChannelClick(channels[position], position)
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
        }

        fun bind(canal: Canal, position: Int, isFocused: Boolean) {
            // Mostrar número del canal
            binding.tvChannelNumber.text = canal.numero.toString()
            
            // Mostrar nombre del canal
            binding.tvChannelName.text = canal.nombre
            
            // Cargar logo del canal
            if (canal.logo != null && canal.logo.startsWith("asset:///")) {
                val assetPath = canal.logo.removePrefix("asset:///")
                try {
                    val assetManager = itemView.context.assets
                    val inputStream = assetManager.open(assetPath)
                    val drawable = android.graphics.drawable.Drawable.createFromStream(inputStream, null)
                    binding.ivChannelLogo.setImageDrawable(drawable)
                    inputStream.close()
                } catch (e: Exception) {
                    binding.ivChannelLogo.setImageResource(R.drawable.placeholder_channel)
                }
            } else {
                Glide.with(itemView.context)
                    .load(canal.logo)
                    .placeholder(R.drawable.placeholder_channel)
                    .error(R.drawable.placeholder_channel)
                    .timeout(3000)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .override(80, 80)
                    .into(binding.ivChannelLogo)
            }
            
            // Aplicar estilo según si tiene foco
            if (isFocused) {
                binding.root.setBackgroundColor(
                    itemView.context.resources.getColor(R.color.primary_tv, null)
                )
                binding.tvChannelNumber.setTextColor(
                    itemView.context.resources.getColor(R.color.white, null)
                )
                binding.tvChannelName.setTextColor(
                    itemView.context.resources.getColor(R.color.white, null)
                )
            } else {
                binding.root.setBackgroundColor(
                    itemView.context.resources.getColor(R.color.surface_dark, null)
                )
                binding.tvChannelNumber.setTextColor(
                    itemView.context.resources.getColor(R.color.white, null)
                )
                binding.tvChannelName.setTextColor(
                    itemView.context.resources.getColor(android.R.color.white, null)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val isFocused = position == focusedPosition
        holder.bind(channels[position], position, isFocused)
    }
    
    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads[0] == "focus") {
            // Solo actualizar el estado de foco sin re-renderizar todo
            val isFocused = position == focusedPosition
            holder.bind(channels[position], position, isFocused)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount(): Int = channels.size

    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        if (oldPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPosition)
        }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position)
        }
    }

    fun setFocusedPosition(position: Int) {
        val oldPosition = focusedPosition
        focusedPosition = position
        if (oldPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPosition)
        }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position)
        }
    }
}

