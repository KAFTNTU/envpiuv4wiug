package com.robocar.app.ui.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robocar.app.model.BlockParam
import com.robocar.app.model.ProgramBlock

private const val NOTCH_X  = 28f
private const val NOTCH_W  = 24f
private const val NOTCH_H  = 9f
private const val CORNER_R = 5f

fun buildBlockPath(
    left: Float, top: Float, w: Float, h: Float,
    hasPrev: Boolean, hasNext: Boolean, isHat: Boolean
): Path = Path().apply {
    val r = CORNER_R
    val nx = left + NOTCH_X
    val nw = NOTCH_W
    val nh = NOTCH_H

    // Верхній лівий кут
    moveTo(left + r, top)

    if (!isHat && hasPrev) {
        // Виріз зверху
        lineTo(nx, top)
        lineTo(nx + 4f, top + nh * 0.5f)
        lineTo(nx + nw - 4f, top + nh * 0.5f)
        lineTo(nx + nw, top)
    }

    // Верхній правий
    lineTo(left + w - r, top)
    quadraticBezierTo(left + w, top, left + w, top + r)

    // Правий бік вниз
    lineTo(left + w, top + h - r)
    quadraticBezierTo(left + w, top + h, left + w - r, top + h)

    // Нижній — виступ якщо hasNext
    if (hasNext) {
        lineTo(nx + nw, top + h)
        lineTo(nx + nw - 4f, top + h + nh * 0.6f)
        lineTo(nx + 4f, top + h + nh * 0.6f)
        lineTo(nx, top + h)
    }

    // Нижній лівий
    lineTo(left + r, top + h)
    quadraticBezierTo(left, top + h, left, top + h - r)

    // Ліва сторона вгору
    if (isHat) {
        lineTo(left, top + 14f)
        quadraticBezierTo(left, top, left + r, top)
    } else {
        lineTo(left, top + r)
        quadraticBezierTo(left, top, left + r, top)
    }
    close()
}

@Composable
fun PuzzleBlock(
    block: ProgramBlock,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color    = Color(block.type.color.toInt())
    val darkMult = 0.65f
    val colorDark = Color(
        (color.red   * darkMult).coerceIn(0f, 1f),
        (color.green * darkMult).coerceIn(0f, 1f),
        (color.blue  * darkMult).coerceIn(0f, 1f),
        1f
    )

    val isHat   = !block.type.hasPrev
    val hasNext = block.type.hasNext
    val blockH  = 46f
    val tabH    = if (hasNext) NOTCH_H * 0.6f else 0f
    val hatExtra = if (isHat) 12f else 0f
    val totalDp = ((blockH + tabH + hatExtra) / 2.5f).coerceAtLeast(20f)

    val labelText = block.type.label
    val paramText = block.params
        .filterNot { it is BlockParam.SubProgram }
        .take(2)
        .joinToString("  ") { p ->
            when (p) {
                is BlockParam.NumberInput   -> "${p.value.toInt()}"
                is BlockParam.DropdownInput -> p.options.find { it.second == p.selected }?.first?.take(6) ?: ""
                is BlockParam.TextInput     -> p.value.take(8)
                else -> ""
            }
        }.trim()

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(totalDp.dp)
            .clickable(onClick = onClick)
    ) {
        val w    = size.width
        val topY = hatExtra

        val path = buildBlockPath(0f, topY, w, blockH, block.type.hasPrev, hasNext, isHat)

        // Тінь (зміщена вниз)
        val shadowPath = buildBlockPath(2f, topY + 3f, w, blockH, block.type.hasPrev, hasNext, isHat)
        drawPath(shadowPath, color = Color(0x33000000))

        // Основне тіло
        drawPath(path, color = color)

        // Ліва темна смужка
        val stripePath = buildBlockPath(0f, topY, 7f, blockH, block.type.hasPrev, hasNext, isHat)
        drawPath(stripePath, color = colorDark)

        // Обведення
        drawPath(
            path,
            color = if (isActive) Color.White.copy(0.8f) else colorDark.copy(0.6f),
            style = Stroke(width = if (isActive) 2.5f else 1f)
        )

        // Текст назви
        val labelStyle = TextStyle(
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        val labelMeasured = textMeasurer.measure(labelText, labelStyle)
        drawText(
            textLayoutResult = labelMeasured,
            topLeft = Offset(12f, topY + (blockH - labelMeasured.size.height) / 2f)
        )

        // Параметри (маленький текст справа)
        if (paramText.isNotBlank()) {
            val paramStyle = TextStyle(
                color = Color.White.copy(0.7f),
                fontSize = 11.sp,
            )
            val pm = textMeasurer.measure(paramText, paramStyle)
            val px = w - pm.size.width - 12f
            if (px > labelMeasured.size.width + 16f) {
                drawText(
                    textLayoutResult = pm,
                    topLeft = Offset(px, topY + (blockH - pm.size.height) / 2f)
                )
            }
        }
    }
}
