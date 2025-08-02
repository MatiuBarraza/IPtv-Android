package com.example.iptvcpruebadesdecero.model

import java.io.Serializable

/**
 * Clase de datos que representa un canal de IPTV.
 * Esta clase se utiliza para almacenar y transportar la información de cada canal.
 * Implementa Serializable para permitir el paso de datos entre actividades.
 * 
 * @property id Identificador único del canal (usado para comparaciones y búsquedas)
 * @property nombre Nombre o título del canal (ej: "Canal 13 HD", "ESPN")
 * @property url URL del stream del canal (formato HLS, M3U8, etc.)
 * @property logo URL opcional del logo del canal (puede ser null si no hay logo disponible)
 * @property categoria Categoría a la que pertenece el canal (ej: "Deportes", "Películas", "Series")
 */
data class Canal(
    val id: String,
    val nombre: String,
    val url: String,
    val logo: String?,
    val categoria: String
) : Serializable 