# IPTV App - Aplicación de Televisión por Internet

## 📺 Descripción del Proyecto

Esta es una aplicación Android para reproducir contenido IPTV (Internet Protocol Television). La aplicación permite a los usuarios autenticarse con un servidor IPTV, descargar su lista de canales personalizada y reproducir contenido en tiempo real.

## 🚀 Funcionalidades Principales

### 🔐 Autenticación y Gestión de Usuarios
- **Login seguro**: Autenticación con servidor IPTV usando credenciales de usuario
- **Múltiples servidores**: Soporte para diferentes servidores (Los Vilos, La Serena)
- **Persistencia de datos**: Guardado automático de credenciales para futuras sesiones
- **Validación en tiempo real**: Verificación de campos durante la entrada de datos

### 📋 Gestión de Playlists
- **Descarga automática**: Descarga de playlist M3U desde el servidor
- **Parseo inteligente**: Procesamiento de archivos M3U con información de canales
- **Categorización**: Organización automática de canales por categorías
- **Búsqueda avanzada**: Búsqueda en tiempo real de canales por nombre

### 🎬 Reproductor de Video
- **Reproducción VLC**: Motor de reproducción robusto basado en VLC
- **Controles personalizados**: Interfaz de usuario optimizada para TV/Android TV
- **Navegación entre canales**: Cambio de canal con botones anterior/siguiente
- **Controles de reproducción**: Play/pause, adelantar/retroceder, barra de progreso
- **Modo pantalla completa**: Experiencia inmersiva de visualización

### 🎨 Interfaz de Usuario
- **Diseño adaptativo**: Interfaz optimizada para diferentes tamaños de pantalla
- **Animaciones Lottie**: Animaciones fluidas en splash y carga
- **Carga de logos**: Sistema inteligente de búsqueda y carga de logos de canales
- **Navegación táctil**: Controles táctiles para mostrar/ocultar interfaz

## 🏗️ Arquitectura del Proyecto

### Patrón MVVM (Model-View-ViewModel)
La aplicación sigue el patrón MVVM para una separación clara de responsabilidades:

- **Model**: Clases de datos (`Canal`, `Categoria`)
- **View**: Actividades y adaptadores (`MainActivity`, `LoginActivity`, `PlayerActivity`)
- **ViewModel**: Lógica de negocio (`MainViewModel`)

### Estructura de Paquetes
```
com.example.iptvcpruebadesdecero/
├── adapter/           # Adaptadores para RecyclerView
├── model/            # Clases de datos
├── util/             # Utilidades (M3UParser)
├── viewmodel/        # ViewModels
└── [Activities]      # Actividades principales
```

## 📱 Flujo de la Aplicación

### 1. SplashActivity
- **Duración**: 7 segundos
- **Animación**: Lottie con archivo "InicioTv2.json"
- **Función**: Pantalla de bienvenida y carga inicial

### 2. LoginActivity
- **Autenticación**: Validación de credenciales con servidor IPTV
- **Servidores**: Selección entre Los Vilos y La Serena
- **Descarga**: Descarga de playlist M3U personalizada
- **Persistencia**: Guardado de credenciales en SharedPreferences

### 3. MainActivity
- **Categorías**: Lista vertical de categorías de canales
- **Canales**: Lista horizontal de canales por categoría
- **Búsqueda**: Búsqueda en tiempo real de canales
- **Navegación**: Transición al reproductor de video

### 4. PlayerActivity
- **Reproducción**: Motor VLC para streams IPTV
- **Controles**: Interfaz personalizada con controles táctiles
- **Navegación**: Cambio entre canales desde el reproductor
- **Pantalla completa**: Modo inmersivo para mejor experiencia

## 🔧 Componentes Técnicos

### M3UParser
Clase especializada en el parseo de archivos M3U:
- **Parseo de streams**: Extracción de URLs y metadatos de canales
- **Búsqueda de logos**: Sistema inteligente de búsqueda de logos
- **Optimización**: Índices para búsquedas rápidas
- **Soporte local**: Logos desde assets y URLs remotas

### Adaptadores
- **CategoriaAdapter**: Maneja la lista vertical de categorías
- **CanalAdapter**: Maneja la lista horizontal de canales
- **Selección visual**: Indicadores de selección y navegación

### ViewModel
- **Estado reactivo**: LiveData para actualizaciones automáticas de UI
- **Búsqueda**: Filtrado en tiempo real de canales
- **Persistencia**: Mantenimiento de datos durante cambios de configuración

## 🎯 Características Destacadas

### Sistema de Logos Inteligente
1. **Prioridad local**: Búsqueda en assets/logos/ primero
2. **Búsqueda exacta**: Coincidencia exacta de nombres
3. **Búsqueda flexible**: Variaciones de nombres (minúsculas, mayúsculas, etc.)
4. **Búsqueda por palabras clave**: Para nombres largos
5. **Similitud de nombres**: Algoritmo de Levenshtein para nombres similares

### Optimización de Rendimiento
- **Carga lazy**: Logos cargados solo cuando necesario
- **Cache de imágenes**: Glide para optimización de memoria
- **Índices de búsqueda**: Búsquedas rápidas de logos
- **Hilos secundarios**: Operaciones pesadas en background

### Experiencia de Usuario
- **Navegación intuitiva**: Controles adaptados para TV/Android TV
- **Feedback visual**: Indicadores de carga y errores
- **Persistencia**: Recuerdo de preferencias del usuario
- **Manejo de errores**: Mensajes claros y recuperación automática

## 📋 Requisitos Técnicos

### Dependencias Principales
- **VLC**: Motor de reproducción multimedia
- **Glide**: Carga y cache de imágenes
- **Lottie**: Animaciones vectoriales
- **ViewBinding**: Acceso seguro a vistas
- **LiveData**: Datos reactivos
- **Coroutines**: Programación asíncrona

### Configuración de Red
- **HTTP/HTTPS**: Soporte para diferentes protocolos
- **Timeouts**: Manejo de conexiones lentas
- **Errores de red**: Recuperación automática

## 🔒 Seguridad

### Autenticación
- **Credenciales seguras**: Almacenamiento en SharedPreferences
- **Validación**: Verificación de campos en tiempo real
- **Manejo de errores**: Mensajes claros para problemas de autenticación

### Datos
- **Almacenamiento interno**: Playlists guardadas en almacenamiento privado
- **Sin datos sensibles**: No se almacenan contraseñas en texto plano

## 🚀 Instalación y Uso

### Para Desarrolladores
1. Clonar el repositorio
2. Abrir en Android Studio
3. Configurar las dependencias en `build.gradle.kts`
4. Sincronizar el proyecto
5. Ejecutar en dispositivo/emulador

### Para Usuarios Finales
1. Instalar la aplicación APK
2. Abrir la aplicación
3. Seleccionar servidor (Los Vilos o La Serena)
4. Ingresar credenciales IPTV
5. Navegar y reproducir canales

## 📊 Estructura de Datos

### Canal
```kotlin
data class Canal(
    val id: String,           // Identificador único
    val nombre: String,       // Nombre del canal
    val url: String,          // URL del stream
    val logo: String?,        // URL del logo (opcional)
    val categoria: String     // Categoría del canal
)
```

### Categoria
```kotlin
data class Categoria(
    val nombre: String,                    // Nombre de la categoría
    val canales: MutableList<Canal>       // Lista de canales
)
```

## 🎨 Interfaz de Usuario

### Diseño Material
- **Colores**: Paleta de colores consistente
- **Tipografía**: Jerarquía visual clara
- **Espaciado**: Diseño limpio y espacioso
- **Iconografía**: Iconos intuitivos

### Responsive Design
- **Adaptativo**: Funciona en diferentes tamaños de pantalla
- **TV Optimizado**: Interfaz optimizada para Android TV
- **Navegación**: Controles adaptados para control remoto

## 🔄 Flujo de Datos

1. **Login** → Descarga playlist M3U
2. **Parseo** → Convierte M3U en objetos Canal/Categoria
3. **Búsqueda de logos** → Encuentra logos apropiados
4. **UI** → Muestra categorías y canales
5. **Selección** → Usuario selecciona canal
6. **Reproducción** → VLC reproduce el stream

## 🛠️ Mantenimiento

### Logs
- **TAGs consistentes**: Cada clase tiene su TAG para debugging
- **Niveles apropiados**: DEBUG, INFO, WARNING, ERROR
- **Información útil**: Contexto para debugging

### Manejo de Errores
- **Try-catch**: Manejo robusto de excepciones
- **Mensajes de usuario**: Errores claros para el usuario final
- **Recuperación**: Intentos automáticos cuando es posible

## 📈 Futuras Mejoras

### Funcionalidades Planificadas
- **Favoritos**: Sistema de canales favoritos
- **EPG**: Guía de programación electrónica
- **Grabación**: Funcionalidad de grabación de programas
- **Múltiples perfiles**: Soporte para múltiples usuarios
- **Temas**: Diferentes temas visuales

### Optimizaciones Técnicas
- **Cache offline**: Reproducción sin conexión
- **Calidad adaptativa**: Cambio automático de calidad
- **Analytics**: Métricas de uso
- **Tests**: Cobertura de pruebas unitarias

---

## 📞 Soporte

Para soporte técnico o preguntas sobre el desarrollo, contactar al equipo de desarrollo.

## 📄 Licencia

Este proyecto es propiedad privada y está destinado para uso interno. 