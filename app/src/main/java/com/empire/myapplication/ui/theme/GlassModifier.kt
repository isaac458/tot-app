package com.empire.myapplication.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a Gemini-style Glassmorphism effect for cards and surfaces.
 * Uses completely rounded capsule shapes by default and very thin borders.
 */
fun Modifier.glassCard(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color(0x0DFFFFFF), // 5% white by default
    borderColor: Color = Color(0x1AFFFFFF), // 10% white by default
    borderWidth: Dp = 0.5.dp, // Extremely thin border
    blurRadius: Float = 0f // Kept for API compatibility
): Modifier = composed {
    this
        .background(backgroundColor, shape)
        .border(borderWidth, borderColor, shape)
        .clip(shape)
}

/**
 * Creates a soft glowing effect behind the element (The Sparkle Glow).
 */
fun Modifier.glowEffect(
    color: Color = CosmicBlue,
    radius: Dp = 30.dp,
    shape: Shape = CircleShape,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
): Modifier = composed {
    this.drawBehind {
        val shadowColor = color.copy(alpha = 0.3f) // Soft glow
        this.drawIntoCanvas {
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            frameworkPaint.color = Color.Transparent.value.toInt()
            frameworkPaint.setShadowLayer(
                radius.toPx(),
                offsetX.toPx(),
                offsetY.toPx(),
                shadowColor.value.toInt()
            )
            // Draw a rounded rect that acts as the glow source
            it.drawRoundRect(
                0f,
                0f,
                this.size.width,
                this.size.height,
                100.dp.toPx(), // Very rounded to simulate capsule
                100.dp.toPx(),
                paint
            )
        }
    }
}

/**
 * Gemini Cosmic background glow (subtle, not overwhelming)
 */
fun modernGradientBackground(): Brush {
    return Brush.radialGradient(
        colors = listOf(
            CosmicBlue.copy(alpha = 0.1f),
            GeminiBg
        ),
        radius = 1500f
    )
}

/**
 * The main "Sparkle" Button gradient
 */
fun buttonGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            CosmicBlue,
            CosmicViolet,
            SoftPink
        )
    )
}
