package com.robocar.app.ui.joystick

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robocar.app.MainViewModel
import com.robocar.app.ui.settings.TuningDialog

private val BgPage      = Color(0xFF0F172A)
private val BgCard      = Color(0xFF0F172A)
private val BorderColor = Color(0xFF334155)
private val TextMuted   = Color(0xFF94A3B8)
private val TextGreen   = Color(0xFF22C55E)
private val TextBlue    = Color(0xFF3B82F6)
private val TextYellow  = Color(0xFFEAB308)
private val SliderTrack = Color(0xFF334155)

@Composable
fun JoystickScreen(viewModel: MainViewModel) {
    val motorL      by viewModel.motorL.collectAsState()
    val motorR      by viewModel.motorR.collectAsState()
    val gyroEnabled by viewModel.gyroEnabled.collectAsState()
    var speedPercent by remember { mutableStateOf(100f) }
    var showTuning  by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Джойстик
        Box(
            modifier = Modifier.padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            JoystickControl(
                onMove    = { vx, vy -> if (!gyroEnabled) viewModel.updateJoystick(vx, vy) },
                onRelease = { if (!gyroEnabled) viewModel.resetJoystick() }
            )
        }

        // Нижня панель
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x221E293B))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // === L | Gyro | R — рядок ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // L — займає весь простір зліва
                MotorDisplay(
                    label    = "L",
                    value    = motorL,
                    modifier = Modifier.weight(1f)
                )

                // Gyro кнопка — фіксований розмір по центру
                val gyroColor by animateColorAsState(
                    targetValue = if (gyroEnabled) Color(0xFF3B82F6) else Color(0xFF334155),
                    animationSpec = tween(300),
                    label = "gyro"
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(gyroColor)
                        .clickable { viewModel.toggleGyro() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Smartphone,
                        contentDescription = "Gyro",
                        tint = if (gyroEnabled) Color.White else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // R — займає весь простір справа
                MotorDisplay(
                    label    = "R",
                    value    = motorR,
                    modifier = Modifier.weight(1f)
                )
            }

            // Підпис під рядком — підказка про гіроскоп
            if (gyroEnabled) {
                Text(
                    "🔵 Гіроскоп активний — нахиляйте телефон",
                    fontSize  = 9.sp,
                    color     = TextBlue,
                    fontWeight = FontWeight.Bold,
                    modifier  = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Розділювач
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x0DFFFFFF))
            )

            // Слайдер потужності
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Потужність",
                        fontSize = 10.sp, color = TextMuted,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                    )
                    Text(
                        "${speedPercent.toInt()}%",
                        fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Slider(
                    value          = speedPercent,
                    onValueChange  = { speedPercent = it; viewModel.setSpeed(it.toInt()) },
                    valueRange     = 10f..100f,
                    steps          = 8,
                    colors         = SliderDefaults.colors(
                        thumbColor         = TextBlue,
                        activeTrackColor   = TextBlue,
                        inactiveTrackColor = SliderTrack
                    )
                )
            }

            // Кнопка налаштування
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF334155))
                    .clickable { showTuning = true }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tune, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Налаштування моторів", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
            }

            // HEX
            val lHex = (motorL.toByte().toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
            val rHex = (motorR.toByte().toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row {
                    Text("HEX: ", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B), letterSpacing = 2.sp)
                    Text("$lHex $rHex", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextYellow, letterSpacing = 2.sp)
                }
            }
        }
    }

    if (showTuning) {
        TuningDialog(viewModel = viewModel, onDismiss = { showTuning = false })
    }
}

// L / R дисплей — розтягується на всю доступну ширину через Modifier.weight(1f)
@Composable
private fun MotorDisplay(label: String, value: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            color      = TextGreen,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text       = value.toString(),
            fontSize   = 20.sp,
            color      = TextBlue,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
