package com.example.iptvcpruebadesdecero.adapter

// Importaciones necesarias para el funcionamiento del adaptador
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.iptvcpruebadesdecero.databinding.ItemCategoriaBinding
import com.example.iptvcpruebadesdecero.model.Categoria
import com.example.iptvcpruebadesdecero.model.Canal

/**
 * Adaptador para mostrar una lista de categorías en un RecyclerView.
 * Cada categoría contiene su propia lista horizontal de canales.
 * Este adaptador crea una vista jerárquica donde cada categoría es una fila
 * que contiene un RecyclerView horizontal con los canales de esa categoría.
 * 
 * @param categorias Lista de categorías a mostrar
 * @param onCanalClick Función lambda que se ejecuta cuando se hace clic en un canal
 */
class CategoriaAdapter(
    private val categorias: List<Categoria>,
    private val onCanalClick: (List<Canal>, Int) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    /**
     * ViewHolder que maneja la vista de cada categoría.
     * Contiene un RecyclerView horizontal para mostrar los canales de la categoría.
     * Cada ViewHolder tiene su propio CanalAdapter para manejar la lista horizontal de canales.
     */
    inner class CategoriaViewHolder(private val binding: ItemCategoriaBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        // Adaptador para la lista horizontal de canales dentro de esta categoría
        // Cada ViewHolder tiene su propio adaptador para manejar los canales de su categoría
        private val canalAdapter = CanalAdapter { canales, position ->
            // Callback que se ejecuta cuando se hace clic en un canal
            // Pasa la lista de canales de esta categoría y la posición del canal clickeado
            onCanalClick(canales, position)
        }

        // Inicialización del RecyclerView de canales
        init {
            // Configurar el RecyclerView horizontal que contiene los canales de esta categoría
            binding.recyclerViewCanales.apply {
                // LayoutManager horizontal para mostrar los canales en fila
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                // Asignar el adaptador de canales
                adapter = canalAdapter
                // Configurar para recibir foco y manejar navegación con teclado/control remoto
                isFocusable = true
                isFocusableInTouchMode = true
                // Configurar para manejar navegación con teclado
                descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
            }
        }

        /**
         * Vincula los datos de la categoría con las vistas del ViewHolder.
         * Configura el título de la categoría y actualiza la lista de canales.
         * 
         * @param categoria La categoría a mostrar
         */
        fun bind(categoria: Categoria) {
            // Establecer el título de la categoría en el TextView
            binding.tvCategoriaTitulo.text = categoria.nombre
            
            // Actualizar la lista de canales en el adaptador horizontal
            // Esto hará que el RecyclerView horizontal se actualice con los nuevos canales
            canalAdapter.submitList(categoria.canales)
        }
    }

    /**
     * Crea nuevas instancias de ViewHolder.
     * Se llama automáticamente por el RecyclerView cuando necesita crear nuevos ViewHolders.
     * 
     * @param parent El ViewGroup padre donde se inflará la vista
     * @param viewType El tipo de vista (no usado en este adaptador)
     * @return Una nueva instancia de CategoriaViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        // Inflar el layout del item de categoría usando ViewBinding
        val binding = ItemCategoriaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoriaViewHolder(binding)
    }

    /**
     * Vincula los datos de la categoría con el ViewHolder en la posición especificada.
     * Se llama automáticamente por el RecyclerView para mostrar los datos.
     * 
     * @param holder El ViewHolder a vincular
     * @param position La posición del elemento en la lista
     */
    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        // Obtener la categoría en la posición especificada y vincularla al ViewHolder
        holder.bind(categorias[position])
    }

    /**
     * Retorna el número total de categorías en la lista.
     * Este método es requerido por RecyclerView.Adapter.
     * 
     * @return El tamaño de la lista de categorías
     */
    override fun getItemCount() = categorias.size

    /**
     * Obtiene la lista actual de categorías.
     * Útil para acceder a los datos desde fuera del adaptador.
     * 
     * @return La lista actual de categorías
     */
    fun getCategorias(): List<Categoria> = categorias
} 