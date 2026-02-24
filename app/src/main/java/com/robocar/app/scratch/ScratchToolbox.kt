package com.robocar.app.scratch

// ═══════════════════════════════════════════════════════════════════════
// SCRATCH TOOLBOX — точна копія .blocklyToolboxDiv + flyout
//
// Оригінальний CSS:
// .blocklyToolboxDiv {
//   border: 1px solid rgba(255,255,255,0.15);
//   position: absolute; bottom: 40px; left: 50%;
//   transform: translateX(-50%);
//   height: 64px;
//   border-radius: 16px;
//   box-shadow: 0 10px 40px rgba(0,0,0,0.6);
//   display: flex; flex-direction: row; align-items: center;
//   justify-content: center;
//   padding: 0 10px;
//   background-color: #1f2937  ← menuBg
// }
// .blocklyTreeRow {
//   height: 48px; padding: 0 15px; margin: 0 4px;
//   border-radius: 10px;
//   border: 1px solid transparent;
//   display: flex; align-items: center;
// }
// .blocklyTreeLabel { font-family:'Segoe UI'; font-weight:700; font-size:13px }
// Активна: blocklyTreeSelected → background: #1f2937
//
// Flyout:
//   flyoutBackgroundColour: '#1f2937'
//   flyoutOpacity: 0.95
//   horizontalLayout: true
// ═══════════════════════════════════════════════════════════════════════

import androidx.compose.animation.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.awaitEachGesture
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.waitForUpOrCancellation
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.robocar.app.model.BlockCategory
import com.robocar.app.model.BlockType

// ───────────────────────────────────────────────────────────────────────
// ГОЛОВНИЙ КОМПОНЕНТ
// ───────────────────────────────────────────────────────────────────────
@Composable
fun ScratchToolbox(
    onDragBlockStart: (BlockType, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<BlockCategory?>(null) }

    Column(
        modifier            = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Flyout — виїжджає вгору ──────────────────────────
        AnimatedVisibility(
            visible = selected != null,
            enter   = slideInVertically(
                initialOffsetY = { it },
                animationSpec  = tween(220, easing = FastOutSlowInEasing),
            ) + fadeIn(tween(180)),
            exit    = slideOutVertically(
                targetOffsetY  = { it },
                animationSpec  = tween(200),
            ) + fadeOut(tween(150)),
        ) {
            selected?.let { cat ->
                BlocklyFlyout(
                    category    = cat,
                    onDragStart = onDragBlockStart,
                    onClose     = { selected = null },
                )
            }
        }

        // ── Нижня панель ─────────────────────────────────────
        // background-color: #1f2937 (menuBg)
        // border-top: 1px solid rgba(255,255,255,0.08)
        Box(
            modifier          = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2937))   // menuBg: '#1f2937'
                .drawBehind {
                    drawLine(
                        Color(0x14FFFFFF),
                        Offset(0f, 0f),
                        Offset(size.width, 0f),
                        1f,
                    )
                },
            contentAlignment  = Alignment.Center,
        ) {
            // .blocklyToolboxDiv — горизонтальний скрол
            // height:64px; padding: 0 10px
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .height(64.dp)                  // height: 64px
                    .padding(horizontal = 10.dp),   // padding: 0 10px
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                BlockCategory.values().forEach { cat ->
                    BlocklyTreeRow(
                        category   = cat,
                        isSelected = cat == selected,
                        onClick    = {
                            selected = if (selected == cat) null else cat
                        },
                    )
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────
// BLOCKLY TREE ROW — одна категорія
//
// .blocklyTreeRow {
//   height: 48px; padding: 0 15px; margin: 0 4px;
//   border-radius: 10px; border: 1px solid transparent;
//   display: flex; align-items: center; justify-content: center;
// }
// .blocklyTreeLabel { font-weight: 700; font-size: 13px }
// Активна: border-color кольором категорії + легкий фон
// ───────────────────────────────────────────────────────────────────────
@Composable
private fun BlocklyTreeRow(
    category: BlockCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val col = categoryColor(category)

    val bgAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 0.14f else 0f,
        animationSpec = tween(180),
        label         = "tr_bg",
    )
    val borderAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = tween(180),
        label         = "tr_border",
    )
    val labelAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0.7f,
        animationSpec = tween(180),
        label         = "tr_label",
    )

    Row(
        modifier = Modifier
            .height(48.dp)                        // height: 48px
            .padding(horizontal = 4.dp)           // margin: 0 4px
            .clip(RoundedCornerShape(10.dp))      // border-radius: 10px
            .background(col.copy(alpha = bgAlpha))
            .border(
                width = 1.dp,
                color = col.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),         // padding: 0 15px
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        // Кольоровий індикатор (квадрат кольором категорії)
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(col),
        )

        // .blocklyTreeLabel { font-weight:700; font-size:13px }
        Text(
            text       = category.label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,          // font-weight: 700
            color      = Color.White.copy(alpha = labelAlpha),
        )
    }
}

// ───────────────────────────────────────────────────────────────────────
// BLOCKLY FLYOUT — панель блоків
//
// flyoutBackgroundColour: '#1f2937'
// flyoutOpacity: 0.95
// horizontalLayout: true  ← горизонтальний список
// ───────────────────────────────────────────────────────────────────────
@Composable
private fun BlocklyFlyout(
    category: BlockCategory,
    onDragStart: (BlockType, Float, Float) -> Unit,
    onClose: () -> Unit,
) {
    val col    = categoryColor(category)
    val blocks = BlockType.values().filter { it.category == category }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // flyoutBackgroundColour: '#1f2937', flyoutOpacity: 0.95
            .background(Color(0xF21F2937)),
    ) {
        Column {
            // Лінія кольором категорії зверху
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, col, col, Color.Transparent)
                        )
                    )
            )

            // Заголовок flyout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text          = category.label.uppercase(),
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = col,
                    letterSpacing = 1.5.sp,
                )
                // Кнопка закрити
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "×",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF94A3B8),
                    )
                }
            }

            // Горизонтальний список блоків (horizontalLayout: true)
            LazyRow(
                contentPadding        = PaddingValues(
                    start  = 12.dp,
                    end    = 80.dp,  // місце для FAB
                    bottom = 12.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                items(blocks, key = { it.name }) { type ->
                    FlyoutBlock(
                        type        = type,
                        onDragStart = { sx, sy -> onDragStart(type, sx, sy) },
                    )
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────
// FLYOUT BLOCK — один блок у flyout
// Виглядає як справжній Blockly блок
// Tap/Long press → drag на workspace
// ───────────────────────────────────────────────────────────────────────
@Composable
private fun FlyoutBlock(
    type: BlockType,
    onDragStart: (Float, Float) -> Unit,
) {
    val col  = blockColor(type)
    val colD = blockColorDark(type)
    val colL = blockColorLight(type)

    val bw = 150.dp
    val bh = if (!type.hasPrev) 46.dp else 40.dp

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow,
        ),
        label = "fly_scale",
    )

    Box(
        modifier = Modifier
            .width(bw)
            .height(bh)
            .scale(scale)
            // Long press drag
            .pointerInput(type) {
                detectDragGesturesAfterLongPress(
                    onDragStart  = { off ->
                        pressed = false
                        onDragStart(off.x, off.y)
                    },
                    onDrag       = { _, _ -> },
                    onDragEnd    = { },
                    onDragCancel = { pressed = false },
                )
            }
            // Tap → drag від центру
            .pointerInput(type) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    val up = waitForUpOrCancellation()
                    pressed = false
                    if (up != null) {
                        onDragStart(size.width / 2f, size.height / 2f)
                    }
                }
            },
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val shadow = if (!type.hasPrev)
                BlockPaths.hatBlock(1.5f, 2.5f, w, h, type.hasNext)
            else
                BlockPaths.statementBlock(1.5f, 2.5f, w, h, type.hasPrev, type.hasNext)
            drawPath(shadow, Color(0x55000000))

            val body = if (!type.hasPrev)
                BlockPaths.hatBlock(0f, 0f, w, h, type.hasNext)
            else
                BlockPaths.statementBlock(0f, 0f, w, h, type.hasPrev, type.hasNext)

            drawPath(
                path  = body,
                brush = Brush.verticalGradient(
                    colors = listOf(colL, col),
                    startY = 0f, endY = h,
                )
            )

            val stripe = if (!type.hasPrev)
                BlockPaths.hatBlock(0f, 0f, BlockDimensions.STRIPE_W, h, false)
            else
                BlockPaths.statementBlock(0f, 0f, BlockDimensions.STRIPE_W, h, type.hasPrev, false)
            drawPath(stripe, colD)
            drawPath(body, colD.copy(0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
        }

        // Текст
        val label = blockLabel(type)
        val hatOff = if (!type.hasPrev) BlockDimensions.HAT_EXTRA * 0.3f else 0f
        val bodyH  = if (!type.hasPrev) bh.value - BlockDimensions.HAT_EXTRA else bh.value

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = (BlockDimensions.STRIPE_W + 8).dp,
                    end   = 4.dp,
                    top   = hatOff.dp,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text       = label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }
    }
}
