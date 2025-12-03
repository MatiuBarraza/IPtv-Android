package com.example.iptvcpruebadesdecero.adapter

// Importaciones necesarias para el funcionamiento del adaptador
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.iptvcpruebadesdecero.R
import com.example.iptvcpruebadesdecero.databinding.ItemCanalBinding
import com.example.iptvcpruebadesdecero.model.Canal

/**
 * Adaptador para mostrar una lista de canales en un RecyclerView.
 * Utiliza DiffUtil para optimizar las actualizaciones de la lista.
 * 
 * @param onCanalClick Función lambda que se ejecuta cuando se hace clic en un canal
 */
class CanalAdapter(
    private val onCanalClick: (List<Canal>, Int) -> Unit
) : ListAdapter<Canal, CanalAdapter.CanalViewHolder>(CanalDiffCallback()) {

    // Variable para mantener la posición del canal seleccionado
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    /**
     * ViewHolder que maneja la vista de cada canal.
     * Contiene la lógica para mostrar el logo, nombre y manejar la selección del canal.
     */
    inner class CanalViewHolder(private val binding: ItemCanalBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        init {
            // Configurar el listener de clic para el item completo
            itemView.setOnClickListener {
                val position = adapterPosition
                // Validar que la posición sea válida
                if (position == RecyclerView.NO_POSITION || position < 0 || position >= currentList.size) {
                    return@setOnClickListener
                }
                
                try {
                    // Obtener la posición anterior seleccionada
                    val previousPosition = selectedPosition
                    // Actualizar la posición seleccionada actual
                    selectedPosition = position
                    
                    // Actualizar visualmente el item anterior (quitar selección)
                    if (previousPosition >= 0 && previousPosition < itemCount) {
                        itemView.post { 
                            try {
                                notifyItemChanged(previousPosition)
                            } catch (e: Exception) {
                                // Ignorar errores de notificación durante reciclaje rápido
                            }
                        }
                    }
                    // Actualizar visualmente el item actual (aplicar selección)
                    if (selectedPosition >= 0 && selectedPosition < itemCount) {
                        itemView.post { 
                            try {
                                notifyItemChanged(selectedPosition)
                            } catch (e: Exception) {
                                // Ignorar errores de notificación durante reciclaje rápido
                            }
                        }
                    }
                    
                    // Ejecutar el callback con la lista de canales y la posición
                    onCanalClick(currentList, position)
                } catch (e: Exception) {
                    // Prevenir crash si hay problemas durante el clic
                    android.util.Log.e("CanalAdapter", "Error en onClick: ${e.message}", e)
                }
            }
            
            // Configurar el listener de cambio de foco para navegación con teclado/control remoto
            itemView.setOnFocusChangeListener { view, hasFocus ->
                val position = adapterPosition
                // Validar que la posición sea válida
                if (position == RecyclerView.NO_POSITION || position < 0 || position >= currentList.size) {
                    return@setOnFocusChangeListener
                }
                
                try {
                    if (hasFocus) {
                        // Obtener la posición anterior seleccionada
                        val previousPosition = selectedPosition
                        // Actualizar la posición seleccionada actual
                        selectedPosition = position
                        
                        // Cancelar animaciones anteriores si existen
                        view.animate().cancel()
                        
                        // Movimiento muy leve y suave (sin vibración)
                        view.animate()
                            .scaleX(1.02f)
                            .scaleY(1.02f)
                            .setDuration(150)
                            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                            .start()
                        
                        // Actualizar visualmente solo los items que cambiaron
                        if (previousPosition >= 0 && previousPosition < itemCount && previousPosition != position) {
                            try {
                                notifyItemChanged(previousPosition, "focus")
                            } catch (e: Exception) {
                                // Ignorar errores durante reciclaje rápido
                            }
                        }
                        try {
                            notifyItemChanged(position, "focus")
                        } catch (e: Exception) {
                            // Ignorar errores durante reciclaje rápido
                        }
                    } else {
                        // Cancelar animaciones anteriores si existen
                        view.animate().cancel()
                        
                        // Movimiento muy leve de vuelta
                        view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                            .start()
                        
                        // Si este item ya no tiene foco, actualizar su estado
                        if (selectedPosition == position) {
                            selectedPosition = RecyclerView.NO_POSITION
                            try {
                                notifyItemChanged(position, "focus")
                            } catch (e: Exception) {
                                // Ignorar errores durante reciclaje rápido
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Prevenir crash si hay problemas durante el cambio de foco
                    android.util.Log.e("CanalAdapter", "Error en onFocusChange: ${e.message}", e)
                }
            }
            
            // Configurar el listener de teclado para manejar el botón central del control remoto
            itemView.setOnKeyListener { view, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    val position = adapterPosition
                    // Validar que la posición sea válida
                    if (position == RecyclerView.NO_POSITION || position < 0 || position >= currentList.size) {
                        return@setOnKeyListener false
                    }
                    
                    try {
                        when (keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                // Ejecutar el clic en el canal seleccionado
                                onCanalClick(currentList, position)
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                // Dejar que el RecyclerView maneje la navegación automáticamente
                                false
                            }
                            else -> false
                        }
                    } catch (e: Exception) {
                        // Prevenir crash si hay problemas durante el key event
                        android.util.Log.e("CanalAdapter", "Error en onKey: ${e.message}", e)
                        false
                    }
                } else {
                    false
                }
            }
        }

        /**
         * Vincula los datos del canal con las vistas del ViewHolder.
         * Configura el nombre del canal, carga el logo y aplica el estilo de selección.
         * 
         * @param canal El canal a mostrar
         */
        fun bind(canal: Canal) {
            try {
                val position = adapterPosition
                // Validar posición antes de usar
                val isValidPosition = position != RecyclerView.NO_POSITION && position >= 0
                
                // Establecer el nombre del canal en el TextView
                binding.tvCanalNombre.text = canal.nombre
                
                // Mostrar número del canal si está disponible
                if (canal.numero > 0) {
                    binding.tvCanalNumero.text = canal.numero.toString()
                    binding.tvCanalNumero.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvCanalNumero.visibility = android.view.View.GONE
                }
                
                // Cargar el logo del canal usando diferentes estrategias
                if (canal.logo != null && canal.logo.startsWith("asset:///")) {
                    // Estrategia 1: Cargar desde assets locales (archivos en la carpeta assets)
                    val assetPath = canal.logo.removePrefix("asset:///")
                    try {
                        val assetManager = itemView.context.assets
                        val inputStream = assetManager.open(assetPath)
                        val drawable = android.graphics.drawable.Drawable.createFromStream(inputStream, null)
                        binding.ivCanalLogo.setImageDrawable(drawable)
                        inputStream.close()
                    } catch (e: Exception) {
                        // Si falla la carga desde assets, usar imagen placeholder
                        binding.ivCanalLogo.setImageResource(R.drawable.placeholder_channel)
                    }
                } else {
                    // Estrategia 2: Cargar desde URL remota usando Glide
                    // Cancelar cualquier carga anterior para evitar conflictos
                    Glide.with(itemView.context).clear(binding.ivCanalLogo)
                    Glide.with(itemView.context)
                        .load(canal.logo)
                        .placeholder(R.drawable.placeholder_channel) // Imagen mientras carga
                        .error(R.drawable.placeholder_channel) // Imagen si falla la carga
                        .timeout(3000) // Timeout de 3 segundos para evitar bloqueos
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // Cache en disco
                        .skipMemoryCache(false) // Usar cache en memoria
                        .override(160, 160) // Limitar tamaño para optimizar rendimiento (80dp * 2)
                        .into(binding.ivCanalLogo)
                }
                
                // Aplicar estilo visual según si el canal está seleccionado o no
                if (isValidPosition) {
                    updateFocusState(position == selectedPosition)
                } else {
                    // Si la posición no es válida, usar estado por defecto
                    updateFocusState(false)
                }
            } catch (e: Exception) {
                // Prevenir crash si hay problemas durante el bind
                android.util.Log.e("CanalAdapter", "Error en bind: ${e.message}", e)
                // Aplicar estado por defecto en caso de error
                updateFocusState(false)
            }
        }
        
        /**
         * Actualiza solo el estado visual de foco sin re-renderizar todo el item.
         */
        fun updateFocusState(isSelected: Boolean) {
            if (isSelected) {
                // Canal seleccionado: fondo con color morado (color principal)
                binding.root.setCardBackgroundColor(itemView.context.resources.getColor(R.color.primary_tv, null))
                binding.tvCanalNombre.setTextColor(itemView.context.resources.getColor(R.color.white, null))
                binding.root.cardElevation = 8f
            } else {
                // Canal no seleccionado: fondo oscuro
                binding.root.setCardBackgroundColor(itemView.context.resources.getColor(R.color.surface_dark, null))
                binding.tvCanalNombre.setTextColor(itemView.context.resources.getColor(R.color.white, null))
                binding.root.cardElevation = 2f
            }
        }
    }

    /**
     * Crea nuevas instancias de ViewHolder.
     * Se llama automáticamente por el RecyclerView cuando necesita crear nuevos ViewHolders.
     * 
     * @param parent El ViewGroup padre donde se inflará la vista
     * @param viewType El tipo de vista (no usado en este adaptador)
     * @return Una nueva instancia de CanalViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CanalViewHolder {
        // Inflar el layout del item usando ViewBinding
        val binding = ItemCanalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CanalViewHolder(binding)
    }

    /**
     * Vincula los datos del canal con el ViewHolder en la posición especificada.
     * Se llama automáticamente por el RecyclerView para mostrar los datos.
     * 
     * @param holder El ViewHolder a vincular
     * @param position La posición del elemento en la lista
     */
    override fun onBindViewHolder(holder: CanalViewHolder, position: Int) {
        try {
            // Validar que la posición sea válida
            if (position >= 0 && position < currentList.size) {
                holder.bind(getItem(position))
            }
        } catch (e: Exception) {
            android.util.Log.e("CanalAdapter", "Error en onBindViewHolder: ${e.message}", e)
        }
    }
    
    override fun onBindViewHolder(holder: CanalViewHolder, position: Int, payloads: MutableList<Any>) {
        try {
            // Validar que la posición sea válida
            if (position < 0 || position >= currentList.size) {
                return
            }
            
            if (payloads.isNotEmpty() && payloads[0] == "focus") {
                // Solo actualizar el estado de foco sin re-renderizar todo
                holder.updateFocusState(position == selectedPosition)
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        } catch (e: Exception) {
            android.util.Log.e("CanalAdapter", "Error en onBindViewHolder con payloads: ${e.message}", e)
        }
    }

    /**
     * Método para actualizar la lista de canales.
     * Resetea la posición seleccionada cuando se actualiza la lista.
     * 
     * @param list Nueva lista de canales
     */
    override fun submitList(list: List<Canal>?) {
        try {
            // Resetear la posición seleccionada cuando se actualiza la lista
            selectedPosition = RecyclerView.NO_POSITION
            super.submitList(list)
        } catch (e: Exception) {
            android.util.Log.e("CanalAdapter", "Error en submitList: ${e.message}", e)
            // Intentar actualizar de forma segura
            try {
                selectedPosition = RecyclerView.NO_POSITION
                super.submitList(list)
            } catch (e2: Exception) {
                android.util.Log.e("CanalAdapter", "Error crítico en submitList: ${e2.message}", e2)
            }
        }
    }
}

/**
 * DiffUtil.ItemCallback para optimizar las actualizaciones del RecyclerView.
 * Compara canales para determinar si deben ser redibujados, evitando actualizaciones innecesarias.
 */
class CanalDiffCallback : DiffUtil.ItemCallback<Canal>() {
    
    /**
     * Determina si dos items representan el mismo canal.
     * Se basa en el ID del canal para la comparación.
     * 
     * @param oldItem El canal anterior
     * @param newItem El canal nuevo
     * @return true si representan el mismo canal
     */
    override fun areItemsTheSame(oldItem: Canal, newItem: Canal): Boolean {
        return oldItem.id == newItem.id
    }

    /**
     * Determina si el contenido de dos canales es el mismo.
     * Se compara toda la información del canal.
     * 
     * @param oldItem El canal anterior
     * @param newItem El canal nuevo
     * @return true si el contenido es el mismo
     */
    override fun areContentsTheSame(oldItem: Canal, newItem: Canal): Boolean {
        return oldItem == newItem
    }
} 