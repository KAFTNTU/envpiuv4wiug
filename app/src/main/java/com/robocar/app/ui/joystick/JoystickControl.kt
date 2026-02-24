package com.robocar.app.ui.joystick

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun JoystickControl(
    modifier: Modifier = Modifier,
    onMove: (x: Int, y: Int) -> Unit,
    onRelease: () -> Unit,
) {
    var stickOffset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .size(280.dp)
            .pointerInput(Unit) {
                val maxDist = size.width * 0.32f  // ~90px як в оригіналі
                detectDragGestures(
                    onDragEnd = {
                        stickOffset = Offset.Zero
                        onRelease()
                    },
                    onDragCancel = {
                        stickOffset = Offset.Zero
                        onRelease()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = stickOffset + dragAmount
                        val dist = sqrt(newOffset.x.pow(2) + newOffset.y.pow(2))
                        val angle = atan2(newOffset.y, newOffset.x)
                        val clamped = min(dist, maxDist)
                        stickOffset = Offset(cos(angle) * clamped, sin(angle) * clamped)
                        val vx = ((stickOffset.x / maxDist) * 100).toInt().coerceIn(-100, 100)
                        val vy = ((-stickOffset.y / maxDist) * 100).toInt().coerceIn(-100, 100)
                        onMove(vx, vy)
                    }
                )
            }
    ) {
        val w = size.width
        val center = Offset(w / 2f, w / 2f)
        val baseRadius = w / 2f - 2.dp.toPx()

        // База: radial-gradient(circle at center, #1e293b 0%, #0f172a 70%)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFF1e293b),
                    0.7f to Color(0xFF0f172a),
                    1.0f to Color(0xFF0f172a),
                ),
                center = center,
                radius = baseRadius
            ),
            radius = baseRadius,
            center = center
        )

        // border: 2px solid rgba(59,130,246,0.3)
        drawCircle(
            color = Color(0x4C3B82F6),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Стік: 90x90px, radial-gradient(circle at 30% 30%, #3b82f6, #1d4ed8)
        val stickR = 45.dp.toPx()
        val stickCenter = center + stickOffset

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF60a5fa), Color(0xFF3b82f6), Color(0xFF1d4ed8)),
                center = stickCenter - Offset(stickR * 0.4f, stickR * 0.4f),
                radius = stickR
            ),
            radius = stickR,
            center = stickCenter
        )

        // Бліка: rgba(255,255,255,0.2) — ::after
        drawCircle(
            color = Color(0x33FFFFFF),
            radius = 12.5.dp.toPx(),
            center = stickCenter - Offset(stickR * 0.3f, stickR * 0.3f)
        )
    }
}
