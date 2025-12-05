package com.example.iptvcpruebadesdecero

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    // Adaptador para mostrar todos los canales en una sola lista
    private lateinit var canalAdapter: CanalAdapter
    // Almacena la lista completa de canales (aplanada de todas las categorías)
    private var todosLosCanales: List<Canal> = emptyList()
    // Almacena la lista completa sin filtrar para búsquedas
    private var canalesCompletos: List<Canal> = emptyList()

    /**
     * Método de inicialización de la actividad.
     * Configura la interfaz de usuario y carga los datos iniciales.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Mantener la pantalla activa (no se bloqueará automáticamente)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Inicialización del ViewBinding para acceso seguro a las vistas
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Configuración inicial de la actividad
            setupViewModel() // Configurar el ViewModel
            setupRecyclerView() // Configurar la lista de categorías
            setupSearch() // Configurar la búsqueda
            setupRefresh() // Configurar botón de refrescar
            setupLogout() // Configurar botón de cerrar sesión
            setupGlideConfiguration() // Optimizar carga de imágenes
            observeViewModel() // Observar cambios en los datos
            cargarPlaylist() // Cargar la playlist guardada
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar la aplicación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Limpiar el flag cuando se destruye la actividad
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
     * El RecyclerView mostrará todos los canales en formato grid.
     */
    private fun setupRecyclerView() {
        try {
            // Creación del adaptador con callback para manejar clics en canales
            // Cuando se hace clic en un canal, se abre el reproductor
            canalAdapter = CanalAdapter { canales, position ->
                abrirReproductor(canales, position)
            }

            // Configuración del RecyclerView principal
            binding.recyclerViewCategorias.apply {
                // GridLayoutManager para mostrar canales en grid (5 columnas en Android TV)
                layoutManager = GridLayoutManager(this@MainActivity, 5)
                // Asignar el adaptador
                adapter = canalAdapter
                // Configurar para recibir foco y manejar navegación con teclado/control remoto
                isFocusable = true
                isFocusableInTouchMode = true
                // Configurar para manejar navegación con teclado
                descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
                // Deshabilitar animaciones para evitar vibraciones
                itemAnimator = null
                // Mejorar rendimiento durante scroll rápido
                setHasFixedSize(false)
                // Prevenir problemas de reciclaje durante scroll rápido
                isNestedScrollingEnabled = false
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en setupRecyclerView: ${e.message}", e)
            throw e
        }
    }

    // Variables para cambio de canal con números
    private var channelNumberInput = StringBuilder()
    private var channelNumberHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val channelNumberTimeout = 2000L // 2 segundos para ejecutar cambio de canal
    
    /**
     * Configura la funcionalidad de búsqueda en la actividad.
     * Permite buscar canales usando un ícono de lupa navegable.
     */
    private fun setupSearch() {
        // Configurar foco y navegación para el botón de búsqueda
        binding.searchButton.isFocusable = true
        binding.searchButton.isFocusableInTouchMode = true
        
        // Listener para mostrar/ocultar el campo de búsqueda
        binding.searchButton.setOnClickListener {
            toggleSearch()
        }
        
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
                val query = s?.toString() ?: ""
                buscarCanales(query)
            }
        })
    }
    
    /**
     * Alterna la visibilidad del campo de búsqueda.
     */
    private fun toggleSearch() {
        val isVisible = binding.searchLayout.visibility == View.VISIBLE
        if (isVisible) {
            binding.searchLayout.visibility = View.GONE
            binding.searchEditText.text?.clear()
            buscarCanales("")
            // Devolver foco al botón de búsqueda
            binding.searchButton.requestFocus()
        } else {
            binding.searchLayout.visibility = View.VISIBLE
            // Enfocar el campo de búsqueda después de un pequeño delay para asegurar que esté visible
            binding.searchEditText.post {
                binding.searchEditText.requestFocus()
            }
        }
    }
    
    /**
     * Busca canales en la lista completa y actualiza el adaptador.
     */
    private fun buscarCanales(query: String) {
        viewModel.buscarCanales(query)
        // La búsqueda se maneja en observeViewModel que actualiza el adaptador
    }
    
    /**
     * Configura el botón de refrescar la lista.
     */
    private fun setupRefresh() {
        binding.refreshButton.isFocusable = true
        binding.refreshButton.isFocusableInTouchMode = true
        
        binding.refreshButton.setOnClickListener {
            refrescarLista()
        }
    }
    
    /**
     * Configura el botón de cerrar sesión.
     */
    private fun setupLogout() {
        binding.logoutButton.isFocusable = true
        binding.logoutButton.isFocusableInTouchMode = true
        
        binding.logoutButton.setOnClickListener {
            performLogout()
        }
    }
    
    /**
     * Realiza el cierre de sesión y vuelve a LoginActivity.
     * Las credenciales se mantienen guardadas para facilitar el siguiente inicio de sesión.
     */
    private fun performLogout() {
        // NO limpiar credenciales guardadas - se mantienen para el próximo login
        // Las credenciales permanecen en SharedPreferences para facilitar el acceso
        
        // Navegar a LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
                try {
                    // Aplanar todas las categorías en una sola lista de canales
                    val canalesAplanados = categorias.flatMap { it.canales }
                    
                    if (canalesAplanados.isEmpty()) {
                        // Mostrar mensaje si no hay canales
                        binding.textViewMensaje.visibility = View.VISIBLE
                        binding.textViewMensaje.text = "No se encontraron canales"
                    } else {
                        // Asegurar numeración correcta en orden ascendente
                        val canalesConNumeracion = canalesAplanados.mapIndexed { index, canal ->
                            canal.copy(numero = index + 1)
                        }
                        
                        // Guardar la lista completa sin filtrar la primera vez (cuando no hay búsqueda activa)
                        if (canalesCompletos.isEmpty() || canalesAplanados.size == canalesCompletos.size) {
                            canalesCompletos = canalesConNumeracion
                        }
                        
                        // Guardar la lista completa para el reproductor (siempre usar la completa)
                        this.todosLosCanales = canalesCompletos
                        
                        // Actualizar el adaptador con los canales (filtrados o completos)
                        binding.textViewMensaje.visibility = View.GONE
                        // Usar post para evitar actualizaciones concurrentes durante scroll rápido
                        binding.recyclerViewCategorias.post {
                            try {
                                canalAdapter.submitList(canalesConNumeracion)
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error al actualizar adaptador: ${e.message}", e)
                            }
                        }
                    }
                    binding.progressBar.visibility = View.GONE
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error al actualizar canales: ${e.message}", e)
                    binding.textViewMensaje.visibility = View.VISIBLE
                    binding.textViewMensaje.text = "Error al mostrar canales: ${e.message}"
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
     * Usa la lista completa de canales ya aplanada y numerada.
     * 
     * @param canales La lista de canales (puede ser la lista completa o filtrada)
     * @param position La posición del canal seleccionado en la lista
     */
    private fun abrirReproductor(canales: List<Canal>, position: Int) {
        try {
            // Validar que la posición sea válida
            if (position < 0 || position >= canales.size) {
                Toast.makeText(this, "Canal no válido", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Validar que tengamos canales completos
            if (todosLosCanales.isEmpty()) {
                Toast.makeText(this, "Error: Lista de canales vacía", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Usar la lista completa de canales con numeración correcta
            val canalSeleccionado = canales[position]
            val posicionGlobal = todosLosCanales.indexOfFirst { it.id == canalSeleccionado.id }

            if (posicionGlobal == -1) {
                Toast.makeText(this, "Error al procesar la lista de canales.", Toast.LENGTH_SHORT).show()
                return
            }

            // Enviar la lista global completa con numeración correcta y la posición correcta al reproductor
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
     * Refresca la lista de canales recargando la playlist.
     * Limpia la búsqueda activa y recarga los datos desde el archivo guardado.
     */
    private fun refrescarLista() {
        try {
            // Limpiar búsqueda activa si existe
            binding.searchLayout.visibility = View.GONE
            binding.searchEditText.text?.clear()
            
            // Mostrar indicador de progreso
            binding.progressBar.visibility = View.VISIBLE
            binding.textViewMensaje.visibility = View.GONE
            
            // Recargar la playlist
            viewModel.cargarPlaylist(this)
            
            // Mostrar mensaje de confirmación
            Toast.makeText(this, "Refrescando lista...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al refrescar lista: ${e.message}", e)
            Toast.makeText(this, "Error al refrescar la lista: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
        }
    }

    /**
     * Maneja los eventos de teclado para navegación con control remoto.
     * Permite usar las teclas de dirección para navegar, ENTER para seleccionar,
     * números para cambio de canal, y botones de colores.
     */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // Manejar botones de colores
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_PROG_RED -> {
                // Botón rojo - Favoritos
                showFavorites()
                return true
            }
            android.view.KeyEvent.KEYCODE_PROG_GREEN -> {
                // Botón verde - EPG
                showEPG()
                return true
            }
            android.view.KeyEvent.KEYCODE_PROG_YELLOW -> {
                // Botón amarillo - Idioma/Subtítulos
                showLanguageSubtitleMenu()
                return true
            }
            android.view.KeyEvent.KEYCODE_PROG_BLUE -> {
                // Botón azul - Menú contextual
                showContextMenu()
                return true
            }
        }
        
        // Manejar números para cambio de canal
        if (keyCode >= android.view.KeyEvent.KEYCODE_0 && keyCode <= android.view.KeyEvent.KEYCODE_9) {
            val digit = keyCode - android.view.KeyEvent.KEYCODE_0
            handleChannelNumberInput(digit)
            return true
        }
        
        // Manejar BACK para ocultar búsqueda
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            if (binding.searchLayout.visibility == View.VISIBLE) {
                toggleSearch()
                return true
            }
        }
        
        // Manejar DPAD_RIGHT para navegación entre canales
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
            val focusedView = currentFocus
            
            // Prevenir que al navegar a la derecha desde el botón de logout se cierre la sesión
            if (focusedView == binding.logoutButton || focusedView == binding.searchButton) {
                // Si estamos en los botones superiores, no hacer nada
                return true
            }
            // La navegación en grid se maneja automáticamente por el RecyclerView
        }
        
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER -> {
                // Obtener el botón actualmente enfocado
                val focusedView = currentFocus
                when (focusedView) {
                    binding.searchButton -> {
                        toggleSearch()
                        true
                    }
                    binding.refreshButton -> {
                        refrescarLista()
                        true
                    }
                    binding.logoutButton -> {
                        // Solo cerrar sesión con OK/Enter explícito
                        performLogout()
                        true
                    }
                        else -> {
                        // El manejo del botón central ahora se hace directamente en el CanalAdapter
                        // Solo manejamos aquí si no hay ningún item enfocado
                        val focusedChild = binding.recyclerViewCategorias.findFocus()
                        if (focusedChild == null) {
                            // Si no hay foco en ningún canal, intentar enfocar el primer canal
                            val firstViewHolder = binding.recyclerViewCategorias.findViewHolderForAdapterPosition(0)
                            if (firstViewHolder != null) {
                                firstViewHolder.itemView.requestFocus()
                                return true
                            }
                        }
                        super.onKeyDown(keyCode, event)
                    }
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    /**
     * Maneja la entrada de números para cambio de canal.
     * Muestra un overlay con los dígitos ingresados y cambia al canal después de un timeout.
     */
    private fun handleChannelNumberInput(digit: Int) {
        channelNumberInput.append(digit)
        showChannelNumberOverlay(channelNumberInput.toString())
        
        // Cancelar timeout anterior
        channelNumberHandler.removeCallbacksAndMessages(null)
        
        // Programar cambio de canal después del timeout
        channelNumberHandler.postDelayed({
            val channelNumber = channelNumberInput.toString().toIntOrNull()
            if (channelNumber != null) {
                changeToChannel(channelNumber)
            }
            channelNumberInput.clear()
            hideChannelNumberOverlay()
        }, channelNumberTimeout)
    }
    
    /**
     * Muestra un overlay con el número de canal ingresado.
     */
    private fun showChannelNumberOverlay(number: String) {
        // Crear o actualizar un diálogo simple con el número
        // Por ahora usamos un Toast, pero se puede mejorar con un overlay personalizado
        Toast.makeText(this, "Canal: $number", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Oculta el overlay del número de canal.
     */
    private fun hideChannelNumberOverlay() {
        // Implementación para ocultar el overlay
    }
    
    /**
     * Cambia al canal con el número especificado.
     */
    private fun changeToChannel(channelNumber: Int) {
        try {
            // Buscar el canal con el número especificado en la lista completa
            val canalEncontrado = todosLosCanales.firstOrNull { it.numero == channelNumber }
            
            if (canalEncontrado != null) {
                // Encontrar la posición del canal en la lista
                val posicion = todosLosCanales.indexOf(canalEncontrado)
                if (posicion != -1) {
                    abrirReproductor(todosLosCanales, posicion)
                } else {
                    Toast.makeText(this, "Canal $channelNumber no encontrado", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Canal $channelNumber no encontrado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al cambiar de canal: ${e.message}", e)
            Toast.makeText(this, "Error al cambiar de canal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Muestra la lista de favoritos.
     */
    private fun showFavorites() {
        Toast.makeText(this, "Favoritos (próximamente)", Toast.LENGTH_SHORT).show()
        // TODO: Implementar funcionalidad de favoritos
    }
    
    /**
     * Muestra la guía de programación electrónica (EPG).
     */
    private fun showEPG() {
        Toast.makeText(this, "EPG (próximamente)", Toast.LENGTH_SHORT).show()
        // TODO: Implementar funcionalidad de EPG
    }
    
    /**
     * Muestra el menú de idioma y subtítulos.
     */
    private fun showLanguageSubtitleMenu() {
        Toast.makeText(this, "Idioma/Subtítulos (próximamente)", Toast.LENGTH_SHORT).show()
        // TODO: Implementar funcionalidad de idioma/subtítulos
    }
    
    /**
     * Muestra el menú contextual.
     */
    private fun showContextMenu() {
        val options = arrayOf("Información del canal", "Agregar a favoritos", "Configuración")
        AlertDialog.Builder(this)
            .setTitle("Menú")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Información del canal", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
}