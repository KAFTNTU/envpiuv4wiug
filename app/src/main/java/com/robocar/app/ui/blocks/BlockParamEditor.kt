package com.robocar.app.ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.robocar.app.model.*

@Composable
fun BlockParamEditor(
    block: ProgramBlock,
    onDismiss: () -> Unit,
    onUpdate: (Int, Any) -> Unit,
) {
    val blockColor = Color(block.type.color)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F1B2E))
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(blockColor.copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(block.type.emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        block.type.label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
            }

            // Параметри
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                block.params.forEachIndexed { index, param ->
                    when (param) {
                        is BlockParam.NumberInput -> NumberParamRow(
                            param = param,
                            blockColor = blockColor,
                            onChange = { onUpdate(index, it) }
                        )
                        is BlockParam.DropdownInput -> DropdownParamRow(
                            param = param,
                            blockColor = blockColor,
                            onChange = { onUpdate(index, it) }
                        )
                        is BlockParam.TextInput -> TextParamRow(
                            param = param,
                            blockColor = blockColor,
                            onChange = { onUpdate(index, it) }
                        )
                        else -> {}
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = blockColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Готово", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NumberParamRow(
    param: BlockParam.NumberInput,
    blockColor: Color,
    onChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2540))
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(param.label, color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (param.value == param.value.toLong().toFloat())
                    param.value.toLong().toString()
                else "%.2f".format(param.value),
                color = blockColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Slider(
            value = param.value,
            onValueChange = onChange,
            valueRange = param.min..param.max,
            colors = SliderDefaults.colors(
                thumbColor = blockColor,
                activeTrackColor = blockColor,
                inactiveTrackColor = Color(0xFF2D3A50)
            )
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("${param.min.toLong()}", color = Color(0xFF64748B), fontSize = 10.sp)
            Text("${param.max.toLong()}", color = Color(0xFF64748B), fontSize = 10.sp)
        }

        // Точне введення
        var textVal by remember(param.value) {
            mutableStateOf(
                if (param.value == param.value.toLong().toFloat())
                    param.value.toLong().toString()
                else "%.2f".format(param.value)
            )
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = textVal,
            onValueChange = { s ->
                textVal = s
                s.toFloatOrNull()?.let { v -> onChange(v.coerceIn(param.min, param.max)) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = blockColor,
                unfocusedBorderColor = Color(0xFF334155),
                cursorColor = blockColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
    }
}

@Composable
private fun DropdownParamRow(
    param: BlockParam.DropdownInput,
    blockColor: Color,
    onChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = param.options.find { it.second == param.selected }?.first ?: param.selected

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2540))
            .padding(12.dp)
    ) {
        Text(param.label, color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        // Options as chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            param.options.forEach { (label, value) ->
                val isSelected = value == param.selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onChange(value) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = blockColor,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF0D1525),
                        labelColor = Color(0xFF94A3B8)
                    )
                )
            }
        }
    }
}

@Composable
private fun TextParamRow(
    param: BlockParam.TextInput,
    blockColor: Color,
    onChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A2540))
            .padding(12.dp)
    ) {
        Text(param.label, color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = param.value,
            onValueChange = onChange,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = blockColor,
                unfocusedBorderColor = Color(0xFF334155),
                cursorColor = blockColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
    }
}
