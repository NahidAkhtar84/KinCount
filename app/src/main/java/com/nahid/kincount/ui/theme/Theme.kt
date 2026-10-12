package com.nahid.kincount.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006D3B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF93F8B2),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF4F6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8D7),
    onSecondaryContainer = Color(0xFF0C1F14),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC0E9FB),
    onTertiaryContainer = Color(0xFF001F2A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF77Dc97),
    onPrimary = Color(0xFF00391D),
    primaryContainer = Color(0xFF00522C),
    onPrimaryContainer = Color(0xFF93F8B2),
    secondary = Color(0xFFB7CCBB),
    onSecondary = Color(0xFF223528),
    secondaryContainer = Color(0xFF384B3E),
    onSecondaryContainer = Color(0xFFD2E8D7),
    tertiary = Color(0xFFA4CDDF),
    onTertiary = Color(0xFF053545),
    tertiaryContainer = Color(0xFF244B5C),
    onTertiaryContainer = Color(0xFFC0E9FB),
)

@Composable
fun KinCountTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
