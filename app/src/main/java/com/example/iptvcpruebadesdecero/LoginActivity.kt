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
 * El usuario debe ingresar sus credenciales para descargar su lista de reproducción.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val TAG = "LoginActivity"
    
    // SharedPreferences para guardar las credenciales
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "LoginPrefs"
    private val KEY_USERNAME = "saved_username"
    private val KEY_PASSWORD = "saved_password"
    private val KEY_SERVER = "selected_server"
    
    // Servidores disponibles
    private val SERVER_LOS_VILOS = "Los Vilos"
    private val SERVER_LA_SERENA = "La Serena"
    private var currentServer = SERVER_LOS_VILOS // Servidor por defecto
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate iniciado")
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        setupViews()
        setupListeners()
        cargarCredencialesGuardadas()
        cargarServidorGuardado()
        Log.d(TAG, "onCreate completado")
    }

    /**
     * Configura las vistas iniciales
     */
    private fun setupViews() {
        try {
            Log.d(TAG, "Configurando vistas")
            // Configurar el estado inicial del botón de login
            updateLoginButtonState()
            
            // Configurar el estado inicial de los campos de error
            binding.textInputLayoutUsername.error = null
            binding.textInputLayoutPassword.error = null
            Log.d(TAG, "Vistas configuradas correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupViews: ${e.message}", e)
        }
    }

    /**
     * Configura los listeners para los campos de texto y botones
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

            // Listener para el botón de configuración
            binding.buttonConfig.setOnClickListener {
                showServerMenu()
            }

            // TextWatchers para validación en tiempo real
            binding.editTextUsername.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    clearUsernameError()
                    updateLoginButtonState()
                }
            })

            binding.editTextPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    clearPasswordError()
                    updateLoginButtonState()
                }
            })

            // Listener para el Enter en el campo de contraseña
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
     * Muestra el menú desplegable para seleccionar servidor
     */
    private fun showServerMenu() {
        val popupMenu = PopupMenu(this, binding.buttonConfig)
        popupMenu.menuInflater.inflate(R.menu.server_menu, popupMenu.menu)
        
        // Marcar el servidor actual como seleccionado
        when (currentServer) {
            SERVER_LOS_VILOS -> popupMenu.menu.findItem(R.id.menu_server_los_vilos)?.isChecked = true
            SERVER_LA_SERENA -> popupMenu.menu.findItem(R.id.menu_server_la_serena)?.isChecked = true
        }
        

        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_server_los_vilos -> {
                    if (currentServer != SERVER_LOS_VILOS) {
                        currentServer = SERVER_LOS_VILOS
                        guardarServidorSeleccionado()
                        Toast.makeText(this, "Servidor seleccionado: Los Vilos", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.menu_server_la_serena -> {
                    if (currentServer != SERVER_LA_SERENA) {
                        currentServer = SERVER_LA_SERENA
                        guardarServidorSeleccionado()
                        Toast.makeText(this, "Servidor seleccionado: La Serena", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }

    /**
     * Realiza la validación de login
     */
    private fun performLogin() {
        Log.d(TAG, "Iniciando proceso de login")
        val username = binding.editTextUsername.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        // Validar campos vacíos
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

        // Mostrar progreso
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Construir URL según el servidor seleccionado
                val baseUrl = when (currentServer) {
                    SERVER_LOS_VILOS -> "http://iptv.ctvc.cl:80"
                    SERVER_LA_SERENA -> "https://tv.wntv.cl:443"
                    else -> "http://iptv.ctvc.cl:80"
                }
                
                val urlString = "$baseUrl/playlist/$username/$password/m3u_plus?output=hls"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val playlistContent = inputStream.bufferedReader().use { it.readText() }
                    inputStream.close()

                    // Guardar la playlist en el almacenamiento interno
                    savePlaylistToFile(playlistContent)

                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Login exitoso y playlist guardada")
                        // Guardar las credenciales exitosas
                        guardarCredenciales(username, password)
                        showSuccess("Login exitoso")
                        // Navegar a MainActivity
                        Log.d(TAG, "Iniciando MainActivity")
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
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
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error de red durante el login: ${e.message}", e)
                    showError("Error de red. Verifique su conexión.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error inesperado durante el login: ${e.message}", e)
                    showError("Error inesperado durante el login.")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    // Ocultar progreso y habilitar botón
                    binding.progressBar.visibility = View.GONE
                    binding.buttonLogin.isEnabled = true
                }
            }
        }
    }

    private fun savePlaylistToFile(content: String) {
        try {
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
     * Actualiza el estado del botón de login basado en los campos
     */
    private fun updateLoginButtonState() {
        try {
            val username = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            
            binding.buttonLogin.isEnabled = username.isNotEmpty() && password.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error en updateLoginButtonState: ${e.message}", e)
        }
    }

    /**
     * Limpia el error del campo de usuario
     */
    private fun clearUsernameError() {
        binding.textInputLayoutUsername.error = null
    }

    /**
     * Limpia el error del campo de contraseña
     */
    private fun clearPasswordError() {
        binding.textInputLayoutPassword.error = null
    }

    /**
     * Muestra un mensaje de error
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Muestra un mensaje de éxito
     */
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Guarda las credenciales en SharedPreferences
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
     * Carga las credenciales guardadas y las coloca en los campos de texto
     */
    private fun cargarCredencialesGuardadas() {
        try {
            val savedUsername = sharedPreferences.getString(KEY_USERNAME, "")
            val savedPassword = sharedPreferences.getString(KEY_PASSWORD, "")
            
            if (!savedUsername.isNullOrEmpty()) {
                binding.editTextUsername.setText(savedUsername)
                Log.d(TAG, "Usuario cargado: $savedUsername")
            }
            
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
     * Guarda el servidor seleccionado en SharedPreferences
     */
    private fun guardarServidorSeleccionado() {
        try {
            sharedPreferences.edit().apply {
                putString(KEY_SERVER, currentServer)
                apply()
            }
            Log.d(TAG, "Servidor guardado: $currentServer")
            updateServerIndicator()
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar servidor: ${e.message}", e)
        }
    }

    /**
     * Carga el servidor guardado desde SharedPreferences
     */
    private fun cargarServidorGuardado() {
        try {
            val savedServer = sharedPreferences.getString(KEY_SERVER, SERVER_LOS_VILOS)
            currentServer = savedServer ?: SERVER_LOS_VILOS
            Log.d(TAG, "Servidor cargado: $currentServer")
            updateServerIndicator()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar servidor guardado: ${e.message}", e)
            currentServer = SERVER_LOS_VILOS
        }
    }

    /**
     * Actualiza el indicador visual del servidor seleccionado
     */
    private fun updateServerIndicator() {
        // Cambiar el icono del botón según el servidor seleccionado
        val iconRes = when (currentServer) {
            SERVER_LOS_VILOS -> android.R.drawable.ic_menu_manage
            SERVER_LA_SERENA -> android.R.drawable.ic_menu_manage
            else -> android.R.drawable.ic_menu_manage
        }
        binding.buttonConfig.setImageResource(iconRes)
    }
} 