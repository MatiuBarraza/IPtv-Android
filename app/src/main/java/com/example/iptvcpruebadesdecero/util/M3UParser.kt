package com.example.iptvcpruebadesdecero.util

import android.content.Context
import android.util.Log
import com.example.iptvcpruebadesdecero.model.Canal
import com.example.iptvcpruebadesdecero.model.Categoria
import java.io.BufferedReader
import java.io.InputStreamReader
import org.json.JSONArray
import org.json.JSONObject

/**
 * Clase encargada de parsear archivos M3U (formato de lista de reproducción IPTV).
 * Convierte el contenido del archivo M3U en una lista de categorías con sus respectivos canales.
 */
class M3UParser {
    private val TAG = "M3UParser"

    // Clase de datos para representar un canal del JSON
    private data class CanalInfo(
        val name: String,
        val logo: String,
        val country: String
    )

    // Índices para búsquedas rápidas
    private data class IndicesCanales(
        val porNombreExacto: Map<String, CanalInfo>,
        val porNombreFlexible: Map<String, CanalInfo>,
        val canalesChile: List<CanalInfo>,
        val todosLosCanales: List<CanalInfo>
    )

    /**
     * Parsea un archivo M3U desde un InputStream y lo convierte en una lista de categorías.
     * Ahora recibe el contexto para poder acceder a assets y buscar logos en channels.json
     * 
     * @param inputStream El stream de entrada que contiene el archivo M3U
     * @param context Contexto para acceder a assets
     * @return Lista de categorías con sus canales correspondientes
     * @throws Exception Si ocurre un error al leer o parsear el archivo
     */
    fun parse(inputStream: java.io.InputStream, context: Context): List<Categoria> {
        // Inicialización de variables para el proceso de parseo
        val reader = BufferedReader(InputStreamReader(inputStream))
        val categorias = mutableMapOf<String, Categoria>() // Mapa para almacenar categorías únicas
        var currentCanal: Canal? = null // Variable temporal para almacenar el canal que se está procesando
        var lineCount = 0 // Contador de líneas para el manejo de errores

        // Cargar información de canales del JSON una sola vez
        val indices = cargarCanalesInfo(context)

        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    lineCount++
                    try {
                        when {
                            // Línea que contiene la información del canal (EXTINF)
                            line.startsWith("#EXTINF:") -> {
                                // Extracción de información del canal desde la línea EXTINF
                                val nombre = line.substringAfter(",").trim()
                                val categoria = line.substringAfter("group-title=\"").substringBefore("\"").trim()
                                val id = line.substringAfter("tvg-id=\"").substringBefore("\"").trim()
                                val logo = line.substringAfter("tvg-logo=\"").substringBefore("\"").trim()

                                Log.d(TAG, "Parseando canal: $nombre, categoría: $categoria")

                                // Prioridad especial para GOL TV: siempre usar logo local si existe
                                val logoFinal = if (nombre.equals("GOL TV", ignoreCase = true)) {
                                    buscarLogoLocal(nombre, context) ?: if (logo.isNotEmpty()) logo else buscarLogoLocalPrimero(nombre, indices, context)
                                } else if (logo.isNotEmpty()) {
                                    logo
                                } else {
                                    buscarLogoLocalPrimero(nombre, indices, context)
                                }
                                
                                // Creación del objeto Canal con la información extraída
                                currentCanal = Canal(
                                    id = id,
                                    nombre = nombre,
                                    url = "", // La URL se agregará en la siguiente línea
                                    logo = logoFinal,
                                    categoria = categoria
                                )
                            }
                            // Línea que contiene la URL del stream del canal
                            !line.startsWith("#") && line.isNotBlank() -> {
                                currentCanal?.let { canal ->
                                    // Usar la URL directamente sin modificarla
                                    val url = line.trim()
                                    
                                    // Creación del canal completo con su URL
                                    val canalCompleto = canal.copy(url = url)
                                    // Obtención o creación de la categoría correspondiente
                                    val categoria = categorias.getOrPut(canal.categoria) { Categoria(canal.categoria) }
                                    // Agregar el canal a su categoría
                                    categoria.canales.add(canalCompleto)
                                    Log.d(TAG, "Canal agregado: ${canal.nombre} a categoría: ${canal.categoria} con URL: $url")
                                }
                                currentCanal = null // Resetear el canal actual
                            }
                        }
                    } catch (e: Exception) {
                        // Manejo de errores para cada línea individual
                        Log.e(TAG, "Error al parsear línea $lineCount: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            // Manejo de errores generales del proceso de lectura
            Log.e(TAG, "Error al leer el archivo: ${e.message}", e)
            throw e
        }

        // Log final con el resultado del proceso
        Log.d(TAG, "Parseo completado. Categorías encontradas: ${categorias.size}")
        // Mostrar la cantidad total de canales
        val totalCanales = categorias.values.sumOf { it.canales.size }
        Log.d(TAG, "Cantidad total de canales procesados: ${totalCanales}")
        return categorias.values.toList()
    }

    /**
     * Carga la información de canales desde el archivo channels.json y crea índices para búsquedas rápidas
     * @param context Contexto para acceder a assets
     * @return Índices optimizados para búsquedas rápidas
     */
    private fun cargarCanalesInfo(context: Context): IndicesCanales {
        val canalesInfo = mutableListOf<CanalInfo>()
        val porNombreExacto = mutableMapOf<String, CanalInfo>()
        val porNombreFlexible = mutableMapOf<String, CanalInfo>()
        val canalesChile = mutableListOf<CanalInfo>()
        
        try {
            val input = context.assets.open("channels.json")
            val json = input.bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.getString("name").trim()
                val logo = obj.optString("logo", "")
                val country = obj.optString("country", "")
                
                if (logo.isNotEmpty()) {
                    val canalInfo = CanalInfo(name, logo, country)
                    canalesInfo.add(canalInfo)
                    
                    // Crear índices para búsquedas rápidas
                    val nombreLower = name.lowercase()
                    porNombreExacto[nombreLower] = canalInfo
                    
                    val nombreFlexible = nombreLower.replace(Regex("[^a-z0-9]"), "")
                    porNombreFlexible[nombreFlexible] = canalInfo
                    
                    if (country == "CL") {
                        canalesChile.add(canalInfo)
                    }
                }
            }
            Log.d(TAG, "Cargados ${canalesInfo.size} canales con logos desde channels.json")
            Log.d(TAG, "Índices creados: ${porNombreExacto.size} nombres exactos, ${porNombreFlexible.size} nombres flexibles, ${canalesChile.size} canales de Chile")
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando información de canales: ${e.message}")
        }
        
        return IndicesCanales(porNombreExacto, porNombreFlexible, canalesChile, canalesInfo)
    }

    /**
     * Busca el logo de un canal, priorizando logos locales de assets/logos/ primero
     * @param nombreCanal Nombre del canal a buscar
     * @param indices Índices optimizados para búsquedas rápidas
     * @param context Contexto para acceder a assets
     * @return URL del logo encontrado o null si no se encuentra
     */
    private fun buscarLogoLocalPrimero(nombreCanal: String, indices: IndicesCanales, context: Context): String? {
        val nombreNormalizado = nombreCanal.trim().lowercase()
        
        // 1. PRIORIDAD MÁXIMA: Buscar en assets/logos/ primero
        val logoLocal = buscarLogoLocal(nombreCanal, context)
        if (logoLocal != null) {
            Log.d(TAG, "Logo local encontrado para '$nombreCanal': $logoLocal")
            return logoLocal
        }
        
        // 2. Búsqueda exacta en Chile (usando índice)
        val canalChile = indices.canalesChile.find { 
            it.name.lowercase() == nombreNormalizado 
        }
        if (canalChile != null) {
            Log.d(TAG, "Logo encontrado para '$nombreCanal' (Chile, exacto): ${canalChile.logo}")
            return canalChile.logo
        }
        
        // 3. Búsqueda exacta en cualquier país (usando índice)
        val canalExacto = indices.porNombreExacto[nombreNormalizado]
        if (canalExacto != null) {
            Log.d(TAG, "Logo encontrado para '$nombreCanal' (${canalExacto.country}, exacto): ${canalExacto.logo}")
            return canalExacto.logo
        }
        
        // 4. Búsqueda flexible (usando índice)
        val nombreFlexible = nombreNormalizado.replace(Regex("[^a-z0-9]"), "")
        val canalFlexible = indices.porNombreFlexible[nombreFlexible]
        if (canalFlexible != null) {
            Log.d(TAG, "Logo encontrado para '$nombreCanal' (${canalFlexible.country}, flexible): ${canalFlexible.logo}")
            return canalFlexible.logo
        }
        
        // 5. Búsqueda por palabras clave más restrictiva
        val palabrasClave = nombreNormalizado.split(Regex("\\s+")).filter { it.length > 2 }
        if (palabrasClave.isNotEmpty()) {
            // Buscar en Chile por palabras clave con coincidencia más específica
            val canalChilePalabras = indices.canalesChile.find { canal ->
                val nombreCanalLower = canal.name.lowercase()
                // Verificar que al menos 2 palabras clave coincidan
                val coincidencias = palabrasClave.count { palabra ->
                    nombreCanalLower.contains(palabra)
                }
                coincidencias >= 2 && coincidencias >= palabrasClave.size * 0.7 // Al menos 70% de las palabras
            }
            if (canalChilePalabras != null) {
                Log.d(TAG, "Logo encontrado para '$nombreCanal' (Chile, palabras clave): ${canalChilePalabras.logo}")
                return canalChilePalabras.logo
            }
            
            // Buscar cualquier canal por palabras clave con criterios más estrictos
            val canalCualquieraPalabras = indices.todosLosCanales.take(50).find { canal ->
                val nombreCanalLower = canal.name.lowercase()
                // Verificar que al menos 2 palabras clave coincidan y que sea una coincidencia significativa
                val coincidencias = palabrasClave.count { palabra ->
                    nombreCanalLower.contains(palabra)
                }
                val porcentajeCoincidencia = coincidencias.toFloat() / palabrasClave.size
                coincidencias >= 2 && porcentajeCoincidencia >= 0.8 // Al menos 80% de las palabras
            }
            if (canalCualquieraPalabras != null) {
                Log.d(TAG, "Logo encontrado para '$nombreCanal' (${canalCualquieraPalabras.country}, palabras clave): ${canalCualquieraPalabras.logo}")
                return canalCualquieraPalabras.logo
            }
        }
        
        // 6. Búsqueda por similitud de nombre (solo para casos muy específicos)
        if (nombreNormalizado.length > 5) {
            val canalSimilar = indices.canalesChile.find { canal ->
                val nombreCanalLower = canal.name.lowercase()
                // Verificar si los nombres son muy similares (diferencia de máximo 2 caracteres)
                val distancia = calcularDistanciaLevenshtein(nombreNormalizado, nombreCanalLower)
                distancia <= 2 && distancia <= nombreNormalizado.length * 0.3
            }
            if (canalSimilar != null) {
                Log.d(TAG, "Logo encontrado para '$nombreCanal' (Chile, similitud): ${canalSimilar.logo}")
                return canalSimilar.logo
            }
        }
        
        Log.d(TAG, "No se encontró logo para '$nombreCanal'")
        return null
    }
    
    /**
     * Busca logo local en assets/logos/ con múltiples estrategias de búsqueda
     * @param nombreCanal Nombre del canal a buscar
     * @param context Contexto para acceder a assets
     * @return URL del logo local encontrado o null si no se encuentra
     */
    private fun buscarLogoLocal(nombreCanal: String, context: Context): String? {
        val assetManager = context.assets
        val posiblesExtensiones = listOf(".png", ".jpg", ".jpeg")
        val nombreNormalizado = nombreCanal.trim()
        
        // Estrategia 1: Búsqueda exacta
        for (ext in posiblesExtensiones) {
            val assetPath = "logos/$nombreNormalizado$ext"
            try {
                assetManager.open(assetPath).close()
                Log.d(TAG, "Logo local encontrado (exacto): $assetPath")
                return "asset:///$assetPath"
            } catch (_: Exception) {}
        }
        
        // Estrategia 2: Búsqueda sin espacios ni caracteres especiales
        val nombreLimpio = nombreNormalizado.replace(Regex("[^a-zA-Z0-9]"), "")
        for (ext in posiblesExtensiones) {
            val assetPath = "logos/$nombreLimpio$ext"
            try {
                assetManager.open(assetPath).close()
                Log.d(TAG, "Logo local encontrado (limpio): $assetPath")
                return "asset:///$assetPath"
            } catch (_: Exception) {}
        }
        
        // Estrategia 3: Búsqueda en minúsculas
        val nombreLower = nombreNormalizado.lowercase()
        for (ext in posiblesExtensiones) {
            val assetPath = "logos/$nombreLower$ext"
            try {
                assetManager.open(assetPath).close()
                Log.d(TAG, "Logo local encontrado (minúsculas): $assetPath")
                return "asset:///$assetPath"
            } catch (_: Exception) {}
        }
        
        // Estrategia 4: Búsqueda por palabras clave (para nombres largos)
        val palabras = nombreNormalizado.split(Regex("\\s+")).filter { it.length > 2 }
        if (palabras.size > 1) {
            for (palabra in palabras) {
                for (ext in posiblesExtensiones) {
                    val assetPath = "logos/$palabra$ext"
                    try {
                        assetManager.open(assetPath).close()
                        Log.d(TAG, "Logo local encontrado (palabra clave): $assetPath")
                        return "asset:///$assetPath"
                    } catch (_: Exception) {}
                }
            }
        }
        
        return null
    }

    /**
     * Calcula la distancia de Levenshtein entre dos strings
     * @param s1 Primer string
     * @param s2 Segundo string
     * @return Distancia de Levenshtein
     */
    private fun calcularDistanciaLevenshtein(s1: String, s2: String): Int {
        val matrix = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) {
            matrix[i][0] = i
        }
        for (j in 0..s2.length) {
            matrix[0][j] = j
        }
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // eliminación
                    matrix[i][j - 1] + 1,      // inserción
                    matrix[i - 1][j - 1] + cost // sustitución
                )
            }
        }
        
        return matrix[s1.length][s2.length]
    }
} 