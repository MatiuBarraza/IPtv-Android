package com.example.iptvcpruebadesdecero.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvcpruebadesdecero.model.Canal
import com.example.iptvcpruebadesdecero.model.Categoria
import com.example.iptvcpruebadesdecero.util.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel principal de la aplicación que maneja la lógica de negocio relacionada con la playlist IPTV.
 * Implementa el patrón MVVM (Model-View-ViewModel) para separar la lógica de la interfaz de usuario.
 * 
 * Funcionalidades principales:
 * - Carga y parseo de playlist M3U
 * - Búsqueda de canales en tiempo real
 * - Gestión de estado de la aplicación
 * - Manejo de errores
 * - Persistencia de datos durante cambios de configuración
 */
class MainViewModel : ViewModel() {
    /**
     * LiveData mutable que contiene la lista de categorías de la playlist.
     * Solo se puede modificar desde dentro del ViewModel.
     * Los observadores pueden ver los cambios automáticamente.
     */
    private val _categorias = MutableLiveData<List<Categoria>>()
    
    /**
     * LiveData público que expone la lista de categorías a la UI.
     * Los observadores solo pueden leer este valor, no modificarlo.
     * Esto garantiza la inmutabilidad de los datos desde la UI.
     */
    val categorias: LiveData<List<Categoria>> = _categorias

    /**
     * LiveData mutable para manejar mensajes de error.
     * Solo se puede modificar desde dentro del ViewModel.
     * Se usa para comunicar errores a la UI de forma reactiva.
     */
    private val _error = MutableLiveData<String>()
    
    /**
     * LiveData público que expone los mensajes de error a la UI.
     * Los observadores solo pueden leer este valor.
     */
    val error: LiveData<String> = _error

    // Lista original de categorías para mantener los datos sin filtrar
    // Se usa para restaurar la lista completa cuando se limpia la búsqueda
    private var categoriasOriginales: List<Categoria> = emptyList()

    /**
     * Busca canales que coincidan con el texto de búsqueda.
     * La búsqueda se realiza en el nombre del canal y es insensible a mayúsculas/minúsculas.
     * Filtra las categorías para mostrar solo aquellas que contengan canales que coincidan.
     * 
     * @param query Texto de búsqueda (puede estar vacío para restaurar la lista completa)
     */
    fun buscarCanales(query: String) {
        if (query.isEmpty()) {
            // Si la búsqueda está vacía, restaurar la lista original completa
            _categorias.value = categoriasOriginales
            return
        }

        // Filtrar canales que coincidan con la búsqueda
        val categoriasFiltradas = categoriasOriginales.map { categoria ->
            // Filtrar canales dentro de cada categoría
            val canalesFiltrados = categoria.canales.filter { canal ->
                canal.nombre.contains(query, ignoreCase = true)
            }
            // Solo incluir categorías que tengan canales después del filtrado
            if (canalesFiltrados.isNotEmpty()) {
                categoria.copy(canales = canalesFiltrados.toMutableList())
            } else null
        }.filterNotNull() // Eliminar categorías vacías

        // Actualizar el LiveData con las categorías filtradas
        _categorias.value = categoriasFiltradas
    }

    /**
     * Carga la playlist IPTV desde el almacenamiento interno de la aplicación.
     * Este método realiza las siguientes operaciones:
     * 1. Abre el archivo M3U ("downloaded_playlist.m3u") guardado en el login
     * 2. Parsea el contenido usando M3UParser
     * 3. Actualiza el LiveData con las categorías encontradas
     * 4. Maneja los errores que puedan ocurrir durante el proceso
     *
     * @param context Contexto de la aplicación necesario para acceder a los archivos internos
     */
    fun cargarPlaylist(context: Context) {
        // Usar viewModelScope para manejar el ciclo de vida del ViewModel
        viewModelScope.launch {
            try {
                // Inicio del proceso de carga
                Log.d("MainViewModel", "Iniciando carga de playlist desde almacenamiento interno")
                
                // Abrir el archivo M3U guardado durante el login
                val inputStream = context.openFileInput("downloaded_playlist.m3u")
                Log.d("MainViewModel", "Archivo de playlist encontrado en almacenamiento interno")
                
                // Creación del parser y proceso de parseo en un hilo secundario
                val parser = M3UParser()
                val categoriasParseadas = withContext(Dispatchers.IO) {
                    Log.d("MainViewModel", "Iniciando parseo de playlist")
                    val resultado = parser.parse(inputStream, context)
                    Log.d("MainViewModel", "Playlist parseada. Categorías encontradas: ${resultado.size}")
                    resultado
                }
                
                // Verificación de resultados y actualización del estado
                if (categoriasParseadas.isEmpty()) {
                    Log.w("MainViewModel", "No se encontraron categorías en la playlist")
                    _error.postValue("No se encontraron canales en la playlist")
                } else {
                    Log.d("MainViewModel", "Actualizando LiveData con ${categoriasParseadas.size} categorías")
                    // Guardar las categorías originales para búsquedas posteriores
                    categoriasOriginales = categoriasParseadas
                    // Actualizar el LiveData en el hilo principal
                    _categorias.postValue(categoriasParseadas)
                }
            } catch (e: Exception) {
                // Manejo de errores durante el proceso de carga
                Log.e("MainViewModel", "Error al cargar la playlist: ${e.message}", e)
                _error.postValue("Error al cargar la playlist: ${e.message}")
            }
        }
    }
} 