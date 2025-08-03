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
                // Obtener la posición anterior seleccionada
                val previousPosition = selectedPosition
                // Actualizar la posición seleccionada actual
                selectedPosition = adapterPosition
                
                // Actualizar visualmente el item anterior (quitar selección)
                if (previousPosition >= 0 && previousPosition < itemCount) {
                    itemView.post { notifyItemChanged(previousPosition) }
                }
                // Actualizar visualmente el item actual (aplicar selección)
                if (selectedPosition >= 0 && selectedPosition < itemCount) {
                    itemView.post { notifyItemChanged(selectedPosition) }
                }
                
                // Ejecutar el callback con la lista de canales y la posición
                onCanalClick(currentList, adapterPosition)
            }
            
            // Configurar el listener de cambio de foco para navegación con teclado/control remoto
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    // Obtener la posición anterior seleccionada
                    val previousPosition = selectedPosition
                    // Actualizar la posición seleccionada actual
                    selectedPosition = adapterPosition
                    
                    // Actualizar visualmente el item anterior (quitar selección)
                    if (previousPosition >= 0 && previousPosition < itemCount) {
                        itemView.post { notifyItemChanged(previousPosition) }
                    }
                    // Actualizar visualmente el item actual (aplicar selección)
                    if (selectedPosition >= 0 && selectedPosition < itemCount) {
                        itemView.post { notifyItemChanged(selectedPosition) }
                    }
                }
            }
            
            // Configurar el listener de teclado para manejar el botón central del control remoto
            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER -> {
                            // Ejecutar el clic en el canal seleccionado
                            onCanalClick(currentList, adapterPosition)
                            true
                        }
                        else -> false
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
            // Establecer el nombre del canal en el TextView
            binding.tvCanalNombre.text = canal.nombre
            
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
                Glide.with(itemView.context)
                    .load(canal.logo)
                    .placeholder(R.drawable.placeholder_channel) // Imagen mientras carga
                    .error(R.drawable.placeholder_channel) // Imagen si falla la carga
                    .timeout(3000) // Timeout de 3 segundos para evitar bloqueos
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // Cache en disco
                    .skipMemoryCache(false) // Usar cache en memoria
                    .override(120, 120) // Limitar tamaño para optimizar rendimiento
                    .into(binding.ivCanalLogo)
            }
            
            // Aplicar estilo visual según si el canal está seleccionado o no
            if (adapterPosition == selectedPosition) {
                // Canal seleccionado: fondo gris
                binding.root.setCardBackgroundColor(0xFFB0B0B0.toInt())
            } else {
                // Canal no seleccionado: fondo blanco
                binding.root.setCardBackgroundColor(0xFFFFFFFF.toInt())
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
        holder.bind(getItem(position))
    }

    /**
     * Método para actualizar la lista de canales.
     * Resetea la posición seleccionada cuando se actualiza la lista.
     * 
     * @param list Nueva lista de canales
     */
    override fun submitList(list: List<Canal>?) {
        // Resetear la posición seleccionada cuando se actualiza la lista
        selectedPosition = RecyclerView.NO_POSITION
        super.submitList(list)
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