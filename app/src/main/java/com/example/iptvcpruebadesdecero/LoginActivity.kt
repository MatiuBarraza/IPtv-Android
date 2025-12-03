package com.example.iptvcpruebadesdecero

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.iptvcpruebadesdecero.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Actividad de login que permite al usuario autenticarse antes de acceder a la aplicación.
 * El usuario debe ingresar sus credenciales para descargar su lista de reproducción IPTV.
 * 
 * Funcionalidades principales:
 * - Autenticación con servidor IPTV
 * - Descarga de playlist M3U
 * - Guardado de credenciales
 * - Selección de servidor (Los Vilos / La Serena)
 * - Validación de campos en tiempo real
 */
class LoginActivity : AppCompatActivity() {
    // ViewBinding para acceder a las vistas de manera segura
    private lateinit var binding: ActivityLoginBinding
    private val TAG = "LoginActivity"
    
    // SharedPreferences para guardar las credenciales de forma persistente
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "LoginPrefs"
    private val KEY_USERNAME = "saved_username"
    private val KEY_PASSWORD = "saved_password"
    
    // Servidor fijo - solo Los Vilos
    private val SERVER_LOS_VILOS = "Los Vilos"
    
    /**
     * Método de inicialización de la actividad.
     * Configura la interfaz de usuario y carga las credenciales guardadas.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate iniciado")
        
        // Inicializar ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Inicializar SharedPreferences para persistencia de datos
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Configurar la interfaz de usuario
        setupViews()
        setupListeners()
        cargarCredencialesGuardadas()
        Log.d(TAG, "onCreate completado")
    }

    /**
     * Configura las vistas iniciales de la actividad.
     * Establece el estado inicial de botones y campos de error.
     */
    private fun setupViews() {
        try {
            Log.d(TAG, "Configurando vistas")
            // Configurar el estado inicial del botón de login (deshabilitado hasta que se llenen los campos)
            updateLoginButtonState()
            
            // Limpiar cualquier error previo en los campos de texto
            binding.textInputLayoutUsername.error = null
            binding.textInputLayoutPassword.error = null
            
            // Ocultar botón de configuración ya que solo hay un servidor
            binding.buttonConfig.visibility = View.GONE
            
            Log.d(TAG, "Vistas configuradas correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupViews: ${e.message}", e)
        }
    }

    /**
     * Configura los listeners para los campos de texto y botones.
     * Incluye validación en tiempo real y manejo de eventos.
     */
    private fun setupListeners() {
        try {
            Log.d(TAG, "Configurando listeners")
            
            // Listener para el botón de login
            binding.buttonLogin.setOnClickListener {
                performLogin()
            }

            // Listener para el botón de salir
            binding.buttonExit.setOnClickListener {
                finish()
            }

            // TextWatcher para el campo de usuario - validación en tiempo real
            binding.editTextUsername.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    clearUsernameError() // Limpiar error si existe
                    updateLoginButtonState() // Actualizar estado del botón
                }
            })

            // TextWatcher para el campo de contraseña - validación en tiempo real
            binding.editTextPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    clearPasswordError() // Limpiar error si existe
                    updateLoginButtonState() // Actualizar estado del botón
                }
            })

            // Listener para el Enter en el campo de contraseña - iniciar login automáticamente
            binding.editTextPassword.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    performLogin()
                    true
                } else {
                    false
                }
            }
            Log.d(TAG, "Listeners configurados correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupListeners: ${e.message}", e)
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
                // Obtener el botón actualmente enfocado
                val focusedView = currentFocus
                when (focusedView) {
                    binding.buttonLogin -> {
                        performLogin()
                        true
                    }
                    binding.buttonExit -> {
                        finish()
                        true
                    }
                    else -> super.onKeyDown(keyCode, event)
                }
            }
            android.view.KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }


    /**
     * Realiza el proceso de autenticación con el servidor IPTV.
     * Descarga la playlist M3U y la guarda localmente.
     */
    private fun performLogin() {
        Log.d(TAG, "Iniciando proceso de login")
        
        // Obtener y limpiar los valores de los campos
        val username = binding.editTextUsername.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        // Validar que los campos no estén vacíos
        if (username.isEmpty()) {
            binding.textInputLayoutUsername.error = "El usuario es requerido"
            binding.editTextUsername.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = "La contraseña es requerida"
            binding.editTextPassword.requestFocus()
            return
        }

        // Mostrar indicador de progreso y deshabilitar botón
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false

        // Ejecutar la autenticación en un hilo secundario
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // URL fija para Los Vilos
                val baseUrl = "http://iptv.ctvc.cl:80"
                
                // Construir la URL completa para descargar la playlist
                val urlString = "$baseUrl/playlist/$username/$password/m3u_plus?output=hls"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                // Verificar el código de respuesta del servidor
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Leer el contenido de la playlist
                    val inputStream = connection.inputStream
                    val playlistContent = inputStream.bufferedReader().use { it.readText() }
                    inputStream.close()

                    // Guardar la playlist en el almacenamiento interno de la app
                    savePlaylistToFile(playlistContent)

                    // Actualizar UI en el hilo principal
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Login exitoso y playlist guardada")
                        // Guardar las credenciales exitosas para futuras sesiones
                        guardarCredenciales(username, password)
                        showSuccess("Login exitoso")
                        // Navegar a la actividad principal
                        Log.d(TAG, "Iniciando MainActivity")
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
                    // Manejar errores de autenticación
                    withContext(Dispatchers.Main) {
                        Log.w(TAG, "Error en el login, código de respuesta: $responseCode")
                        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                            showError("Usuario o contraseña incorrectos")
                        } else {
                            showError("Error en el login: $responseCode")
                        }
                    }
                }
            } catch (e: IOException) {
                // Manejar errores de red
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error de red durante el login: ${e.message}", e)
                    showError("Error de red. Verifique su conexión.")
                }
            } catch (e: Exception) {
                // Manejar errores inesperados
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error inesperado durante el login: ${e.message}", e)
                    showError("Error inesperado durante el login.")
                }
            } finally {
                // Restaurar UI en el hilo principal
                withContext(Dispatchers.Main) {
                    // Ocultar progreso y habilitar botón
                    binding.progressBar.visibility = View.GONE
                    binding.buttonLogin.isEnabled = true
                }
            }
        }
    }

    /**
     * Guarda el contenido de la playlist en un archivo local.
     * 
     * @param content Contenido de la playlist M3U
     */
    private fun savePlaylistToFile(content: String) {
        try {
            // Crear archivo en el almacenamiento interno de la aplicación
            val file = File(filesDir, "downloaded_playlist.m3u")
            FileOutputStream(file).use {
                it.write(content.toByteArray())
            }
            Log.d(TAG, "Playlist guardada en ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Error al guardar la playlist: ${e.message}", e)
            throw e // Re-lanzar para que el bloque catch principal lo maneje
        }
    }

    /**
     * Actualiza el estado del botón de login basado en el contenido de los campos.
     * El botón solo se habilita cuando ambos campos tienen contenido.
     */
    private fun updateLoginButtonState() {
        try {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            
            // Habilitar botón solo si ambos campos tienen contenido
            binding.buttonLogin.isEnabled = username.isNotEmpty() && password.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error en updateLoginButtonState: ${e.message}", e)
        }
    }

    /**
     * Limpia el error del campo de usuario.
     */
    private fun clearUsernameError() {
        binding.textInputLayoutUsername.error = null
    }

    /**
     * Limpia el error del campo de contraseña.
     */
    private fun clearPasswordError() {
        binding.textInputLayoutPassword.error = null
    }

    /**
     * Muestra un mensaje de error al usuario.
     * 
     * @param message Mensaje de error a mostrar
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Muestra un mensaje de éxito al usuario.
     * 
     * @param message Mensaje de éxito a mostrar
     */
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Guarda las credenciales en SharedPreferences para persistencia.
     * 
     * @param username Nombre de usuario
     * @param password Contraseña
     */
    private fun guardarCredenciales(username: String, password: String) {
        try {
            sharedPreferences.edit().apply {
                putString(KEY_USERNAME, username)
                putString(KEY_PASSWORD, password)
                apply()
            }
            Log.d(TAG, "Credenciales guardadas exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar credenciales: ${e.message}", e)
        }
    }
    
    /**
     * Carga las credenciales guardadas y las coloca en los campos de texto.
     * Se ejecuta al iniciar la actividad para restaurar la sesión anterior.
     */
    private fun cargarCredencialesGuardadas() {
        try {
            val savedUsername = sharedPreferences.getString(KEY_USERNAME, "")
            val savedPassword = sharedPreferences.getString(KEY_PASSWORD, "")
            
            // Restaurar usuario si existe
            if (!savedUsername.isNullOrEmpty()) {
                binding.editTextUsername.setText(savedUsername)
                Log.d(TAG, "Usuario cargado: $savedUsername")
            }
            
            // Restaurar contraseña si existe
            if (!savedPassword.isNullOrEmpty()) {
                binding.editTextPassword.setText(savedPassword)
                Log.d(TAG, "Contraseña cargada")
            }
            
            // Actualizar el estado del botón después de cargar las credenciales
            updateLoginButtonState()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar credenciales guardadas: ${e.message}", e)
        }
    }


    /**
     * Método llamado cuando cambia la configuración del dispositivo.
     * Se ejecuta en lugar de recrear la actividad cuando configChanges está configurado.
     * Esto permite que el layout se adapte correctamente a la nueva orientación.
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuración cambiada - orientación: ${newConfig.orientation}")
        
        // Forzar la recreación del layout para asegurar que se aplique el layout correcto
        try {
            // Guardar el estado actual de los campos
            val currentUsername = binding.editTextUsername.text.toString()
            val currentPassword = binding.editTextPassword.text.toString()
            
            // Reinicializar ViewBinding con el nuevo layout
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Reconfigurar las vistas después del cambio de orientación
            setupViews()
            setupListeners()
            
            // Restaurar el estado de los campos
            binding.editTextUsername.setText(currentUsername)
            binding.editTextPassword.setText(currentPassword)
            
            // Actualizar el estado del botón
            updateLoginButtonState()
            
            Log.d(TAG, "Layout recreado correctamente para nueva orientación")
        } catch (e: Exception) {
            Log.e(TAG, "Error al recrear layout: ${e.message}", e)
        }
    }
} 