package com.example.iptvcpruebadesdecero

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.iptvcpruebadesdecero.databinding.ActivityPlayerBinding
import com.example.iptvcpruebadesdecero.model.Canal
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer as VLCMediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Actividad que maneja la reproducción de streams IPTV.
 * Utiliza VLC para reproducir el contenido multimedia.
 * Implementa un reproductor de video a pantalla completa con controles personalizados.
 * 
 * Funcionalidades principales:
 * - Reproducción de streams IPTV usando VLC
 * - Controles de reproducción (play/pause, adelantar/retroceder)
 * - Navegación entre canales (anterior/siguiente)
 * - Controles táctiles para mostrar/ocultar interfaz
 * - Barra de progreso con tiempo actual/total
 * - Modo pantalla completa
 */
class PlayerActivity : AppCompatActivity() {
    // ViewBinding para acceder a las vistas de manera segura
    private lateinit var binding: ActivityPlayerBinding

    // Lista de canales y posición actual en la lista
    private var canales: List<Canal> = emptyList()
    private var currentPosition: Int = -1

    // Instancias del reproductor VLC
    private var libVLC: LibVLC? = null
    private var vlcPlayer: VLCMediaPlayer? = null

    // Variables para control de UI y estado
    private var isPlaying = true // Estado de reproducción
    private var controlsVisible = false // Visibilidad de controles
    private val handler = Handler(Looper.getMainLooper()) // Handler para tareas programadas
    
    // Variables para cambio de canal con números
    private var channelNumberInput = StringBuilder()
    private val channelNumberTimeout = 2000L // 2 segundos para ejecutar cambio de canal
    
    // Variables para lista de canales simple
    private var channelListDialog: android.app.Dialog? = null
    private var isChannelListVisible = false
    private var orderedChannels: List<Canal> = emptyList() // Canales ordenados con numeración correcta
    private var focusedChannelView: View? = null
    
    // Runnable para ocultar controles automáticamente
    private val hideControlsRunnable = Runnable { hideControls() }
    
    // Runnable para ocultar overlay de número de canal
    private val hideChannelNumberRunnable = Runnable { 
        binding.channelNumberOverlay.visibility = View.GONE 
        channelNumberInput.clear()
    }
    
    // Runnable para mostrar error si no se puede cargar el canal
    private val splashTimeoutRunnable = Runnable {
        binding.loadingAnimation.visibility = View.GONE
        binding.playerView.visibility = View.VISIBLE
        AlertDialog.Builder(this)
            .setTitle("Error de carga")
            .setMessage("No se pudo iniciar la reproducción del canal. Puede que el canal no tenga video o haya un problema de red/codec.")
            .setPositiveButton("Cerrar") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
    
    // Runnable para actualizar la barra de progreso cada segundo
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgressBar()
            handler.postDelayed(this, 1000) // Actualizar cada segundo
        }
    }

    /**
     * Método de inicialización de la actividad.
     * Configura el reproductor y obtiene la URL del stream a reproducir.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mantener la pantalla activa (no se bloqueará automáticamente)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener la lista de canales y la posición desde el intent de forma segura
        // Compatible con API 29+ (Android 10+)
        @Suppress("DEPRECATION")
        canales = intent.getSerializableExtra("canales") as? List<Canal> ?: emptyList()
        currentPosition = intent.getIntExtra("position", -1)

        // Validar que se recibieron los datos correctos
        if (canales.isEmpty() || currentPosition == -1) {
            Toast.makeText(this, "Error: No se pudieron cargar los datos del canal.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Asegurar numeración correcta y orden ascendente de canales
        orderedChannels = ensureChannelNumbering(canales)
        
        // Configurar la actividad
        setupLoadingAnimation() // Configurar animación de carga
        setupVLCPlayer() // Configurar reproductor VLC
        setupControls() // Configurar controles personalizados
        setupTouchListener() // Configurar listener de toques
        showControls() // Mostrar controles inicialmente
    }

    /**
     * Configura la animación de carga Lottie.
     * Muestra una animación mientras se carga el canal.
     */
    private fun setupLoadingAnimation() {
        binding.loadingAnimation.apply {
            setAnimation("CargaTV - 1749346448115.json") // Archivo de animación Lottie
            repeatCount = -1 // Repetir indefinidamente
            playAnimation()
        }
    }

    /**
     * Configura los controles personalizados del reproductor.
     * Incluye botones de navegación, reproducción y barra de progreso.
     */
    private fun setupControls() {
        // Configurar foco para todos los botones de control
        setupButtonFocus()
        
        // Botón de volver - cierra la actividad
        binding.backButton.setOnClickListener {
            finish()
        }

        // Botón de pausa/reproducción
        binding.playPauseButton.setOnClickListener {
            if (isPlaying) {
                vlcPlayer?.pause()
                binding.playPauseButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                vlcPlayer?.play()
                binding.playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            }
            isPlaying = !isPlaying
            resetControlsTimer() // Reiniciar timer para ocultar controles
        }

        // Botón de canal anterior
        binding.previousButton.setOnClickListener {
            if (currentPosition > 0) {
                currentPosition--
                loadChannel(currentPosition)
            }
            resetControlsTimer()
        }

        // Botón de siguiente canal
        binding.nextButton.setOnClickListener {
            if (currentPosition < canales.size - 1) {
                currentPosition++
                loadChannel(currentPosition)
            }
            resetControlsTimer()
        }

        // Botón para abrir lista de canales
        binding.channelListButton.setOnClickListener {
            if (isChannelListVisible) {
                closeChannelList()
            } else {
                openChannelList()
            }
            resetControlsTimer()
        }

        // Botón de adelantar 5 segundos
        binding.forwardButton.setOnClickListener {
            vlcPlayer?.let { player ->
                val currentTime = player.time
                player.time = currentTime + 5000 // Adelantar 5 segundos
            }
            resetControlsTimer()
        }

        // Botón de retroceder 5 segundos
        binding.rewindButton.setOnClickListener {
            vlcPlayer?.let { player ->
                val currentTime = player.time
                player.time = maxOf(0, currentTime - 5000) // Retroceder 5 segundos, mínimo 0
            }
            resetControlsTimer()
        }

        // Barra de progreso (SeekBar) para navegar en el contenido
        binding.progressSeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            var userSeeking = false // Flag para evitar actualizaciones durante el seeking
            
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && vlcPlayer != null && vlcPlayer!!.length > 0) {
                    // Calcular el tiempo basado en el progreso de la barra
                    val newTime = (progress / 1000.0 * vlcPlayer!!.length).toLong()
                    binding.currentTimeText.text = formatMillis(newTime)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                userSeeking = true
            }
            
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                userSeeking = false
                if (vlcPlayer != null && vlcPlayer!!.length > 0) {
                    // Aplicar el cambio de tiempo al reproductor
                    val newTime = (seekBar!!.progress / 1000.0 * vlcPlayer!!.length).toLong()
                    vlcPlayer!!.time = newTime
                }
            }
        })
    }

    /**
     * Configura el foco para todos los botones de control del reproductor.
     * Permite la navegación con teclado/control remoto.
     */
    private fun setupButtonFocus() {
        // Configurar foco para todos los botones de control
        val controlButtons = listOf(
            binding.backButton,
            binding.previousButton,
            binding.rewindButton,
            binding.playPauseButton,
            binding.forwardButton,
            binding.nextButton,
            binding.channelListButton
        )
        
        controlButtons.forEach { button ->
            button.isFocusable = true
            button.isFocusableInTouchMode = true
            
            // Agregar listener para reiniciar timer cuando se enfoca un botón
            button.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    resetControlsTimer() // Reiniciar timer cuando se enfoca un botón
                }
            }
        }
    }

    /**
     * Maneja los eventos de teclado para navegación con control remoto.
     * Permite usar las teclas de dirección para navegar, ENTER para seleccionar,
     * números para cambio de canal, y botones de colores.
     */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // Si los controles están ocultos, mostrarlos con cualquier evento de teclado
        if (!controlsVisible && keyCode != android.view.KeyEvent.KEYCODE_BACK) {
            showControls()
            return true
        }
        
        // Manejar botones de colores
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_PROG_RED -> {
                // Botón rojo - Favoritos
                toggleFavorite()
                resetControlsTimer()
                return true
            }
            android.view.KeyEvent.KEYCODE_PROG_GREEN -> {
                // Botón verde - EPG
                showEPG()
                resetControlsTimer()
                return true
            }
            android.view.KeyEvent.KEYCODE_PROG_YELLOW -> {
                // Botón amarillo - Idioma/Subtítulos
                showLanguageSubtitleMenu()
                resetControlsTimer()
                return true
            }
            android.view.KeyEvent.KEYCODE_PROG_BLUE -> {
                // Botón azul - Menú contextual
                showContextMenu()
                resetControlsTimer()
                return true
            }
        }
        
        // Manejar números para cambio de canal
        if (keyCode >= android.view.KeyEvent.KEYCODE_0 && keyCode <= android.view.KeyEvent.KEYCODE_9) {
            val digit = keyCode - android.view.KeyEvent.KEYCODE_0
            handleChannelNumberInput(digit)
            resetControlsTimer()
            return true
        }
        
        // Manejar volumen
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_VOLUME_UP,
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Permitir que el sistema maneje el volumen
                return super.onKeyDown(keyCode, event)
            }
        }
        
        // Manejar cambio de canal con CHANNEL_UP/DOWN
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_CHANNEL_UP -> {
                if (currentPosition < canales.size - 1) {
                    currentPosition++
                    loadChannel(currentPosition)
                }
                resetControlsTimer()
                return true
            }
            android.view.KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                if (currentPosition > 0) {
                    currentPosition--
                    loadChannel(currentPosition)
                }
                resetControlsTimer()
                return true
            }
        }
        
        // Manejar DPAD_LEFT para abrir lista de canales
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
            if (isChannelListVisible) {
                // Si la lista está visible, cerrarla
                closeChannelList()
                return true
            } else {
                // Verificar si el foco está en el primer botón de control
                val focusedView = currentFocus
                val isFirstControl = focusedView == binding.previousButton || 
                                     focusedView == binding.backButton ||
                                     focusedView == null
                
                if (isFirstControl) {
                    // Si estamos en el primer elemento o no hay foco, abrir lista
                    openChannelList()
                    return true
                }
                // Permitir navegación normal entre controles
                return super.onKeyDown(keyCode, event)
            }
        }
        
        // Si la lista está visible, manejar navegación
        if (isChannelListVisible && keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
            // Cerrar lista al presionar derecha
            closeChannelList()
            return true
        }
        
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER -> {
                // Obtener el botón actualmente enfocado
                val focusedView = currentFocus
                when (focusedView) {
                    binding.backButton -> {
                        finish()
                        true
                    }
                    binding.previousButton -> {
                        if (currentPosition > 0) {
                            currentPosition--
                            loadChannel(currentPosition)
                        }
                        resetControlsTimer()
                        true
                    }
                    binding.nextButton -> {
                        if (currentPosition < canales.size - 1) {
                            currentPosition++
                            loadChannel(currentPosition)
                        }
                        resetControlsTimer()
                        true
                    }
                    binding.channelListButton -> {
                        if (isChannelListVisible) {
                            closeChannelList()
                        } else {
                            openChannelList()
                        }
                        resetControlsTimer()
                        true
                    }
                    binding.playPauseButton -> {
                        if (isPlaying) {
                            vlcPlayer?.pause()
                            binding.playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                        } else {
                            vlcPlayer?.play()
                            binding.playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                        }
                        isPlaying = !isPlaying
                        resetControlsTimer()
                        true
                    }
                    binding.forwardButton -> {
                        vlcPlayer?.let { player ->
                            val currentTime = player.time
                            player.time = currentTime + 5000
                        }
                        resetControlsTimer()
                        true
                    }
                    binding.rewindButton -> {
                        vlcPlayer?.let { player ->
                            val currentTime = player.time
                            player.time = maxOf(0, currentTime - 5000)
                        }
                        resetControlsTimer()
                        true
                    }
                    else -> super.onKeyDown(keyCode, event)
                }
            }
            android.view.KeyEvent.KEYCODE_DPAD_UP,
            android.view.KeyEvent.KEYCODE_DPAD_DOWN,
            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // Reiniciar timer cuando se navega con las teclas de dirección
                resetControlsTimer()
                super.onKeyDown(keyCode, event)
            }
            android.view.KeyEvent.KEYCODE_BACK -> {
                // Si el panel está visible, cerrarlo primero
                if (isChannelListVisible) {
                    closeChannelList()
                    return true
                }
                // Ocultar controles o salir
                if (controlsVisible) {
                    hideControls()
                    return true
                }
                finish()
                true
            }
            android.view.KeyEvent.KEYCODE_HOME -> {
                // Permitir que el sistema maneje HOME
                super.onKeyDown(keyCode, event)
            }
            android.view.KeyEvent.KEYCODE_MENU -> {
                // Mostrar menú contextual
                showContextMenu()
                resetControlsTimer()
                true
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
        handler.removeCallbacks(hideChannelNumberRunnable)
        
        // Programar cambio de canal después del timeout
        handler.postDelayed({
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
        binding.channelNumberOverlay.text = number
        binding.channelNumberOverlay.visibility = View.VISIBLE
    }
    
    /**
     * Oculta el overlay del número de canal.
     */
    private fun hideChannelNumberOverlay() {
        handler.postDelayed({
            binding.channelNumberOverlay.visibility = View.GONE
        }, 500)
    }
    
    /**
     * Cambia al canal con el número especificado.
     */
    private fun changeToChannel(channelNumber: Int) {
        val canal = canales.find { it.numero == channelNumber }
        if (canal != null) {
            val position = canales.indexOf(canal)
            if (position >= 0) {
                currentPosition = position
                loadChannel(position)
            }
        } else {
            Toast.makeText(this, "Canal $channelNumber no encontrado", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Agrega o quita el canal actual de favoritos.
     */
    private fun toggleFavorite() {
        val canal = canales.getOrNull(currentPosition)
        if (canal != null) {
            Toast.makeText(this, "Favorito: ${canal.nombre}", Toast.LENGTH_SHORT).show()
            // TODO: Implementar persistencia de favoritos
        }
    }
    
    /**
     * Muestra la guía de programación electrónica (EPG).
     */
    private fun showEPG() {
        val canal = canales.getOrNull(currentPosition)
        if (canal != null) {
            Toast.makeText(this, "EPG: ${canal.nombre} (próximamente)", Toast.LENGTH_SHORT).show()
            // TODO: Implementar funcionalidad de EPG
        }
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
        val canal = canales.getOrNull(currentPosition)
        val options = arrayOf(
            "Información del canal",
            "Agregar a favoritos",
            "Idioma/Subtítulos",
            "Configuración"
        )
        AlertDialog.Builder(this)
            .setTitle(canal?.nombre ?: "Menú")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Información del canal", Toast.LENGTH_SHORT).show()
                    1 -> toggleFavorite()
                    2 -> showLanguageSubtitleMenu()
                    3 -> Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    /**
     * Configura el listener de toques para mostrar/ocultar controles.
     * Permite controlar la visibilidad de la interfaz con toques en la pantalla.
     */
    private fun setupTouchListener() {
        binding.playerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Alternar visibilidad de controles con un toque
                    if (controlsVisible) {
                        hideControls()
                    } else {
                        showControls()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Muestra los controles y programa su ocultación automática.
     * Hace visible toda la interfaz de controles.
     */
    private fun showControls() {
        controlsVisible = true
        binding.topBarLayout.visibility = View.VISIBLE
        binding.controlsBar.visibility = View.VISIBLE
        binding.progressBarLayout.visibility = View.VISIBLE
        resetControlsTimer() // Programar ocultación automática
    }

    /**
     * Oculta los controles del reproductor.
     * Permite una experiencia de reproducción más inmersiva.
     */
    private fun hideControls() {
        controlsVisible = false
        binding.topBarLayout.visibility = View.GONE
        binding.controlsBar.visibility = View.GONE
        binding.progressBarLayout.visibility = View.GONE
    }

    /**
     * Reinicia el timer para ocultar controles automáticamente.
     * Los controles se ocultan después de 5 segundos de inactividad.
     */
    private fun resetControlsTimer() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 5000) // Ocultar después de 5 segundos
    }

    /**
     * Carga un canal específico en el reproductor.
     * 
     * @param position Posición del canal en la lista
     */
    private fun loadChannel(position: Int) {
        val canal = canales.getOrNull(position)
        if (canal != null) {
            // Actualizar nombre del canal en la interfaz
            binding.channelNameText.text = canal.nombre
            binding.loadingAnimation.visibility = View.VISIBLE
            
            // Detener reproducción actual
            vlcPlayer?.stop()
            
            // Preparar nuevo media con optimizaciones
            val media = Media(libVLC, Uri.parse(canal.url))
            media.setHWDecoderEnabled(true, false) // Habilitar decodificación por hardware
            media.addOption(":aout=android_audiotrack") // Forzar salida de audio específica
            
            // Configurar el reproductor
            vlcPlayer?.media = media
            vlcPlayer?.volume = 100 // Asegurar volumen máximo
            
            // Programar timeout para mostrar error si no carga
            handler.removeCallbacks(splashTimeoutRunnable)
            handler.postDelayed(splashTimeoutRunnable, 10000) // 10 segundos de timeout
            
            // Iniciar reproducción
            vlcPlayer?.play()
            
            // Seleccionar automáticamente la primera pista de audio disponible
            vlcPlayer?.let { player ->
                val audioTracks = player.audioTracks
                if (audioTracks != null && audioTracks.isNotEmpty()) {
                    val firstTrackId = audioTracks[0].id
                    player.setAudioTrack(firstTrackId)
                }
            }
            
            // Actualizar estado de reproducción
            isPlaying = true
            binding.playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            
            // Iniciar actualización de barra de progreso
            handler.removeCallbacks(updateProgressRunnable)
            handler.post(updateProgressRunnable)
        }
    }

    /**
     * Configura el reproductor VLC con la URL proporcionada.
     * Inicializa el reproductor, configura los listeners y comienza la reproducción.
     */
    private fun setupVLCPlayer() {
        try {
            // Inicializar LibVLC con opciones optimizadas
            libVLC = LibVLC(this, arrayListOf("--no-drop-late-frames", "--no-skip-frames"))
            vlcPlayer = VLCMediaPlayer(libVLC)

            // Obtener la URL del canal actual
            val canal = canales.getOrNull(currentPosition)
            if (canal == null) {
                Toast.makeText(this, "No se pudo obtener el canal.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Mostrar nombre del canal en la interfaz
            binding.channelNameText.text = canal.nombre

            // Asignar el VLCVideoLayout al reproductor
            vlcPlayer?.attachViews(binding.playerView, null, false, false)

            // Preparar el Media con optimizaciones
            val media = Media(libVLC, Uri.parse(canal.url))
            media.setHWDecoderEnabled(true, false) // Habilitar decodificación por hardware
            media.addOption(":aout=android_audiotrack") // Forzar salida de audio específica
            
            // Configurar el reproductor
            vlcPlayer?.media = media
            vlcPlayer?.volume = 100 // Asegurar volumen máximo
            
            // Programar timeout para mostrar error si no carga
            handler.removeCallbacks(splashTimeoutRunnable)
            handler.postDelayed(splashTimeoutRunnable, 10000) // 10 segundos de timeout

            // Listener para eventos de VLC
            vlcPlayer?.setEventListener { event ->
                runOnUiThread {
                    when (event.type) {
                        0x100 -> { // Playing - Reproducción iniciada
                            handler.removeCallbacks(splashTimeoutRunnable)
                            binding.loadingAnimation.visibility = View.GONE
                            binding.playerView.visibility = View.VISIBLE
                            isPlaying = true
                            binding.playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                            
                            // Seleccionar automáticamente la primera pista de audio disponible
                            vlcPlayer?.let { player ->
                                val audioTracks = player.audioTracks
                                if (audioTracks != null && audioTracks.isNotEmpty()) {
                                    val firstTrackId = audioTracks[0].id
                                    player.setAudioTrack(firstTrackId)
                                }
                            }
                            
                            // Iniciar actualización de barra de progreso
                            handler.removeCallbacks(updateProgressRunnable)
                            handler.post(updateProgressRunnable)
                        }
                        0x10C -> { // VIDEO_OUTPUT (268) - Video disponible
                            handler.removeCallbacks(splashTimeoutRunnable)
                            binding.loadingAnimation.visibility = View.GONE
                            binding.playerView.visibility = View.VISIBLE
                            
                            // Iniciar actualización de barra de progreso
                            handler.removeCallbacks(updateProgressRunnable)
                            handler.post(updateProgressRunnable)
                        }
                        0x103 -> { // Buffering - Cargando contenido
                            binding.loadingAnimation.visibility = View.VISIBLE
                            binding.playerView.visibility = View.VISIBLE
                        }
                        0x102 -> { // EndReached - Fin del contenido
                            handler.removeCallbacks(splashTimeoutRunnable)
                            binding.loadingAnimation.visibility = View.GONE
                            
                            // Detener actualización de barra de progreso
                            handler.removeCallbacks(updateProgressRunnable)
                        }
                        0x101 -> { // Error - Error de reproducción
                            handler.removeCallbacks(splashTimeoutRunnable)
                            binding.loadingAnimation.visibility = View.GONE
                            
                            // Mostrar diálogo de error
                            AlertDialog.Builder(this)
                                .setTitle("Error de Reproducción (VLC)")
                                .setMessage("No se pudo reproducir el canal con VLC.")
                                .setPositiveButton("Cerrar") { _, _ -> finish() }
                                .setCancelable(false)
                                .show()
                            
                            // Detener actualización de barra de progreso
                            handler.removeCallbacks(updateProgressRunnable)
                        }
                    }
                }
            }

            // Iniciar reproducción
            vlcPlayer?.play()
            
            // Seleccionar automáticamente la primera pista de audio disponible
            vlcPlayer?.let { player ->
                val audioTracks = player.audioTracks
                if (audioTracks != null && audioTracks.isNotEmpty()) {
                    val firstTrackId = audioTracks[0].id
                    player.setAudioTrack(firstTrackId)
                }
            }
            
            // Actualizar estado de reproducción
            isPlaying = true
            binding.playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
            
            // Iniciar actualización de barra de progreso
            handler.removeCallbacks(updateProgressRunnable)
            handler.post(updateProgressRunnable)
        } catch (e: Exception) {
            // Manejar errores de inicialización
            handler.removeCallbacks(splashTimeoutRunnable)
            binding.loadingAnimation.visibility = View.GONE
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle("Error Crítico del Reproductor VLC")
                    .setMessage("No se pudo inicializar el reproductor VLC:\n\n${e.message}")
                    .setPositiveButton("Cerrar") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    /**
     * Método llamado cuando la actividad se inicia.
     * Reanuda la reproducción si el reproductor está disponible.
     */
    override fun onStart() {
        super.onStart()
        vlcPlayer?.play()
    }

    /**
     * Método llamado cuando la actividad se detiene.
     * Pausa la reproducción si el reproductor está disponible.
     */
    override fun onStop() {
        super.onStop()
        vlcPlayer?.pause()
    }

    /**
     * Método llamado cuando la actividad se destruye.
     * Libera los recursos del reproductor para evitar memory leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        // Limpiar el flag de mantener pantalla activa
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Limpiar todos los callbacks programados
        handler.removeCallbacks(hideControlsRunnable)
        handler.removeCallbacks(splashTimeoutRunnable)
        handler.removeCallbacks(updateProgressRunnable)
        handler.removeCallbacks(hideChannelNumberRunnable)
        
        // Liberar recursos del reproductor VLC
        try {
            vlcPlayer?.stop()
            vlcPlayer?.detachViews()
            vlcPlayer?.release()
            libVLC?.release()
        } catch (_: Exception) {}
        
        // Limpiar referencias
        vlcPlayer = null
        libVLC = null
    }

    /**
     * Método llamado cuando cambia el foco de la ventana.
     * Oculta la interfaz del sistema cuando la actividad tiene el foco.
     * 
     * @param hasFocus Indica si la actividad tiene el foco
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI() // Ocultar barras del sistema para pantalla completa
        }
    }

    /**
     * Oculta la interfaz del sistema (barras de estado y navegación)
     * para una experiencia de reproducción a pantalla completa.
     */
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    /**
     * Actualiza la barra de progreso con el tiempo actual y total.
     * Se ejecuta cada segundo para mantener la información actualizada.
     */
    private fun updateProgressBar() {
        vlcPlayer?.let { player ->
            val duration = player.length
            val position = player.time
            if (duration > 0) {
                // Calcular progreso como porcentaje (0-1000)
                val progress = ((position.toDouble() / duration) * 1000).toInt()
                binding.progressSeekBar.progress = progress
                binding.currentTimeText.text = formatMillis(position)
                binding.totalTimeText.text = formatMillis(duration)
            } else {
                // Si no hay duración disponible, resetear la barra
                binding.progressSeekBar.progress = 0
                binding.currentTimeText.text = "00:00"
                binding.totalTimeText.text = "00:00"
            }
        }
    }

    /**
     * Formatea milisegundos en formato MM:SS.
     * 
     * @param millis Tiempo en milisegundos
     * @return String formateado como MM:SS
     */
    private fun formatMillis(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Asegura que los canales tengan numeración correcta en orden ascendente (1, 2, 3, 4...).
     * Si los números no están en orden o hay duplicados, los reasigna secuencialmente.
     * 
     * @param channels Lista original de canales
     * @return Lista de canales con numeración corregida en orden ascendente
     */
    private fun ensureChannelNumbering(channels: List<Canal>): List<Canal> {
        return channels.mapIndexed { index, canal ->
            // Asignar número secuencial empezando desde 1
            canal.copy(numero = index + 1)
        }
    }
    
    /**
     * Abre la lista de canales en un diálogo simple y estable.
     */
    private fun openChannelList() {
        if (isChannelListVisible) {
            closeChannelList()
            return
        }
        
        try {
            // Crear diálogo simple
            val dialog = android.app.Dialog(this)
            dialog.setContentView(R.layout.dialog_channel_list)
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.4).toInt(),
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            dialog.window?.setGravity(Gravity.END)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            val linearLayout = dialog.findViewById<LinearLayout>(R.id.linearLayoutChannels)
            val scrollView = dialog.findViewById<android.widget.ScrollView>(R.id.scrollViewChannels)
            
            // Limpiar views anteriores
            linearLayout.removeAllViews()
            
            // Encontrar posición del canal actual
            val currentCanal = canales.getOrNull(currentPosition)
            var currentChannelIndex = 0
            
            // Crear views simples para cada canal
            orderedChannels.forEachIndexed { index, canal ->
                val channelView = createChannelView(canal, index, dialog)
                linearLayout.addView(channelView)
                
                // Marcar el canal actual
                if (currentCanal != null && canal.id == currentCanal.id) {
                    currentChannelIndex = index
                }
            }
            
            // Scroll al canal actual después de que se dibujen los views
            scrollView.post {
                val viewToScroll = linearLayout.getChildAt(currentChannelIndex)
                if (viewToScroll != null) {
                    scrollView.smoothScrollTo(0, viewToScroll.top)
                    // Enfocar el canal actual
                    viewToScroll.requestFocus()
                    focusedChannelView = viewToScroll
                    updateChannelViewFocus(viewToScroll, true)
                }
            }
            
            dialog.setOnDismissListener {
                isChannelListVisible = false
                focusedChannelView = null
                resetControlsTimer()
            }
            
            dialog.show()
            channelListDialog = dialog
            isChannelListVisible = true
            resetControlsTimer()
            
        } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "Error al abrir lista de canales: ${e.message}", e)
            Toast.makeText(this, "Error al abrir lista de canales", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Crea un view simple para un canal.
     */
    private fun createChannelView(canal: Canal, index: Int, dialog: android.app.Dialog): View {
        val view = TextView(this)
        view.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(4, 4, 4, 4)
        }
        
        view.text = "${canal.numero}. ${canal.nombre}"
        view.textSize = 18f
        view.setTextColor(resources.getColor(android.R.color.white, null))
        view.setPadding(24, 24, 24, 24)
        view.background = android.graphics.drawable.ColorDrawable(
            resources.getColor(R.color.surface_dark, null)
        )
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        
        // Listener de foco
        view.setOnFocusChangeListener { v, hasFocus ->
            updateChannelViewFocus(v, hasFocus)
        }
        
        // Listener de clic/OK
        view.setOnClickListener {
            selectChannel(canal, dialog)
        }
        
        view.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                    android.view.KeyEvent.KEYCODE_ENTER -> {
                        selectChannel(canal, dialog)
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
        
        return view
    }
    
    /**
     * Actualiza el estilo visual del view según el foco.
     */
    private fun updateChannelViewFocus(view: View, hasFocus: Boolean) {
        if (hasFocus) {
            view.background = android.graphics.drawable.ColorDrawable(
                resources.getColor(R.color.primary_tv, null)
            )
            focusedChannelView = view
        } else {
            view.background = android.graphics.drawable.ColorDrawable(
                resources.getColor(R.color.surface_dark, null)
            )
        }
    }
    
    /**
     * Selecciona un canal y cierra el diálogo.
     */
    private fun selectChannel(canal: Canal, dialog: android.app.Dialog) {
        val originalPosition = canales.indexOfFirst { it.id == canal.id }
        if (originalPosition >= 0) {
            currentPosition = originalPosition
            loadChannel(originalPosition)
            dialog.dismiss()
            closeChannelList()
        }
    }
    
    /**
     * Cierra la lista de canales.
     */
    private fun closeChannelList() {
        channelListDialog?.dismiss()
        channelListDialog = null
        isChannelListVisible = false
        focusedChannelView = null
        binding.playPauseButton.requestFocus()
        resetControlsTimer()
    }
} 