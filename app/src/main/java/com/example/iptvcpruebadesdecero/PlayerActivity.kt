package com.example.iptvcpruebadesdecero

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
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
    
    // Runnable para ocultar controles automáticamente
    private val hideControlsRunnable = Runnable { hideControls() }
    
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
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener la lista de canales y la posición desde el intent de forma segura
        // Manejo diferente según la versión de Android para compatibilidad
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            canales = intent.getSerializableExtra("canales", ArrayList::class.java) as? List<Canal> ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            canales = intent.getSerializableExtra("canales") as? List<Canal> ?: emptyList()
        }
        currentPosition = intent.getIntExtra("position", -1)

        // Validar que se recibieron los datos correctos
        if (canales.isEmpty() || currentPosition == -1) {
            Toast.makeText(this, "Error: No se pudieron cargar los datos del canal.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
            binding.nextButton
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
     * Permite usar las teclas de dirección para navegar y ENTER para seleccionar.
     */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // Si los controles están ocultos, mostrarlos con cualquier evento de teclado
        if (!controlsVisible) {
            showControls()
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
            else -> super.onKeyDown(keyCode, event)
        }
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
        // Limpiar todos los callbacks programados
        handler.removeCallbacks(hideControlsRunnable)
        handler.removeCallbacks(splashTimeoutRunnable)
        handler.removeCallbacks(updateProgressRunnable)
        
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
} 