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
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val BgPage  = Color(0xFF0A0E1A)
private val CardBg  = Color(0xFF111827)
private val Purple  = Color(0xFF8B5CF6)
private val PurpleD = Color(0xFF5B21B6)

@Composable
fun FourMotorScreen(viewModel: MainViewModel) {
    // Стани 4 моторів -100..100
    var m1 by remember { mutableStateOf(0) }
    var m2 by remember { mutableStateOf(0) }
    var m3 by remember { mutableStateOf(0) }
    var m4 by remember { mutableStateOf(0) }

    // Відправляємо пакет коли змінюється будь-який мотор
    LaunchedEffect(m1, m2, m3, m4) {
        if (viewModel.bleManager.isConnected) {
            viewModel.bleManager.sendDrivePacket(m1, m2, m3, m4)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
            .padding(top = 8.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Рядок 1: M1 (ПЕРЕД) | M2 (ПЕРЕД)
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VerticalMotorJoystick(
                label = "M1 (ПЕРЕД)",
                value = m1,
                onValue = { m1 = it },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            VerticalMotorJoystick(
                label = "M2 (ПЕРЕД)",
                value = m2,
                onValue = { m2 = it },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        Spacer(Modifier.height(10.dp))

        // Рядок 2: M3 (ЗАД) | M4 (ЗАД)
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VerticalMotorJoystick(
                label = "M3 (ЗАД)",
                value = m3,
                onValue = { m3 = it },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            VerticalMotorJoystick(
                label = "M4 (ЗАД)",
                value = m4,
                onValue = { m4 = it },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        Spacer(Modifier.height(10.dp))

        // HEX рядок внизу — точно як в оригіналі
        val toHex: (Int) -> String = { v ->
            (v.toByte().toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF0D1117))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "HEX:  ${toHex(m1)}  ${toHex(m2)}  ${toHex(m3)}  ${toHex(m4)}",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF8B5CF6),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

// Вертикальний джойстик — тільки Y вісь (як в оригіналі)
@Composable
private fun VerticalMotorJoystick(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var stickY by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Підпис
        Text(
            label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Карточка з джойстиком
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .pointerInput(Unit) {
                    val maxDist = size.height * 0.32f
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
                            // Вгору = позитивна швидкість
                            val speed = ((-stickY / maxDist) * 100).roundToInt().coerceIn(-100, 100)
                            onValue(speed)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Вертикальна лінія (напрямок)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight(0.7f)
                    .background(Color(0xFF1E293B))
                    .align(Alignment.Center)
            )

            // Куля джойстика (фіолетова як в Image 3)
            Canvas(
                modifier = Modifier
                    .size(70.dp)
                    .offset(y = stickY.dp / 2.5f)
            ) {
                val r = size.minDimension / 2f
                // Тінь
                drawCircle(Color(0x55000000), radius = r + 4f, center = center + Offset(2f, 4f))
                // Основне
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFA78BFA), Purple, PurpleD),
                        center = center - Offset(r * 0.3f, r * 0.3f),
                        radius = r
                    ),
                    radius = r,
                    center = center
                )
                // Блік
                drawCircle(
                    color  = Color(0x44FFFFFF),
                    radius = r * 0.3f,
                    center = center - Offset(r * 0.3f, r * 0.3f)
                )
            }
        }
    }
}
