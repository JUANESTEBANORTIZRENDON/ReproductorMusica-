package com.juan.reproductormusica.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Esquema de colores oscuro mejorado para el reproductor de música
 * Paleta optimizada para mejor contraste y experiencia visual
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE53935),           // Rojo vibrante principal
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB71C1C),  // Rojo oscuro para contenedores
    onPrimaryContainer = Color.White,
    
    secondary = Color(0xFFFF5722),         // Naranja-rojo complementario
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD84315),
    onSecondaryContainer = Color.White,
    
    tertiary = Color(0xFFFF8A65),          // Acento cálido
    onTertiary = Color(0xFF1C1C1C),
    
    background = Color(0xFF0A0A0A),        // Negro profundo
    onBackground = Color(0xFFF5F5F5),      // Blanco suave
    
    surface = Color(0xFF1C1C1C),           // Superficie gris oscuro
    onSurface = Color(0xFFE0E0E0),         // Texto principal
    surfaceVariant = Color(0xFF2C2C2C),    // Superficie alternativa
    onSurfaceVariant = Color(0xFFBDBDBD),  // Texto secundario
    
    outline = Color(0xFF616161),           // Bordes y divisores
    outlineVariant = Color(0xFF424242),
    
    error = Color(0xFFCF6679),             // Error suave
    onError = Color.White,
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color.White
)

/**
 * Esquema de colores claro (para dynamic theming)
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD32F2F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = Color(0xFFB71C1C),
    
    secondary = Color(0xFFFF5722),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = Color(0xFFD84315),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242)
)

/**
 * Tema principal del reproductor de música con soporte para:
 * - Tema base personalizado (por defecto)
 * - Dynamic Color de Material 3 (opcional)
 * - Modo claro/oscuro
 */
@Composable
fun ReproductorMusicaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false, // Por defecto usa el tema personalizado
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic Color (Material You) - Solo en Android 12+
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Tema personalizado (por defecto)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Variante del tema que fuerza el uso del tema personalizado oscuro
 * Útil para mantener consistencia visual en ciertas pantallas
 */
@Composable
fun ReproductorMusicaThemeDark(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
