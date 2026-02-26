package com.robocar.app.scratch

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.robocar.app.model.BlockCategory
import com.robocar.app.model.BlockType

// ─────────────────────────────────────────────────────────────────────
// SCRATCH TOOLBOX — точна копія .blocklyToolboxDiv
//
// Оригінал:
//  • Нижня панель: фон #1f2937, висота 64px, горизонтальний скрол
//  • Кнопки категорій: тільки кольоровий квадрат (без тексту!)
//  • Flyout: #1f2937, горизонтальний список блоків
//  • Блоки у flyout: точна копія Blockly (форма, stripe, bubble params)
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ScratchToolbox(
    onDragBlockStart: (BlockType, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<BlockCategory?>(null) }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

        // ── Flyout ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = selected != null,
            enter   = slideInVertically(tween(200, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(160)),
            exit    = slideOutVertically(tween(180)) { it } + fadeOut(tween(140)),
        ) {
            selected?.let { cat ->
                BlocklyFlyout(
                    category    = cat,
                    onDragStart = { type, sx, sy -> onDragBlockStart(type, sx, sy); selected = null },
                    onClose     = { selected = null },
                )
            }
        }

        // ── Нижня панель ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2937))
                .drawBehind {
                    drawLine(Color(0x26FFFFFF), Offset(0f, 0f), Offset(size.width, 0f), 1f)
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .height(64.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BlockCategory.values().forEach { cat ->
                    CategoryDot(
                        category   = cat,
                        isSelected = cat == selected,
                        onClick    = { selected = if (selected == cat) null else cat },
                    )
                }
            }
        }
    }
}

// ── Кнопка категорії — ТІЛЬКИ кольоровий квадрат (без тексту) ─────────
@Composable
private fun CategoryDot(category: BlockCategory, isSelected: Boolean, onClick: () -> Unit) {
    val col  = Color(category.color)
    val size by animateDpAsState(if (isSelected) 18.dp else 14.dp, spring(Spring.DampingRatioMediumBouncy), label = "sz")
    val bgA  by animateFloatAsState(if (isSelected) 0.18f else 0f, tween(180), label = "bg")
    val glow by animateFloatAsState(if (isSelected) 1f else 0f, tween(200), label = "gw")

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(col.copy(bgA))
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) col else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Кольоровий квадрат
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(3.dp))
                .background(col)
                .drawBehind {
                    if (glow > 0f) {
                        drawRoundRect(
                            col.copy(glow * 0.5f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
                            style = Stroke(4f),
                        )
                    }
                }
        )
    }
}

// ── Flyout ─────────────────────────────────────────────────────────────
@Composable
private fun BlocklyFlyout(
    category: BlockCategory,
    onDragStart: (BlockType, Float, Float) -> Unit,
    onClose: () -> Unit,
) {
    val col    = Color(category.color)
    val blocks = BlockType.values().filter { it.category == category }

    Column(modifier = Modifier.fillMaxWidth().background(Color(0xF21F2937))) {
        // Кольорова лінія зверху
        Box(Modifier.fillMaxWidth().height(2.dp).background(
            Brush.horizontalGradient(listOf(Color.Transparent, col, col, Color.Transparent))
        ))

        // Заголовок — кольорова назва + кнопка закрити
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(col))
                Text(
                    category.label.uppercase(),
                    fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    color = col, letterSpacing = 1.5.sp,
                )
            }
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape)
                    .background(Color(0xFF334155)).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            }
        }

        // Горизонтальний ряд блоків
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(108.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 100.dp, top = 4.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(blocks, key = { it.name }) { type ->
                FlyoutBlock(type = type, onDragStart = { sx, sy -> onDragStart(type, sx, sy) })
            }
        }
    }
}

// ── Flyout Block — точна копія Blockly блоку ───────────────────────────
@Composable
private fun FlyoutBlock(type: BlockType, onDragStart: (Float, Float) -> Unit) {
    val col  = blockColor(type)
    val colD = blockColorDark(type)
    val colL = blockColorLight(type)

    // Висота: hat block вищий
    val blockH = if (!type.hasPrev) 56.dp else 48.dp

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "sc"
    )

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(blockH)
            .scale(scale)
            .pointerInput(type) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    var didDrag = false

                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull() ?: break

                        if (!ch.pressed) {
                            pressed = false
                            if (!didDrag) onDragStart(size.width / 2f, size.height / 2f)
                            break
                        }

                        val d = ch.position - down.position
                        if (!didDrag && (kotlin.math.abs(d.x) > 8f || kotlin.math.abs(d.y) > 8f)) {
                            didDrag = true
                            pressed = false
                            onDragStart(ch.position.x, ch.position.y)
                            break
                        }
                        ch.consume()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Тінь
            val sh = if (!type.hasPrev) BlockPaths.hatBlock(2f, 3f, w, h, type.hasNext)
                     else BlockPaths.statementBlock(2f, 3f, w, h, type.hasPrev, type.hasNext)
            drawPath(sh, Color(0x55000000))

            // Тіло
            val body = if (!type.hasPrev) BlockPaths.hatBlock(0f, 0f, w, h, type.hasNext)
                       else BlockPaths.statementBlock(0f, 0f, w, h, type.hasPrev, type.hasNext)
            drawPath(body, Brush.verticalGradient(listOf(colL, col), 0f, h))

            // Ліва смужка
            val stripe = if (!type.hasPrev) BlockPaths.hatBlock(0f, 0f, BlockDimensions.STRIPE_W, h, false)
                         else BlockPaths.statementBlock(0f, 0f, BlockDimensions.STRIPE_W, h, type.hasPrev, false)
            drawPath(stripe, colD)

            // Обведення
            drawPath(body, colD.copy(0.55f), style = Stroke(1.2f))
        }

        // Текст label
        val topOff = if (!type.hasPrev) (BlockDimensions.HAT_EXTRA * 0.2f).dp else 0.dp
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(start = (BlockDimensions.STRIPE_W + 8f).dp, end = 4.dp, top = topOff),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                blockLabel(type), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
