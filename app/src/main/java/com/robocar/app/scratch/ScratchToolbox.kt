package com.robocar.app.scratch

// ════════════════════════════════════════════════════════════════════════
// SCRATCH TOOLBOX  v4  —  Scratch-стиль, drag-and-drop
//
// ОСОБЛИВОСТІ:
//  • Категорії — тільки кольоровий кружок (назва лише у відкритому flyout)
//  • Назви і кольори категорій точно як у index82.html
//  • Drag-and-drop: починається одразу при русі пальця (НЕ long press)
//    Tap теж поміщає блок на canvas (у режимі "tap-to-place")
//  • Координати drag — screen-координати (через onGloballyPositioned)
//  • Всі зворотні виклики потрібні для продовження drag за межами flyout:
//    onDragBlockStart → vm.startDragFromToolbar
//    onDragUpdate     → vm.updateDrag
//    onDragEnd        → vm.endDrag
// ════════════════════════════════════════════════════════════════════════

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.robocar.app.model.BlockCategory
import com.robocar.app.model.BlockType

// ─────────────────────────────────────────────────────────────────────────
// TOOLBOX CATEGORY — розширена версія для точного mapping
// ─────────────────────────────────────────────────────────────────────────
data class ToolboxCategory(
    val id: String,
    val emoji: String,
    val label: String,
    val color: Color,
    val blockCategory: BlockCategory,
)

// Категорії — відповідають BlockCategory enum з BlockModels.kt
// Назви і кольори — з index82.html
val TOOLBOX_CATEGORIES = listOf(
    ToolboxCategory("motion",   "🚗", "Рух",        Color(0xFF3B7DD8), BlockCategory.CAR),
    ToolboxCategory("timing",   "⏱",  "Керування",  Color(0xFFFFBF00), BlockCategory.CONTROL),
    ToolboxCategory("loops",    "🔁", "Цикли",      Color(0xFF5BA55B), BlockCategory.LOOPS),
    ToolboxCategory("logic",    "🧠", "Логіка",     Color(0xFF5CB1D6), BlockCategory.LOGIC),
    ToolboxCategory("sensors",  "📡", "Сенсори",    Color(0xFF00897B), BlockCategory.SENSORS),
    ToolboxCategory("math",     "📐", "Математика", Color(0xFF9966FF), BlockCategory.MATH),
    ToolboxCategory("state",    "🏁", "Стан",       Color(0xFF8E24AA), BlockCategory.STATE),
    ToolboxCategory("smart",    "⚙",  "Контроль",   Color(0xFFB36C0C), BlockCategory.SMART),
    ToolboxCategory("vars",     "📦", "Змінні",     Color(0xFFFF8C1A), BlockCategory.VARIABLES),
)

// ─────────────────────────────────────────────────────────────────────────
// ГОЛОВНИЙ КОМПОНЕНТ
//
// onDragBlockStart  — повідомляє ViewModel що почався drag нового блоку
// onDragUpdate      — оновлює позицію drag (screen coords)
// onDragEnd         — завершує drag і розміщає блок
// screenHeight      — висота екрана (для trash detection)
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun ScratchToolbox(
    onDragBlockStart: (BlockType, Float, Float) -> Unit,
    onDragUpdate: (Float, Float) -> Unit = { _, _ -> },
    onDragEnd: (Float, Float, Float) -> Unit = { _, _, _ -> },
    screenHeight: Float = 1000f,
    modifier: Modifier = Modifier,
) {
    var selectedCat by remember { mutableStateOf<ToolboxCategory?>(null) }

    Column(
        modifier            = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Flyout — виїжджає вгору ──────────────────────────
        AnimatedVisibility(
            visible = selectedCat != null,
            enter   = slideInVertically(
                initialOffsetY = { it },
                animationSpec  = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness    = Spring.StiffnessMedium,
                ),
            ) + fadeIn(tween(160)),
            exit    = slideOutVertically(
                targetOffsetY  = { it },
                animationSpec  = tween(180),
            ) + fadeOut(tween(120)),
        ) {
            selectedCat?.let { cat ->
                ToolboxFlyout(
                    category        = cat,
                    onDragStart     = { type, sx, sy ->
                        selectedCat = null  // закриваємо flyout одразу при drag
                        onDragBlockStart(type, sx, sy)
                    },
                    onDragUpdate    = onDragUpdate,
                    onDragEnd       = { sx, sy -> onDragEnd(sx, sy, screenHeight) },
                    onClose         = { selectedCat = null },
                )
            }
        }

        // ── Нижня панель з кружками категорій ──────────────
        ToolboxBar(
            selected = selectedCat,
            onSelect = { cat ->
                selectedCat = if (selectedCat?.id == cat.id) null else cat
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// TOOLBOX BAR — горизонтальний ряд кружків (без тексту!)
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun ToolboxBar(
    selected: ToolboxCategory?,
    onSelect: (ToolboxCategory) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F2937))
            .drawBehind {
                // Верхня лінія-роздільник
                drawLine(
                    Color(0x18FFFFFF),
                    Offset(0f, 0f),
                    Offset(size.width, 0f),
                    1f,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .height(58.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TOOLBOX_CATEGORIES.forEach { cat ->
                CategoryDot(
                    category   = cat,
                    isSelected = selected?.id == cat.id,
                    onClick    = { onSelect(cat) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// CATEGORY DOT — кружок без тексту
// При активному стані — більший + білий border
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryDot(
    category: ToolboxCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val dotSize by animateDpAsState(
        if (isSelected) 38.dp else 30.dp,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "dot_size",
    )
    val dotAlpha by animateFloatAsState(
        if (isSelected) 1f else 0.80f,
        tween(160),
        label = "dot_alpha",
    )
    val borderWidth by animateDpAsState(
        if (isSelected) 2.dp else 0.dp,
        tween(160),
        label = "dot_border",
    )

    Box(
        modifier = Modifier
            .size(44.dp)                         // tap target
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Зовнішній glow при активному стані
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(dotSize + 6.dp)
                    .clip(CircleShape)
                    .background(category.color.copy(alpha = 0.25f)),
            )
        }

        Box(
            modifier = Modifier
                .size(dotSize)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(category.color)
                .border(borderWidth, Color.White.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = category.emoji,
                fontSize = if (isSelected) 15.sp else 12.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// TOOLBOX FLYOUT — панель блоків категорії
// Показує назву категорії + горизонтальний список блоків
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun ToolboxFlyout(
    category: ToolboxCategory,
    onDragStart: (BlockType, Float, Float) -> Unit,
    onDragUpdate: (Float, Float) -> Unit,
    onDragEnd: (Float, Float) -> Unit,
    onClose: () -> Unit,
) {
    val col    = category.color
    val blocks = BlockType.values().filter { it.category == category.blockCategory }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xF21F2937)),
    ) {
        Column {
            // ── Кольорова лінія зверху ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                col,
                                col,
                                col,
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            // ── Заголовок flyout ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Emoji + назва категорії
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(col),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(category.emoji, fontSize = 10.sp)
                    }
                    // Назва показується тільки тут (у flyout)
                    Text(
                        text          = category.label.uppercase(),
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = col,
                        letterSpacing = 1.8.sp,
                    )
                }

                // Кнопка закрити
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "×",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF94A3B8),
                    )
                }
            }

            // ── Горизонтальний список блоків ──
            LazyRow(
                contentPadding        = PaddingValues(
                    start  = 12.dp,
                    end    = 60.dp,
                    bottom = 14.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                items(blocks, key = { it.name }) { type ->
                    DraggableFlyoutBlock(
                        type         = type,
                        catColor     = col,
                        onDragStart  = { sx, sy -> onDragStart(type, sx, sy) },
                        onDragUpdate = onDragUpdate,
                        onDragEnd    = onDragEnd,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// DRAGGABLE FLYOUT BLOCK — один блок у flyout
//
// DRAG ЛОГІКА (справжня, Scratch-стиль):
//   1. Touch down → чекаємо рух або відпускання
//   2. Рух > DRAG_THRESHOLD → запускаємо drag ОДРАЗУ (без long press)
//   3. Tap (відпустив без руху) → drag від центру блоку
//   4. Всі координати → screen-абсолютні (через onGloballyPositioned)
//   5. Після onDragStart продовжуємо відстежувати pointer:
//      кожен рух → onDragUpdate(screenX, screenY)
//      відпускання → onDragEnd(screenX, screenY)
//
// ВІДОБРАЖЕННЯ:
//   — Zelos renderer (заокруглені кути, notch, hat, gradient)
//   — Анімація scale при press
// ─────────────────────────────────────────────────────────────────────────
private const val DRAG_THRESHOLD_PX = 10f

@Composable
private fun DraggableFlyoutBlock(
    type: BlockType,
    catColor: Color,
    onDragStart: (screenX: Float, screenY: Float) -> Unit,
    onDragUpdate: (screenX: Float, screenY: Float) -> Unit,
    onDragEnd: (screenX: Float, screenY: Float) -> Unit,
) {
    val col  = blockColor(type)
    val colD = blockColorDark(type)
    val colL = blockColorLight(type)

    val blockW = 150.dp
    val blockH = if (!type.hasPrev) 48.dp else 40.dp

    var pressed     by remember { mutableStateOf(false) }
    var globalOffset by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow,
        ),
        label = "flyout_scale",
    )

    Box(
        modifier = Modifier
            .width(blockW)
            .height(blockH)
            .scale(scale)
            // Зберігаємо абсолютну позицію на екрані
            .onGloballyPositioned { layoutCoords ->
                globalOffset = layoutCoords.positionInRoot()
            }
            // ── ЄДИНИЙ gesture handler ─────────────────────
            .pointerInput(type) {
                awaitEachGesture {
                    // 1. Чекаємо натискання
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true

                    val startLocal = down.position
                    // Абсолютні screen координати початку
                    val startScreen = globalOffset + startLocal

                    var dragStarted = false

                    // 2. Відстежуємо рухи поки не відпустили
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        val ch = ev.changes.firstOrNull() ?: break

                        // Відпустили палець
                        if (!ch.pressed) {
                            pressed = false

                            if (!dragStarted) {
                                // TAP: розміщаємо блок від центру
                                val centerScreen = globalOffset + Offset(
                                    size.width / 2f,
                                    size.height / 2f,
                                )
                                onDragStart(centerScreen.x, centerScreen.y)
                                onDragEnd(centerScreen.x, centerScreen.y)
                            } else {
                                // DRAG END: відпустили після drag
                                val curScreen = globalOffset + ch.position
                                onDragEnd(curScreen.x, curScreen.y)
                            }
                            break
                        }

                        // Перевіряємо чи достатньо переміщення для drag
                        val localMoved = (ch.position - startLocal).getDistance()
                        if (localMoved > DRAG_THRESHOLD_PX && !dragStarted) {
                            dragStarted = true
                            pressed     = false
                            ch.consume()
                            // Початок drag — передаємо поточні screen coordinates
                            val curScreen = globalOffset + ch.position
                            onDragStart(curScreen.x, curScreen.y)
                        }

                        // Якщо drag активний — оновлюємо позицію
                        if (dragStarted) {
                            ch.consume()
                            val curScreen = globalOffset + ch.position
                            onDragUpdate(curScreen.x, curScreen.y)
                        }
                    }
                    pressed = false
                }
            },
    ) {
        // ── Canvas малює блок у Zelos стилі ──────────────────
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Тінь
            val shadow = if (!type.hasPrev)
                BlockPaths.hatBlock(2f, 3f, w, h, type.hasNext)
            else
                BlockPaths.statementBlock(2f, 3f, w, h, type.hasPrev, type.hasNext)
            drawPath(shadow, Color(0x55000000))

            // Основне тіло
            val body = if (!type.hasPrev)
                BlockPaths.hatBlock(0f, 0f, w, h, type.hasNext)
            else
                BlockPaths.statementBlock(0f, 0f, w, h, type.hasPrev, type.hasNext)

            // Градієнт зверху вниз (колір категорії з index82)
            drawPath(
                path  = body,
                brush = Brush.verticalGradient(
                    colors = listOf(colL, col),
                    startY = 0f, endY = h,
                ),
            )

            // Ліва темна смужка (stripe — як у Blockly)
            val stripe = if (!type.hasPrev)
                BlockPaths.hatBlock(0f, 0f, BlockDimensions.STRIPE_W, h, false)
            else
                BlockPaths.statementBlock(0f, 0f, BlockDimensions.STRIPE_W, h, type.hasPrev, false)
            drawPath(stripe, colD)

            // Обведення
            drawPath(body, colD.copy(alpha = 0.65f), style = Stroke(1.2f))

            // Puzzle connector highlight
            if (type.hasPrev) {
                val nx = BlockDimensions.NOTCH_X + 2f
                val nw = BlockDimensions.NOTCH_W - 4f
                val nh = BlockDimensions.NOTCH_H * 0.5f
                val p = Path().apply {
                    moveTo(nx,      nh * 0.2f)
                    lineTo(nw + nx, nh * 0.2f)
                }
                drawPath(p, Color(0x44FFFFFF), style = Stroke(1.5f))
            }
            if (type.hasNext) {
                val nx = BlockDimensions.NOTCH_X + 2f
                val nw = BlockDimensions.NOTCH_W - 4f
                val p = Path().apply {
                    moveTo(nx,      h - 1f)
                    lineTo(nw + nx, h - 1f)
                }
                drawPath(p, Color(0x33FFFFFF), style = Stroke(1.2f))
            }
        }

        // ── Текст ─────────────────────────────────────────────
        val label     = blockLabel(type)
        val hatOffset = if (!type.hasPrev) BlockDimensions.HAT_EXTRA * 0.25f else 0f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = (BlockDimensions.STRIPE_W + 6).dp,
                    end   = 4.dp,
                    top   = hatOffset.dp,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text       = label,
                fontSize   = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// HELPER — колір категорії (використовується ззовні)
// ─────────────────────────────────────────────────────────────────────────
fun toolboxCategoryColor(cat: BlockCategory): Color =
    TOOLBOX_CATEGORIES.find { it.blockCategory == cat }?.color
        ?: categoryColor(cat)
