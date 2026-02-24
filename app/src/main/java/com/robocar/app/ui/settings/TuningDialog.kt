package com.robocar.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.robocar.app.MainViewModel
import com.robocar.app.model.TuningSettings

// Кольори з оригінального веб-дизайну (Image 2)
private val Blue   = Color(0xFF3B82F6)
private val Muted  = Color(0xFF64748B)
private val Card   = Color(0xFF1E293B)
private val Bg     = Color(0xFF0F172A)

@Composable
fun TuningDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val tuning     by viewModel.tuning.collectAsState()
    var invertL    by remember { mutableStateOf(tuning.invertL) }
    var invertR    by remember { mutableStateOf(tuning.invertR) }
    var trim       by remember { mutableStateOf(tuning.trim.toFloat()) }
    var turnSens   by remember { mutableStateOf(tuning.turnSens.toFloat()) }
    var use4Motors by remember { mutableStateOf(tuning.use4Motors) }
    var useSlip    by remember { mutableStateOf(tuning.useSlip) }

    fun save() {
        viewModel.updateTuning(
            TuningSettings(invertL, invertR, trim.toInt(), turnSens.toInt(), use4Motors, useSlip)
        )
        onDismiss()
    }

    Dialog(
        onDismissRequest = ::save,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .clickable(onClick = ::save)
            )

            // Sheet знизу — стиль як в Image 2 (без скла, прямий фон)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Bg)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Хендл
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(40.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF334155))
                )

                Spacer(Modifier.height(16.dp))

                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⚙️ Налаштування шасі",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Card)
                            .clickable(onClick = ::save),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Muted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // === SLIP Протокол ===
                ToggleRow(
                    label    = "🛡 SLIP Протокол",
                    checked  = useSlip,
                    onChange = { useSlip = it }
                )

                Spacer(Modifier.height(10.dp))

                // === 4WD ===
                ToggleRow(
                    label    = "🚗 Режим 4 моторів (4WD)",
                    checked  = use4Motors,
                    onChange = { use4Motors = it }
                )

                Spacer(Modifier.height(20.dp))

                // === ІНВЕРСІЯ МОТОРІВ ===
                Text(
                    "ІНВЕРСІЯ МОТОРІВ",
                    fontSize = 11.sp,
                    color = Muted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // L / R кнопки — однакова ширина на весь рядок (як в Image 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InvertBtn(
                        label     = "L (Лівий)",
                        checked   = invertL,
                        onChange  = { invertL = it },
                        modifier  = Modifier.weight(1f)
                    )
                    InvertBtn(
                        label     = "R (Правий)",
                        checked   = invertR,
                        onChange  = { invertR = it },
                        modifier  = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // === ВІДХИЛЕННЯ БАЛАНС ===
                SettingSlider(
                    title       = "ВІДХИЛЕННЯ (БАЛАНС)",
                    leftLabel   = "Ліво",
                    rightLabel  = "Право",
                    value       = trim,
                    range       = -50f..50f,
                    display     = trim.toInt().toString(),
                    onValueChange = { trim = it }
                )

                Spacer(Modifier.height(12.dp))

                // === ЧУТЛИВІСТЬ ПОВОРОТУ ===
                SettingSlider(
                    title       = "ЧУТЛИВІСТЬ ПОВОРОТУ",
                    leftLabel   = "Плавний",
                    rightLabel  = "Різкий",
                    value       = turnSens,
                    range       = 10f..100f,
                    display     = "${turnSens.toInt()}%",
                    onValueChange = { turnSens = it }
                )

                Spacer(Modifier.height(24.dp))

                // Кнопка Зберегти
                Button(
                    onClick = ::save,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("Зберегти та закрити", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// Ряд з перемикачем (SLIP, 4WD)
@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2744))
            .border(1.dp, Color(0xFF2D3F6B), RoundedCornerShape(12.dp))
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFADD8E6), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = Color(0xFF3B82F6),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}

// Кнопка інверсії L / R — займає половину рядка
@Composable
private fun InvertBtn(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) Color(0xFF1E3A5F) else Color(0xFF1E293B))
            .border(
                1.5.dp,
                if (checked) Color(0xFF3B82F6) else Color(0xFF334155),
                RoundedCornerShape(12.dp)
            )
            .clickable { onChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (checked) Color(0xFF60A5FA) else Muted,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SettingSlider(
    title: String,
    leftLabel: String,
    rightLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(leftLabel, fontSize = 10.sp, color = Muted)
            Text(title, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold)
            Text(rightLabel, fontSize = 10.sp, color = Muted)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor        = Color(0xFF3B82F6),
                activeTrackColor  = Color(0xFF3B82F6),
                inactiveTrackColor= Color(0xFF334155)
            )
        )
        Text(
            display,
            fontSize = 13.sp,
            color = Color(0xFF3B82F6),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
