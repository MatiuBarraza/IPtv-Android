package com.example.iptvcpruebadesdecero

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

/**
 * Actividad de splash que se muestra al iniciar la aplicación.
 * Muestra una animación Lottie durante un tiempo determinado antes de navegar
 * a la actividad de login.
 * 
 * Funcionalidades:
 * - Animación de carga con Lottie
 * - Timer automático para navegar al login
 * - Manejo de errores de animación
 */
class SplashActivity : AppCompatActivity() {
    // Referencia a la vista de animación Lottie
    private lateinit var lottieAnimationView: LottieAnimationView
    // Handler para programar tareas en el hilo principal
    private val handler = Handler(Looper.getMainLooper())
    // Duración del splash en milisegundos (7 segundos)
    private val splashDuration = 7000L
    private val TAG = "SplashActivity"

    /**
     * Método de inicialización de la actividad.
     * Configura la animación y programa la navegación automática.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Obtener referencia a la vista de animación
        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        setupLottieAnimation()
    }

    /**
     * Configura la animación Lottie y programa la navegación automática.
     * Carga el archivo de animación y lo reproduce durante el tiempo especificado.
     */
    private fun setupLottieAnimation() {
        try {
            Log.d(TAG, "Iniciando carga de animación")
            
            // Cargar y reproducir la animación Lottie
            lottieAnimationView.setAnimation("InicioTv2.json")
            lottieAnimationView.playAnimation()
            
            // Programar la navegación automática después del tiempo especificado
            handler.postDelayed({
                startLoginActivity()
            }, splashDuration)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar la animación: ${e.message}", e)
            // Si hay error en la animación, navegar inmediatamente
            startLoginActivity()
        }
    }

    /**
     * Inicia la actividad de login y finaliza la actividad actual.
     * Se ejecuta automáticamente después del tiempo del splash o si hay error.
     */
    private fun startLoginActivity() {
        Log.d(TAG, "Iniciando LoginActivity")
        startActivity(Intent(this, LoginActivity::class.java))
        finish() // Finalizar la actividad de splash
    }

    /**
     * Método llamado cuando la actividad se destruye.
     * Limpia los recursos para evitar memory leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        // Remover todos los callbacks programados
        handler.removeCallbacksAndMessages(null)
        // Cancelar la animación si está activa
        if (::lottieAnimationView.isInitialized) {
            lottieAnimationView.cancelAnimation()
        }
    }
} 