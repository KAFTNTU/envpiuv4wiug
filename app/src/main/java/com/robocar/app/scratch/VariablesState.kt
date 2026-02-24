package com.robocar.app.scratch

// ═══════════════════════════════════════════════════════════════════════
// VARIABLES STATE — динамічні змінні для виконавця
// Зберігає іменовані змінні (Float) під час виконання програми.
// Доступний для WsExecutor, скидається при кожному запуску.
// ═══════════════════════════════════════════════════════════════════════

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VariablesState {

    // Поточні значення змінних
    private val _vars = mutableMapOf<String, Float>()

    // Snapshot для UI (ViewModel може підписатись)
    private val _snapshot = MutableStateFlow<Map<String, Float>>(emptyMap())
    val snapshot: StateFlow<Map<String, Float>> = _snapshot.asStateFlow()

    // ─── Встановити значення ──────────────────────────────
    fun set(name: String, value: Float) {
        _vars[name.trim()] = value
        publishSnapshot()
    }

    // ─── Отримати значення (0f якщо немає) ───────────────
    fun get(name: String): Float = _vars[name.trim()] ?: 0f

    // ─── Змінити на delta ─────────────────────────────────
    fun change(name: String, delta: Float) {
        val key = name.trim()
        _vars[key] = (_vars[key] ?: 0f) + delta
        publishSnapshot()
    }

    // ─── Очистити всі змінні (при старті програми) ───────
    fun clear() {
        _vars.clear()
        publishSnapshot()
    }

    // ─── Перевірити наявність ─────────────────────────────
    fun has(name: String): Boolean = _vars.containsKey(name.trim())

    // ─── Список всіх змінних ─────────────────────────────
    fun all(): Map<String, Float> = _vars.toMap()

    // ─── Відформатоване значення для логу ─────────────────
    fun formatted(name: String): String {
        val v = get(name)
        return if (v == kotlin.math.floor(v).toFloat() && kotlin.math.abs(v) < 1_000_000)
            v.toInt().toString()
        else "%.3f".format(v)
    }

    private fun publishSnapshot() {
        _snapshot.value = _vars.toMap()
    }
}

// ─────────────────────────────────────────────────────────────
// VARIABLES PANEL — UI для відображення змінних під час виконання
// ─────────────────────────────────────────────────────────────

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*

@Composable
fun VariablesPanel(
    vars: Map<String, Float>,
    modifier: Modifier = Modifier,
) {
    if (vars.isEmpty()) return

    AnimatedVisibility(
        visible  = vars.isNotEmpty(),
        enter    = fadeIn() + expandVertically(),
        exit     = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xE50F172A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.DataObject, null, tint = Color(0xFFFF8C1A),
                    modifier = Modifier.size(12.dp))
                Text("ЗМІННІ", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF8C1A), letterSpacing = 1.sp)
            }
            for ((name, value) in vars) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, fontSize = 10.sp, color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace)
                    val formatted = if (value == kotlin.math.floor(value).toFloat()
                        && kotlin.math.abs(value) < 1e6) value.toInt().toString()
                    else "%.3f".format(value)
                    Text(formatted, fontSize = 10.sp, color = Color(0xFFFF8C1A),
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// VARIABLES WATCH PANEL — більш детальна панель для Debug режиму
// ─────────────────────────────────────────────────────────────
@Composable
fun VariablesWatchPanel(
    vars: Map<String, Float>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0A0E1A),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("ЗМІННІ", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF8C1A), letterSpacing = 2.sp)
                Text("${vars.size} шт.", fontSize = 9.sp, color = Color(0xFF475569))
            }

            if (vars.isEmpty()) {
                Text("Немає активних змінних", fontSize = 11.sp, color = Color(0xFF334155),
                    modifier = Modifier.padding(vertical = 8.dp))
            } else {
                for ((name, value) in vars.entries.sortedBy { it.key }) {
                    VarWatchRow(name = name, value = value)
                }
            }
        }
    }
}

@Composable
private fun VarWatchRow(name: String, value: Float) {
    val barWidth by animateFloatAsState(
        (value.coerceIn(0f, 100f) / 100f), tween(300), label = "vbar")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 11.sp, color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace)
            val formatted = if (value == kotlin.math.floor(value).toFloat()
                && kotlin.math.abs(value) < 1e6) value.toInt().toString()
            else "%.3f".format(value)
            Text(formatted, fontSize = 11.sp, color = Color(0xFFFF8C1A),
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        // Progress bar для значень 0..100
        if (value in 0f..100f) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp)
                .clip(RoundedCornerShape(1.dp)).background(Color(0xFF1E293B))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(barWidth)
                    .clip(RoundedCornerShape(1.dp)).background(Color(0xFFFF8C1A)))
            }
        }
    }
}
