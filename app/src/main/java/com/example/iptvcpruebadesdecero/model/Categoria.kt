package com.example.iptvcpruebadesdecero.model

/**
 * Clase de datos que representa una categoría de canales IPTV.
 * Esta clase se utiliza para agrupar canales relacionados en una misma categoría.
 * Permite organizar los canales de manera jerárquica para una mejor experiencia de usuario.
 * 
 * @property nombre Nombre de la categoría (ej: "Deportes", "Películas", "Series", "Noticias", etc.)
 * @property canales Lista mutable de canales que pertenecen a esta categoría.
 *                  Se inicializa como una lista vacía por defecto.
 *                  Es mutable para permitir agregar canales durante el parseo del archivo M3U.
 */
data class Categoria(
    val nombre: String,
    val canales: MutableList<Canal> = mutableListOf()
) 