package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ElectricTealBright,
    onPrimary = ScholarNavySurface,
    primaryContainer = ElectricTealDarkContainer,
    onPrimaryContainer = ElectricTealContainer,
    secondary = WarmAmberBright,
    onSecondary = ScholarNavySurface,
    secondaryContainer = WarmAmberDarkContainer,
    onSecondaryContainer = WarmAmberContainer,
    tertiary = Color(0xFF60A5FA),
    onTertiary = ScholarNavySurface,
    background = ScholarNavySurface,
    onBackground = LightInk,
    surface = ScholarDarkCard,
    onSurface = LightInk,
    surfaceVariant = ScholarDarkCardElevated,
    onSurfaceVariant = LightInkMuted,
    error = CoralErrorBright,
    onError = ScholarNavySurface,
    outline = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = ScholarIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3F8),
    onPrimaryContainer = ScholarIndigo,
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = ElectricTealContainer,
    onSecondaryContainer = Color(0xFF043D38),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = WarmAmberContainer,
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
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudyPortalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
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
