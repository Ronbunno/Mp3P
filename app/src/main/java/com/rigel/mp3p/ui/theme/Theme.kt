package com.rigel.mp3p.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PurpleColorScheme = darkColorScheme(
    primary = PurplePrimary,
    secondary = PurpleSecondary,
    background = PurpleBackground,
    surface = PurpleSurface,
    surfaceContainerHigh = PurpleSurfaceHigh,
    onPrimary = OnSurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceMuted
)

private val CyanColorScheme = darkColorScheme(
    primary = CyanPrimary,
    secondary = CyanSecondary,
    background = CyanBackground,
    surface = CyanSurface,
    surfaceContainerHigh = CyanSurfaceHigh,
    onPrimary = OnSurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceMuted
)

private val CoralColorScheme = darkColorScheme(
    primary = CoralPrimary,
    secondary = CoralSecondary,
    background = CoralBackground,
    surface = CoralSurface,
    surfaceContainerHigh = CoralSurfaceHigh,
    onPrimary = OnSurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceMuted
)

private val EmeraldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    background = EmeraldBackground,
    surface = EmeraldSurface,
    surfaceContainerHigh = EmeraldSurfaceHigh,
    onPrimary = OnSurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceMuted
)

@Composable
fun Mp3PTheme(
    appTheme: Int = 0,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        1 -> CyanColorScheme
        2 -> CoralColorScheme
        3 -> EmeraldColorScheme
        else -> PurpleColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
