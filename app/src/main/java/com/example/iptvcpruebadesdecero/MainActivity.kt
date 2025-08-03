package com.example.iptvcpruebadesdecero

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.iptvcpruebadesdecero.adapter.CategoriaAdapter
import com.example.iptvcpruebadesdecero.adapter.CanalAdapter
import com.example.iptvcpruebadesdecero.databinding.ActivityMainBinding
import com.example.iptvcpruebadesdecero.model.Canal
import com.example.iptvcpruebadesdecero.model.Categoria
import com.example.iptvcpruebadesdecero.viewmodel.MainViewModel
import com.google.android.material.textfield.TextInputEditText

/**
 * Actividad principal de la aplicación que muestra la lista de categorías y canales IPTV.
 * Implementa la interfaz de usuario principal y maneja la interacción con el ViewModel.
 * 
 * Funcionalidades principales:
 * - Mostrar categorías de canales en formato de lista vertical
 * - Cada categoría contiene canales en formato horizontal
 * - Búsqueda de canales en tiempo real
 * - Navegación al reproductor de video
 * - Carga y parseo de playlist M3U
 */
class MainActivity : AppCompatActivity() {
    // ViewBinding para acceder a las vistas de manera segura
    private lateinit var binding: ActivityMainBinding
    // ViewModel para manejar la lógica de negocio y persistencia de datos
    private lateinit var viewModel: MainViewModel
    // Adaptador para mostrar las categorías y sus canales
    private lateinit var categoriaAdapter: CategoriaAdapter
    // Almacena la lista completa de categorías para pasarla al reproductor
    // Esto permite navegar entre todos los canales desde el reproductor
    private var todasLasCategorias: List<Categoria> = emptyList()

    /**
     * Método de inicialización de la actividad.
     * Configura la interfaz de usuario y carga los datos iniciales.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Inicialización del ViewBinding para acceso seguro a las vistas
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Configuración inicial de la actividad
            setupViewModel() // Configurar el ViewModel
            setupRecyclerView() // Configurar la lista de categorías
            setupSearch() // Configurar la búsqueda
            setupGlideConfiguration() // Optimizar carga de imágenes
            observeViewModel() // Observar cambios en los datos
            cargarPlaylist() // Cargar la playlist guardada
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar la aplicación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Configura el ViewModel de la actividad.
     * Inicializa el ViewModel usando ViewModelProvider para mantener los datos
     * durante cambios de configuración (rotación de pantalla, etc.).
     */
    private fun setupViewModel() {
        try {
            viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en setupViewModel: ${e.message}", e)
            throw e
        }
    }

    /**
     * Configura el RecyclerView y su adaptador.
     * Inicializa el adaptador con una lista vacía y configura el layout manager.
     * El RecyclerView mostrará las categorías en formato vertical.
     */
    private fun setupRecyclerView() {
        try {
            // Creación del adaptador con callback para manejar clics en canales
            // Cuando se hace clic en un canal, se abre el reproductor
            categoriaAdapter = CategoriaAdapter(emptyList()) { canales, position ->
                abrirReproductor(canales, position)
            }

            // Configuración del RecyclerView principal
            binding.recyclerViewCategorias.apply {
                // LayoutManager vertical para mostrar categorías en lista
                layoutManager = LinearLayoutManager(this@MainActivity)
                // Asignar el adaptador
                adapter = categoriaAdapter
                // Configurar para recibir foco y manejar navegación con teclado/control remoto
                isFocusable = true
                isFocusableInTouchMode = true
                // Configurar para manejar navegación con teclado
                descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en setupRecyclerView: ${e.message}", e)
            throw e
        }
    }

    /**
     * Configura la funcionalidad de búsqueda en la actividad.
     * Permite buscar canales en tiempo real mientras el usuario escribe.
     */
    private fun setupSearch() {
        // Listener para cuando se presiona el botón de búsqueda en el teclado
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchEditText.text.toString()
                viewModel.buscarCanales(query)
                true
            } else {
                false
            }
        }

        // Búsqueda en tiempo real mientras el usuario escribe
        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Realizar búsqueda con el texto actual
                viewModel.buscarCanales(s?.toString() ?: "")
            }
        })
    }

    /**
     * Configura Glide para optimizar la carga de logos de canales.
     * Establece configuraciones de memoria y cache para mejor rendimiento.
     */
    private fun setupGlideConfiguration() {
        try {
            // Configurar Glide para optimizar la carga de imágenes
            // HIGH memory category para mejor rendimiento en carga de imágenes
            Glide.get(this).setMemoryCategory(com.bumptech.glide.MemoryCategory.HIGH)
            
            Log.d("MainActivity", "Glide configurado con cache de memoria optimizado")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error configurando Glide: ${e.message}", e)
        }
    }

    /**
     * Configura los observadores del ViewModel.
     * Observa cambios en las categorías y errores para actualizar la UI.
     */
    private fun observeViewModel() {
        try {
            // Observador para las categorías - se ejecuta cuando cambian los datos
            viewModel.categorias.observe(this) { categorias ->
                this.todasLasCategorias = categorias // Guardar la lista completa para el reproductor
                try {
                    if (categorias.isEmpty()) {
                        // Mostrar mensaje si no hay categorías (por ejemplo, después de una búsqueda sin resultados)
                        binding.textViewMensaje.visibility = View.VISIBLE
                        binding.textViewMensaje.text = "No se encontraron canales"
                    } else {
                        // Actualizar el adaptador con las nuevas categorías
                        binding.textViewMensaje.visibility = View.GONE
                        categoriaAdapter = CategoriaAdapter(categorias) { canales, position ->
                            abrirReproductor(canales, position)
                        }
                        binding.recyclerViewCategorias.adapter = categoriaAdapter
                    }
                    binding.progressBar.visibility = View.GONE
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error al actualizar categorías: ${e.message}", e)
                    binding.textViewMensaje.visibility = View.VISIBLE
                    binding.textViewMensaje.text = "Error al mostrar categorías: ${e.message}"
                }
            }

            // Observador para los errores - se ejecuta cuando ocurre un error
            viewModel.error.observe(this) { error ->
                binding.progressBar.visibility = View.GONE
                binding.textViewMensaje.visibility = View.VISIBLE
                binding.textViewMensaje.text = "Error: $error"
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en observeViewModel: ${e.message}", e)
            throw e
        }
    }

    /**
     * Abre la actividad del reproductor con la URL del canal seleccionado.
     * Crea una lista aplanada con todos los canales para permitir navegación
     * entre canales desde el reproductor.
     * 
     * @param canalesDeCategoria La lista de canales de la categoría seleccionada
     * @param positionEnCategoria La posición del canal dentro de su categoría
     */
    private fun abrirReproductor(canalesDeCategoria: List<Canal>, positionEnCategoria: Int) {
        try {
            // 1. Crear una lista aplanada con TODOS los canales de TODAS las categorías
            // Esto permite navegar entre todos los canales desde el reproductor
            val todosLosCanales = todasLasCategorias.flatMap { it.canales }

            // 2. Encontrar el canal que fue clickeado para saber su posición en la lista global
            val canalClickeado = canalesDeCategoria[positionEnCategoria]
            val posicionGlobal = todosLosCanales.indexOf(canalClickeado)

            if (posicionGlobal == -1) {
                Toast.makeText(this, "Error al procesar la lista de canales.", Toast.LENGTH_SHORT).show()
                return
            }

            // 3. Enviar la lista global completa y la posición correcta al reproductor
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("canales", ArrayList(todosLosCanales))
                putExtra("position", posicionGlobal)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al abrir reproductor: ${e.message}", e)
            Toast.makeText(this, "Error al abrir el reproductor: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Inicia el proceso de carga de la playlist.
     * Muestra el indicador de progreso y oculta mensajes anteriores.
     * La playlist se carga desde el archivo guardado durante el login.
     */
    private fun cargarPlaylist() {
        try {
            // Mostrar indicador de progreso
            binding.progressBar.visibility = View.VISIBLE
            binding.textViewMensaje.visibility = View.GONE
            
            // Iniciar la carga de la playlist en el ViewModel
            viewModel.cargarPlaylist(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al cargar playlist: ${e.message}", e)
            binding.textViewMensaje.visibility = View.VISIBLE
            binding.textViewMensaje.text = "Error al cargar la playlist: ${e.message}"
            binding.progressBar.visibility = View.GONE
        }
    }

    /**
     * Maneja los eventos de teclado para navegación con control remoto.
     * Permite usar las teclas de dirección para navegar y ENTER para seleccionar.
     */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER -> {
                // El manejo del botón central ahora se hace directamente en el CanalAdapter
                // Solo manejamos aquí si no hay ningún item enfocado
                val focusedChild = binding.recyclerViewCategorias.findFocus()
                if (focusedChild == null) {
                    // Si no hay foco en ningún canal, intentar enfocar el primer canal
                    val firstViewHolder = binding.recyclerViewCategorias.findViewHolderForAdapterPosition(0)
                    if (firstViewHolder is CategoriaAdapter.CategoriaViewHolder) {
                        val canalRecyclerView = firstViewHolder.itemView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewCanales)
                        val firstCanalViewHolder = canalRecyclerView?.findViewHolderForAdapterPosition(0)
                        if (firstCanalViewHolder is CanalAdapter.CanalViewHolder) {
                            firstCanalViewHolder.itemView.requestFocus()
                            return true
                        }
                    }
                }
                super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}