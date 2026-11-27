package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF132D63),
    onPrimaryContainer = JarvisCyanGlow,
    secondary = JarvisPurpleEnd,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1B5E),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = JarvisGold,
    onTertiary = JarvisBgDeep,
    tertiaryContainer = JarvisGoldContainer,
    onTertiaryContainer = JarvisGold,
    background = JarvisBgDeep,
    onBackground = JarvisTextPrimary,
    surface = JarvisCardNavy,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisCardNavyElevated,
    onSurfaceVariant = JarvisTextSecondary,
    error = JarvisCoralRed,
    onError = Color.White,
    outline = JarvisBlueBorder
)

private val JarvisLightColorScheme = lightColorScheme(
    primary = ScholarIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3F8),
    onPrimaryContainer = ScholarIndigo,
    secondary = Color(0xFF6D28D9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = IvoryBackground,
    onBackground = InkPrimary,
    surface = IvorySurface,
    onSurface = InkPrimary,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = InkSecondary,
    error = CoralError,
    onError = Color.White,
    outline = Color(0xFF64748B)
)

val StudyPortalShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun StudyPortalTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) JarvisDarkColorScheme else JarvisLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudyPortalTypography,
        shapes = StudyPortalShapes,
        content = content
    )
}
