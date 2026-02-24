package com.robocar.app.ui.joystick

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robocar.app.MainViewModel
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────
// КОЛЬОРИ (точно як в оригіналі)
// ─────────────────────────────────────────────────────────────
private val BgPage  = Color(0xFF0A0E1A)
private val CardBg  = Color(0xFF111827)
private val Purple  = Color(0xFF8B5CF6)
private val PurpleD = Color(0xFF5B21B6)
private val PurpleH = Color(0xFFC4B5FD)   // підсвічення (blick)

// ─────────────────────────────────────────────────────────────
// РОЗМІРИ
// HEIGHT_FRACTION = 0.70f → зменшено на 30% (було 1.0f)
// ─────────────────────────────────────────────────────────────
private const val HEIGHT_FRACTION = 0.70f

@Composable
fun FourMotorScreen(viewModel: MainViewModel) {
    var m1 by remember { mutableStateOf(0) }
    var m2 by remember { mutableStateOf(0) }
    var m3 by remember { mutableStateOf(0) }
    var m4 by remember { mutableStateOf(0) }

    LaunchedEffect(m1, m2, m3, m4) {
        if (viewModel.bleManager.isConnected) {
            viewModel.bleManager.sendDrivePacket(m1, m2, m3, m4)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
            .padding(top = 8.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        // ── Рядок 1: M1 | M2 ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(HEIGHT_FRACTION),   // 0.70 замість 1.0 → -30%
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VerticalMotorSlider(label = "M1", value = m1, onValue = { m1 = it }, modifier = Modifier.weight(1f).fillMaxHeight())
            VerticalMotorSlider(label = "M2", value = m2, onValue = { m2 = it }, modifier = Modifier.weight(1f).fillMaxHeight())
        }

        Spacer(Modifier.height(10.dp))

        // ── Рядок 2: M3 | M4 ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(HEIGHT_FRACTION),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VerticalMotorSlider(label = "M3", value = m3, onValue = { m3 = it }, modifier = Modifier.weight(1f).fillMaxHeight())
            VerticalMotorSlider(label = "M4", value = m4, onValue = { m4 = it }, modifier = Modifier.weight(1f).fillMaxHeight())
        }

        Spacer(Modifier.height(10.dp))

        // ── HEX рядок ────────────────────────────────────────
        val toHex: (Int) -> String = { v ->
            (v.toByte().toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF0D1117))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = "HEX:  ${toHex(m1)}  ${toHex(m2)}  ${toHex(m3)}  ${toHex(m4)}",
                fontSize   = 13.sp,
                fontFamily = FontFamily.Monospace,
                color      = Color(0xFF8B5CF6),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// VERTICAL MOTOR SLIDER
// Без підписів ПЕРЕД/ЗАД — тільки M1..M4
// Висота зменшена через HEIGHT_FRACTION у батьківському Row
// ─────────────────────────────────────────────────────────────
@Composable
private fun VerticalMotorSlider(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var stickY by remember { mutableStateOf(0f) }

    Column(
        modifier              = modifier,
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Top,
    ) {
        // Мітка: тільки "M1", "M2", "M3", "M4" — без ПЕРЕД/ЗАД
        Text(
            text          = label,
            fontSize      = 11.sp,
            color         = Color(0xFF94A3B8),
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier      = Modifier.padding(bottom = 4.dp),
        )

        // Картка
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardBg)
                .pointerInput(Unit) {
                    val maxDist = size.height * 0.34f
                    detectDragGestures(
                        onDragEnd = {
                            stickY = 0f
                            onValue(0)
                        },
                        onDragCancel = {
                            stickY = 0f
                            onValue(0)
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            stickY = (stickY + drag.y).coerceIn(-maxDist, maxDist)
                            val speed = ((-stickY / maxDist) * 100).roundToInt().coerceIn(-100, 100)
                            onValue(speed)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            // Вертикальна лінія
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight(0.75f)
                    .background(Color(0xFF1E293B))
                    .align(Alignment.Center),
            )

            // Значення швидкості (текст)
            SpeedIndicator(value = value)

            // Куля джойстика
            Canvas(
                modifier = Modifier
                    .size(62.dp)
                    .offset(y = (stickY / 2.8f).dp),
            ) {
                val r = size.minDimension / 2f
                // Тінь
                drawCircle(Color(0x55000000), radius = r + 3f, center = center + Offset(2f, 4f))
                // Основне тіло (радіальний градієнт)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PurpleH, Purple, PurpleD),
                        center = center - Offset(r * 0.3f, r * 0.3f),
                        radius = r,
                    ),
                    radius = r,
                    center = center,
                )
                // Блік (highlight)
                drawCircle(
                    color  = Color(0x44FFFFFF),
                    radius = r * 0.28f,
                    center = center - Offset(r * 0.32f, r * 0.32f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SPEED INDICATOR — число зверху/знизу залежно від значення
// ─────────────────────────────────────────────────────────────
@Composable
private fun BoxScope.SpeedIndicator(value: Int) {
    if (value == 0) return
    val isPositive = value > 0
    Text(
        text     = if (isPositive) "+$value" else "$value",
        fontSize = 9.sp,
        color    = when {
            value > 0  -> Color(0xFF34D399)
            value < 0  -> Color(0xFFF87171)
            else       -> Color(0xFF64748B)
        },
        fontFamily    = FontFamily.Monospace,
        fontWeight    = FontWeight.Bold,
        modifier      = Modifier
            .align(if (isPositive) Alignment.TopCenter else Alignment.BottomCenter)
            .padding(vertical = 8.dp),
    )
}
