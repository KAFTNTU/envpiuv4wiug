package com.robocar.app.scratch

// ═══════════════════════════════════════════════════════════════════════
// SAVE / LOAD PANEL — нижня панель збереження та завантаження
// ═══════════════════════════════════════════════════════════════════════

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*

// ─────────────────────────────────────────────────────────────
// ГОЛОВНА ПАНЕЛЬ
// ─────────────────────────────────────────────────────────────
@Composable
fun SaveLoadPanel(
    slots: List<SaveSlot>,
    onSave: (Int, String) -> Unit,
    onLoad: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf(SaveLoadMode.BROWSE) }   // BROWSE | SAVE_PICK | LOAD_PICK
    var savingSlot by remember { mutableStateOf(-1) }
    var saveName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(onClick = onDismiss),
    ) {
        // Bottom sheet surface
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {}
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {

                // ── Ручка ──────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp)).background(Color(0xFF334155)))
                }

                // ── Заголовок ──────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Програми", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Export
                        IconButton(onClick = onExport) {
                            Icon(Icons.Default.IosShare, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                        }
                        // Close
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // ── Вкладки ЗБЕРЕГТИ / ЗАВАНТАЖИТИ ─────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E293B))
                        .padding(4.dp),
                ) {
                    SaveLoadTab("Зберегти", Icons.Default.Save, mode == SaveLoadMode.SAVE_PICK) {
                        mode = if (mode == SaveLoadMode.SAVE_PICK) SaveLoadMode.BROWSE else SaveLoadMode.SAVE_PICK
                        savingSlot = -1
                    }
                    SaveLoadTab("Завантажити", Icons.Default.FolderOpen, mode == SaveLoadMode.LOAD_PICK) {
                        mode = if (mode == SaveLoadMode.LOAD_PICK) SaveLoadMode.BROWSE else SaveLoadMode.LOAD_PICK
                    }
                }

                // ── Сітка слотів ────────────────────────────────────
                AnimatedContent(targetState = mode, transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                }, label = "slotmode") { currentMode ->
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        // Hint
                        val hint = when (currentMode) {
                            SaveLoadMode.SAVE_PICK   -> "Оберіть слот для збереження"
                            SaveLoadMode.LOAD_PICK   -> "Оберіть програму для завантаження"
                            SaveLoadMode.BROWSE      -> "Натисніть слот щоб переглянути дії"
                        }
                        Text(hint, fontSize = 11.sp, color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))

                        Spacer(Modifier.height(6.dp))

                        // Сітка 2 × 3
                        val displaySlots = slots.take(WorkspaceSaveManager.NUM_SLOTS - 1) // без autosave
                        for (row in 0..1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                for (col in 0..2) {
                                    val idx = row * 3 + col
                                    val slot = displaySlots.getOrNull(idx)
                                    if (slot != null) {
                                        SaveSlotCard(
                                            slot       = slot,
                                            mode       = currentMode,
                                            isSaving   = savingSlot == slot.index,
                                            saveName   = saveName,
                                            onSaveNameChange = { saveName = it },
                                            onSaveConfirm = {
                                                onSave(slot.index, saveName)
                                                savingSlot = -1
                                                saveName   = ""
                                                mode       = SaveLoadMode.BROWSE
                                            },
                                            onClick = {
                                                when (currentMode) {
                                                    SaveLoadMode.SAVE_PICK -> {
                                                        savingSlot = slot.index
                                                        saveName   = if (slot.isEmpty) "" else slot.name
                                                    }
                                                    SaveLoadMode.LOAD_PICK -> {
                                                        if (!slot.isEmpty) {
                                                            onLoad(slot.index)
                                                            onDismiss()
                                                        }
                                                    }
                                                    SaveLoadMode.BROWSE -> {
                                                        if (!slot.isEmpty) {
                                                            savingSlot = slot.index
                                                            saveName   = slot.name
                                                            mode       = SaveLoadMode.SAVE_PICK
                                                        }
                                                    }
                                                }
                                            },
                                            onDelete = { onDelete(slot.index) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                            if (row < 1) Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                // ── Автозбереження статус ───────────────────────────
                val autoSlot = slots.getOrNull(WorkspaceSaveManager.NUM_SLOTS - 1)
                if (autoSlot != null && !autoSlot.isEmpty) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .clickable {
                                onLoad(autoSlot.index)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.History, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Column {
                                Text("Автозбереження", fontSize = 11.sp,
                                    color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                                Text("${autoSlot.blockCount} блоків · ${autoSlot.formattedDate}",
                                    fontSize = 10.sp, color = Color(0xFF475569))
                            }
                        }
                        Text("Відновити →", fontSize = 10.sp, color = Color(0xFF3B82F6))
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// СЛОТ КАРТКА
// ─────────────────────────────────────────────────────────────
@Composable
private fun SaveSlotCard(
    slot: SaveSlot,
    mode: SaveLoadMode,
    isSaving: Boolean,
    saveName: String,
    onSaveNameChange: (String) -> Unit,
    onSaveConfirm: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isSaving && mode == SaveLoadMode.SAVE_PICK -> Color(0xFF3B82F6)
        !slot.isEmpty && mode == SaveLoadMode.LOAD_PICK -> Color(0xFF22C55E)
        else -> Color(0xFF1E293B)
    }
    val bg = if (isSaving && mode == SaveLoadMode.SAVE_PICK) Color(0xFF172554)
             else if (slot.isEmpty) Color(0xFF0F1929) else Color(0xFF1E293B)

    val scale by animateFloatAsState(if (isSaving) 1.02f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "sc")

    Box(
        modifier = modifier
            .height(90.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        if (slot.isEmpty) {
            // ── Порожній слот ──────────────────────────────
            if (isSaving && mode == SaveLoadMode.SAVE_PICK) {
                // Поле вводу назви
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween) {
                    BasicTextField(
                        value = saveName,
                        onValueChange = onSaveNameChange,
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp,
                            fontFamily = FontFamily.Default),
                        cursorBrush = SolidColor(Color(0xFF3B82F6)),
                        decorationBox = { inner ->
                            if (saveName.isEmpty()) {
                                Text("Назва програми…", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = onSaveConfirm,
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White,
                            containerColor = Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text("Зберегти", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Звичайний порожній
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF334155), modifier = Modifier.size(22.dp))
                    Text("Порожньо", fontSize = 9.sp, color = Color(0xFF334155),
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            // ── Заповнений слот ─────────────────────────────
            if (isSaving && mode == SaveLoadMode.SAVE_PICK) {
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween) {
                    BasicTextField(
                        value = saveName,
                        onValueChange = onSaveNameChange,
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        cursorBrush = SolidColor(Color(0xFF3B82F6)),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = onSaveConfirm,
                            modifier = Modifier.weight(1f).height(26.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White,
                                containerColor = Color(0xFF3B82F6)),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(5.dp),
                        ) { Text("Перезаписати", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top) {
                        Text(slot.name, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier.size(18.dp).clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .clickable(onClick = onDelete),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFF475569),
                                modifier = Modifier.size(10.dp))
                        }
                    }
                    Column {
                        Text("${slot.blockCount} блоків", fontSize = 9.sp, color = Color(0xFF94A3B8))
                        Text(slot.formattedDate, fontSize = 9.sp, color = Color(0xFF475569))
                    }
                }
            }
        }

        // Номер слоту (кут)
        Text("${slot.index + 1}", fontSize = 8.sp, color = Color(0xFF1E293B),
            modifier = Modifier.align(Alignment.BottomEnd))
    }
}

// ─────────────────────────────────────────────────────────────
// TAB КНОПКА
// ─────────────────────────────────────────────────────────────
@Composable
private fun RowScope.SaveLoadTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f).height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF3B82F6) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) Color.White else Color(0xFF64748B),
                modifier = Modifier.size(14.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else Color(0xFF64748B))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// MODE ENUM
// ─────────────────────────────────────────────────────────────
enum class SaveLoadMode { BROWSE, SAVE_PICK, LOAD_PICK }

// ─────────────────────────────────────────────────────────────
// ПРИКЛАДИ ПАНЕЛЬ — вибір прикладів з превью
// ─────────────────────────────────────────────────────────────
@Composable
fun ExamplesPanel(
    onLoad: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0x88000000)).clickable(onClick = onDismiss),
    ) {
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clickable(enabled = false) {}.navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color(0xFF0F172A),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Ручка
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp)).background(Color(0xFF334155)))
                }
                Spacer(Modifier.height(12.dp))
                Text("Приклади програм", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = Color.White)
                Spacer(Modifier.height(12.dp))

                val examples = listOf(
                    ExampleDef("simple_drive",  "Рух вперед",          "🚗", Color(0xFF0062BA), "Їхати 1с → Стоп"),
                    ExampleDef("follow_line",   "Слідувати по лінії",   "〰", Color(0xFF5BA55B), "Цикл → Чекати сенсор → Поворот"),
                    ExampleDef("square",        "Їхати квадратом",      "⬜", Color(0xFF4C97FF), "4× (Рух + Поворот)"),
                    ExampleDef("autopilot",     "Автопілот",            "🤖", Color(0xFFE65100), "Уникати перешкод"),
                    ExampleDef("pid_line",      "PID лінія",            "📐", Color(0xFF9966FF), "PID регулятор лінії"),
                    ExampleDef("state_machine", "Стейт машина",         "🧠", Color(0xFF8E24AA), "SEARCH → ATTACK → EVADE"),
                )

                for (row in 0..1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0..2) {
                            val ex = examples.getOrNull(row * 3 + col)
                            if (ex != null) {
                                ExampleCard(
                                    example  = ex,
                                    onClick  = { onLoad(ex.key); onDismiss() },
                                    modifier = Modifier.weight(1f),
                                )
                            } else Spacer(Modifier.weight(1f))
                        }
                    }
                    if (row < 1) Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ExampleCard(example: ExampleDef, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "ep")

    Box(
        modifier = modifier
            .height(80.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(Color((example.color.value and 0x33FFFFFFu or 0xFF000000u).toLong()))
            .border(1.dp, Color(example.color.value), RoundedCornerShape(12.dp))
            .clickable {
                pressed = true
                onClick()
            }
            .padding(10.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(example.emoji, fontSize = 20.sp)
            Column {
                Text(example.name, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(example.desc, fontSize = 9.sp, color = Color(0xFF94A3B8),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private data class ExampleDef(
    val key: String, val name: String, val emoji: String,
    val color: Color, val desc: String,
)
