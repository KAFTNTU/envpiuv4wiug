package com.robocar.app.scratch

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.robocar.app.model.BlockParam

// ─────────────────────────────────────────────────────────────
// BLOCK PARAM EDITOR — діалог редагування параметрів блоку
// Відкривається по тапу на виділеному блоку
// ─────────────────────────────────────────────────────────────
@Composable
fun BlockParamEditorDialog(
    block: WsBlock,
    onDismiss: () -> Unit,
    onUpdateParam: (Int, Any) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
) {
    val cat    = block.type.category
    val color  = categoryColor(cat)
    val colorD = categoryColorDark(cat)

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // Scrim — тапнути щоб закрити
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000))
                    .clickable(onClick = onDismiss)
            )

            // Сам діалог
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color(0xFF0F172A),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 20.dp)
                ) {
                    // Хендл
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .align(Alignment.CenterHorizontally)
                            .size(40.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF334155))
                    )

                    Spacer(Modifier.height(12.dp))

                    // Заголовок блоку
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Кольорова мітка блоку
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text       = block.type.label,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White,
                            )
                        }

                        // Кнопка закрити
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close, null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Підзаголовок категорії
                    Text(
                        text     = cat.label,
                        fontSize = 11.sp,
                        color    = color.copy(0.8f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 40.dp, top = 2.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Параметри блоку ─────────────────────────────
                    val editableParams = block.params
                        .mapIndexed { idx, p -> idx to p }
                        .filter { (_, p) -> p !is BlockParam.SubProgram }

                    if (editableParams.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Цей блок не має параметрів",
                                fontSize = 13.sp,
                                color    = Color(0xFF64748B),
                            )
                        }
                    } else {
                        editableParams.forEach { (idx, param) ->
                            ParamRow(
                                param    = param,
                                color    = color,
                                onUpdate = { value -> onUpdateParam(idx, value) },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Кнопки дій ─────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Дублювати
                        OutlinedButton(
                            onClick = { onDuplicate(); onDismiss() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF3B82F6)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(0.5f)),
                            shape  = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Копія", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Видалити
                        Button(
                            onClick = { onDelete(); onDismiss() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape  = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Видалити", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PARAM ROW — один рядок редагування параметру
// ─────────────────────────────────────────────────────────────
@Composable
private fun ParamRow(
    param: BlockParam,
    color: Color,
    onUpdate: (Any) -> Unit,
) {
    when (param) {
        is BlockParam.NumberInput -> NumberParamRow(param, color, onUpdate)
        is BlockParam.DropdownInput -> DropdownParamRow(param, color, onUpdate)
        is BlockParam.TextInput -> TextParamRow(param, color, onUpdate)
        else -> {}
    }
}

// ── Числовий параметр зі слайдером + полем вводу ────────────
@Composable
private fun NumberParamRow(
    param: BlockParam.NumberInput,
    color: Color,
    onUpdate: (Any) -> Unit,
) {
    var sliderVal by remember(param.value) { mutableStateOf(param.value) }
    var textVal   by remember(param.value) {
        mutableStateOf(
            if (param.value == param.value.toLong().toFloat())
                param.value.toInt().toString()
            else param.value.toString()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Назва + поточне значення
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(param.label, fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)

            // Поле вводу числа
            OutlinedTextField(
                value         = textVal,
                onValueChange = { raw ->
                    textVal = raw
                    raw.toFloatOrNull()?.let { f ->
                        sliderVal = f.coerceIn(param.min, param.max)
                        onUpdate(sliderVal)
                    }
                },
                modifier       = Modifier.width(80.dp).height(46.dp),
                singleLine     = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle      = LocalTextStyle.current.copy(
                    color      = Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = color,
                    unfocusedBorderColor = Color(0xFF334155),
                    cursorColor          = color,
                    focusedContainerColor   = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                ),
                shape = RoundedCornerShape(8.dp),
            )
        }

        // Слайдер
        Slider(
            value          = sliderVal,
            onValueChange  = { v ->
                sliderVal = v
                textVal   = if (v == v.toLong().toFloat()) v.toInt().toString() else "%.1f".format(v)
                onUpdate(v)
            },
            valueRange     = param.min..param.max,
            colors         = SliderDefaults.colors(
                thumbColor         = color,
                activeTrackColor   = color,
                inactiveTrackColor = Color(0xFF334155),
            ),
        )

        // Мін/Макс підписи
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(param.min.toInt().toString(), fontSize = 9.sp, color = Color(0xFF64748B))
            Text(param.max.toInt().toString(), fontSize = 9.sp, color = Color(0xFF64748B))
        }
    }
}

// ── Dropdown параметр ────────────────────────────────────────
@Composable
private fun DropdownParamRow(
    param: BlockParam.DropdownInput,
    color: Color,
    onUpdate: (Any) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = param.options.find { it.second == param.selected }?.first ?: param.selected

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(param.label, fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, color.copy(0.4f), RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(selected, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Icon(
                    Icons.Default.KeyboardArrowDown, null,
                    tint     = color,
                    modifier = Modifier.size(18.dp),
                )
            }

            DropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
                modifier         = Modifier.background(Color(0xFF1E293B)),
            ) {
                param.options.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                color      = if (value == param.selected) color else Color.White,
                                fontWeight = if (value == param.selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize   = 13.sp,
                            )
                        },
                        onClick = {
                            onUpdate(value)
                            expanded = false
                        },
                        leadingIcon = if (value == param.selected) ({
                            Icon(Icons.Default.Check, null, tint = color, modifier = Modifier.size(14.dp))
                        }) else null,
                    )
                }
            }
        }
    }
}

// ── Текстовий параметр ───────────────────────────────────────
@Composable
private fun TextParamRow(
    param: BlockParam.TextInput,
    color: Color,
    onUpdate: (Any) -> Unit,
) {
    var text by remember(param.value) { mutableStateOf(param.value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(param.label, fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value         = text,
            onValueChange = { raw ->
                text = raw
                onUpdate(raw)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle  = LocalTextStyle.current.copy(
                color      = Color.White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            placeholder = {
                Text(param.label, color = Color(0xFF475569), fontSize = 13.sp)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = color,
                unfocusedBorderColor    = Color(0xFF334155),
                cursorColor             = color,
                focusedContainerColor   = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
            ),
            shape = RoundedCornerShape(8.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// CONTEXT MENU — по довгому тиску на блок
// ─────────────────────────────────────────────────────────────
@Composable
fun BlockContextMenu(
    block: WsBlock,
    offsetX: Float,
    offsetY: Float,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = categoryColor(block.type.category)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .offset(x = offsetX.coerceIn(0f, 200f).dp, y = offsetY.coerceIn(0f, 400f).dp)
                .width(180.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // Заголовок блоку
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape).background(color)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        block.type.label,
                        fontSize   = 12.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Divider(color = Color(0xFF334155))

                // Пункт: Редагувати
                ContextMenuItem(
                    icon  = Icons.Default.Edit,
                    label = "Редагувати",
                    color = Color(0xFF60A5FA),
                    onClick = { onEdit(); onDismiss() }
                )

                // Пункт: Дублювати
                ContextMenuItem(
                    icon  = Icons.Default.ContentCopy,
                    label = "Дублювати",
                    color = Color(0xFF34D399),
                    onClick = { onDuplicate(); onDismiss() }
                )

                Divider(color = Color(0xFF334155))

                // Пункт: Видалити
                ContextMenuItem(
                    icon  = Icons.Default.Delete,
                    label = "Видалити",
                    color = Color(0xFFF87171),
                    onClick = { onDelete(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = Color.White)
    }
}
