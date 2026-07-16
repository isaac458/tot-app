package com.empire.myapplication.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

// ===== Gemini Style Theme =====
private val TootDarkColorScheme = darkColorScheme(
    primary             = CosmicBlue,
    onPrimary           = GlassText,
    primaryContainer    = CosmicBlue.copy(alpha = 0.2f),
    onPrimaryContainer  = GlassText,
    secondary           = CosmicViolet,
    onSecondary         = GlassText,
    secondaryContainer  = CosmicViolet.copy(alpha = 0.2f),
    onSecondaryContainer = GlassText,
    tertiary            = SoftPink,
    onTertiary          = GlassText,
    background          = GeminiBg,
    onBackground        = GlassText,
    surface             = GeminiSurface,
    onSurface           = GlassText,
    surfaceVariant      = GeminiSurface.copy(alpha = 0.5f),
    onSurfaceVariant    = GlassTextSecondary,
    outline             = GlassBorder,
    outlineVariant      = GlassBorderSubtle,
    error               = GeminiError,
    onError             = GlassText,
    inverseSurface      = GlassText,
    inverseOnSurface    = GeminiBg,
    inversePrimary      = CosmicViolet,
    scrim               = GeminiBg.copy(alpha = 0.8f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force Gemini Dark Theme ALWAYS
    val colorScheme = TootDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GeminiBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // Dark bg, light text
        }
    }

    // التطبيق عربي بالكامل، لذا نفرض اتجاه RTL على كل الواجهة
    // بغض النظر عن لغة النظام، لضمان محاذاة صحيحة للرسائل والعناصر
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography, // Uses the typography defined in Typography.kt (should be clean/modern)
            content = content
        )
    }
}