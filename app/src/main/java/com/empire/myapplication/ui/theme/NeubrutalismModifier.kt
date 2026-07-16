package com.empire.myapplication.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A modifier that applies the Neubrutalism style:
 * 1. A thick solid border.
 * 2. A hard offset drop shadow (no blur).
 */
fun Modifier.neubrutalism(
    borderWidth: Dp = 2.dp,
    borderColor: Color = NeoBlack,
    cornerRadius: Dp = 8.dp,
    shadowOffset: Dp = 4.dp,
    shadowColor: Color = NeoBlack
): Modifier = composed {
    this
        .drawBehind {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = shadowColor
                }
                // Draw hard shadow
                canvas.drawRoundRect(
                    left = shadowOffset.toPx(),
                    top = shadowOffset.toPx(),
                    right = size.width + shadowOffset.toPx(),
                    bottom = size.height + shadowOffset.toPx(),
                    radiusX = cornerRadius.toPx(),
                    radiusY = cornerRadius.toPx(),
                    paint = paint
                )
            }
        }
        .border(
            width = borderWidth,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
}
