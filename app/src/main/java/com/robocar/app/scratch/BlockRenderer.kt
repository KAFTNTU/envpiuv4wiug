package com.robocar.app.scratch

// ═══════════════════════════════════════════════════════════════
// BLOCK RENDERER — точна копія Blockly zelos renderer
//
// Кольори блоків з оригінального файлу (setColour(hue)):
//   start_hat     → setColour(120) → #5BA55B  зелений
//   robot_move    → setColour(230) → #4C97FF  синій
//   move_4_motors → setColour(260) → #5C6BC0  індиго
//   motor_single  → setColour(260) → #5C6BC0
//   robot_stop    → setColour(0)   → #FF6680  червоний
//   wait_seconds  → setColour(40)  → #FFBF00  жовтий
//   sensor_get    → setColour(180) → #5CB1D6  блакитний
//   go_home       → setColour(290) → #8E24AA  фіолетовий
//   console_log   → setColour(60)  → #FFAB19  помаранчевий
//
// Категорії toolbox (colour=""):
//   Машинка:    #4C97FF
//   Логіка:     #5CB1D6
//   Цикли:      #5BA55B
//   Математика: #9966FF
//   Час:        #FFBF00
//   Змінні:     #FF8C1A
//   Стан:       #8E24AA
//   Контроль:   #E65100
//
// Zelos renderer характеристики:
//   - Великі заокруглені кути (cornerRadius ≈ 8-15px)
//   - Pill-подібні hat-блоки
//   - Notch ширина: 15px, висота: 4px (плаский notch)
//   - Блок мінімальна висота: 48px
//   - Шрифт: Roboto/Segoe UI Bold 12sp
// ═══════════════════════════════════════════════════════════════

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import com.robocar.app.model.BlockCategory
import com.robocar.app.model.BlockParam
import com.robocar.app.model.BlockType
import kotlin.math.min
import kotlin.math.max

// ───────────────────────────────────────────────────────────────
// КОЛІР КОЖНОГО БЛОКУ — точно як setColour(hue) в оригіналі
// Blockly HSV: hue → приблизний hex через палітру Blockly
// ───────────────────────────────────────────────────────────────
fun blockColor(type: BlockType): Color = when (type) {
    // ── СТАРТ ──────────────────────────────────────────────────
    BlockType.START_HAT           -> Color(0xFF5BA55B)  // setColour(120) зелений

    // ── МАШИНКА (Їхати, Мотори) ─────────────────────────────
    BlockType.ROBOT_MOVE          -> Color(0xFF4C97FF)  // setColour(230) синій
    BlockType.ROBOT_MOVE_SOFT     -> Color(0xFF4C97FF)
    BlockType.ROBOT_TURN          -> Color(0xFF4C97FF)
    BlockType.ROBOT_SET_SPEED     -> Color(0xFF4C97FF)
    BlockType.ROBOT_STOP          -> Color(0xFFFF6680)  // setColour(0)   червоний
    BlockType.MOTOR_SINGLE        -> Color(0xFF5C6BC0)  // setColour(260) індиго
    BlockType.MOTOR_4             -> Color(0xFF5C6BC0)  // setColour(260)
    BlockType.GO_HOME             -> Color(0xFF8E24AA)  // setColour(290) фіолетовий
    BlockType.CONSOLE_LOG         -> Color(0xFFFFAB19)  // setColour(60)  помаранчевий

    // ── СЕНСОРИ ────────────────────────────────────────────────
    BlockType.SENSOR_GET          -> Color(0xFF5CB1D6)  // setColour(180) блакитний
    BlockType.WAIT_UNTIL_SENSOR   -> Color(0xFF5CB1D6)
    BlockType.WAIT_UNTIL_TRUE_FOR -> Color(0xFF5CB1D6)
    BlockType.AUTOPILOT           -> Color(0xFF5CB1D6)

    // ── ЧАС / WAIT ─────────────────────────────────────────────
    BlockType.WAIT_SECONDS        -> Color(0xFFFFBF00)  // setColour(40)  жовтий

    // ── ЦИКЛИ (Blockly BKY_LOOPS_HUE) ─────────────────────────
    BlockType.LOOP_FOREVER        -> Color(0xFF5BA55B)  // зелений
    BlockType.LOOP_REPEAT         -> Color(0xFF5BA55B)
    BlockType.LOOP_REPEAT_PAUSE   -> Color(0xFF5BA55B)
    BlockType.LOOP_EVERY_SEC      -> Color(0xFF5BA55B)
    BlockType.LOOP_WHILE          -> Color(0xFF5BA55B)
    BlockType.LOOP_FOR            -> Color(0xFF5BA55B)
    BlockType.LOOP_FOR_EACH       -> Color(0xFF5BA55B)

    // ── ЛОГІКА (BKY_LOGIC_HUE) ────────────────────────────────
    BlockType.LOGIC_IF            -> Color(0xFF5CB1D6)  // блакитний
    BlockType.LOGIC_COMPARE       -> Color(0xFF5CB1D6)
    BlockType.LOGIC_AND_OR        -> Color(0xFF5CB1D6)
    BlockType.LOGIC_NOT           -> Color(0xFF5CB1D6)
    BlockType.LOGIC_BOOL          -> Color(0xFF5CB1D6)

    // ── МАТЕМАТИКА (BKY_MATH_HUE) ─────────────────────────────
    BlockType.MATH_NUMBER         -> Color(0xFF9966FF)  // фіолетовий
    BlockType.MATH_ARITH          -> Color(0xFF9966FF)
    BlockType.MATH_RANDOM         -> Color(0xFF9966FF)
    BlockType.MATH_ROUND          -> Color(0xFF9966FF)
    BlockType.MATH_MODULO         -> Color(0xFF9966FF)
    BlockType.MATH_PID            -> Color(0xFF9966FF)
    BlockType.MATH_SMOOTH         -> Color(0xFF9966FF)

    // ── ЗМІННІ (BKY_VARIABLES_HUE) ────────────────────────────
    BlockType.VAR_SET             -> Color(0xFFFF8C1A)  // помаранчевий
    BlockType.VAR_GET             -> Color(0xFFFF8C1A)
    BlockType.VAR_CHANGE          -> Color(0xFFFF8C1A)

    // ── СТАН ───────────────────────────────────────────────────
    BlockType.STATE_SET           -> Color(0xFF8E24AA)  // фіолетовий
    BlockType.STATE_SET_REASON    -> Color(0xFF8E24AA)
    BlockType.STATE_PREV          -> Color(0xFF8E24AA)
    BlockType.STATE_IF            -> Color(0xFF8E24AA)
    BlockType.STATE_GET           -> Color(0xFF8E24AA)
    BlockType.STATE_TIME_S        -> Color(0xFF8E24AA)

    // ── КОНТРОЛЬ / SMART ────────────────────────────────────────
    BlockType.COOLDOWN_DO         -> Color(0xFFE65100)  // темно-помаранчевий
    BlockType.TIMEOUT_DO_UNTIL    -> Color(0xFFE65100)
    BlockType.IF_TRUE_FOR         -> Color(0xFFE65100)
    BlockType.TIMER_RESET         -> Color(0xFFFFBF00)
    BlockType.TIMER_GET           -> Color(0xFFFFBF00)
    BlockType.LATCH_SET           -> Color(0xFFE65100)
    BlockType.LATCH_RESET         -> Color(0xFFE65100)
    BlockType.LATCH_GET           -> Color(0xFFE65100)
    BlockType.EDGE_DETECT         -> Color(0xFFE65100)
    BlockType.SCHMITT_TRIGGER     -> Color(0xFFE65100)

    // ── ЗАПИС / ВІДТВОРЕННЯ ─────────────────────────────────────
    BlockType.RECORD_START        -> Color(0xFFEF4444)  // червоний запис
    BlockType.REPLAY_TRACK        -> Color(0xFF3B82F6)  // синій відтворення
    BlockType.REPLAY_LOOP         -> Color(0xFF3B82F6)

    // ── РЕШТА ──────────────────────────────────────────────────
    else                          -> categoryColor(type.category)
}

// Темніша версія (для stripe + border) — як у Blockly shadow blocks
fun blockColorDark(type: BlockType): Color {
    val c = blockColor(type)
    return Color(c.red * 0.6f, c.green * 0.6f, c.blue * 0.6f, 1f)
}

// Світліша версія (для верху градієнту)
fun blockColorLight(type: BlockType): Color {
    val c = blockColor(type)
    return Color(
        min(c.red   + 0.15f, 1f),
        min(c.green + 0.15f, 1f),
        min(c.blue  + 0.15f, 1f),
        1f,
    )
}

// ───────────────────────────────────────────────────────────────
// КОЛІР КАТЕГОРІЇ (для toolbox)
// Точно як colour="" в <category> тегах
// ───────────────────────────────────────────────────────────────
fun categoryColor(cat: BlockCategory): Color = when (cat) {
    BlockCategory.CAR       -> Color(0xFF4C97FF)  // Машинка
    BlockCategory.CONTROL   -> Color(0xFFFFBF00)  // Час/Керування
    BlockCategory.LOGIC   -> Color(0xFF5CB1D6)  // Сенсори (180)
    BlockCategory.MATH      -> Color(0xFF9966FF)  // Математика
    BlockCategory.LOGIC     -> Color(0xFF8E24AA)  // Стан
    BlockCategory.LOOPS     -> Color(0xFFE65100)  // Контроль
    BlockCategory.LOGIC     -> Color(0xFF5CB1D6)  // Логіка
    BlockCategory.VARIABLES -> Color(0xFFFF8C1A)  // Змінні
    BlockCategory.LOOPS     -> Color(0xFF5BA55B)  // Цикли
}

fun categoryColorDark(cat: BlockCategory): Color {
    val c = categoryColor(cat)
    return Color(c.red * 0.6f, c.green * 0.6f, c.blue * 0.6f, 1f)
}

fun categoryColorLight(cat: BlockCategory): Color {
    val c = categoryColor(cat)
    return Color(min(c.red+0.15f,1f), min(c.green+0.15f,1f), min(c.blue+0.15f,1f), 1f)
}

// ───────────────────────────────────────────────────────────────
// ТЕКСТ БЛОКУ — точно як appendField() в оригіналі
// ───────────────────────────────────────────────────────────────
fun blockLabel(type: BlockType): String = when (type) {
    BlockType.START_HAT           -> "🏁 СТАРТ"
    BlockType.ROBOT_MOVE          -> "🚗 Їхати"
    BlockType.ROBOT_MOVE_SOFT     -> "🚗 Плавний старт"
    BlockType.ROBOT_TURN          -> "↩ Поворот"
    BlockType.ROBOT_SET_SPEED     -> "⚡ Швидкість"
    BlockType.ROBOT_STOP          -> "🛑 Стоп"
    BlockType.MOTOR_SINGLE        -> "⚙️ Мотор"
    BlockType.MOTOR_4             -> "🚙 4 Мотори (ABCD)"
    BlockType.GO_HOME             -> "🏠 Додому (Назад)"
    BlockType.CONSOLE_LOG         -> "📝 Лог"
    BlockType.SENSOR_GET          -> "📏 Сенсор"
    BlockType.WAIT_UNTIL_SENSOR   -> "📡 Чекати поки сенсор"
    BlockType.WAIT_UNTIL_TRUE_FOR -> "📡 Чекати поки вірно"
    BlockType.AUTOPILOT           -> "🤖 Автопілот"
    BlockType.WAIT_SECONDS        -> "⏳ Чекати"
    BlockType.LOOP_FOREVER        -> "🔁 Назавжди"
    BlockType.LOOP_REPEAT         -> "🔁 Повторити"
    BlockType.LOOP_REPEAT_PAUSE   -> "🔁 Повторити з паузою"
    BlockType.LOOP_EVERY_SEC      -> "⏱ Кожні N секунд"
    BlockType.LOOP_WHILE          -> "🔄 Поки / До"
    BlockType.LOOP_FOR            -> "🔢 Для від до"
    BlockType.LOOP_FOR_EACH       -> "📋 Для кожного"
    BlockType.LOGIC_IF            -> "🧠 Якщо"
    BlockType.LOGIC_COMPARE       -> "⚖️ Порівняти"
    BlockType.LOGIC_AND_OR        -> "🔗 І / АБО"
    BlockType.LOGIC_NOT           -> "❌ НЕ"
    BlockType.LOGIC_BOOL          -> "✓ True / False"
    BlockType.MATH_NUMBER         -> "# Число"
    BlockType.MATH_ARITH          -> "± Арифметика"
    BlockType.MATH_RANDOM         -> "🎲 Випадкове"
    BlockType.MATH_ROUND          -> "○ Заокруглити"
    BlockType.MATH_MODULO         -> "% Залишок"
    BlockType.MATH_PID            -> "📐 PID регулятор"
    BlockType.MATH_SMOOTH         -> "〰 Згладити"
    BlockType.VAR_SET             -> "📦 Встановити"
    BlockType.VAR_GET             -> "📦 Значення"
    BlockType.VAR_CHANGE          -> "📦 Змінити на"
    BlockType.STATE_SET           -> "🧠 Стан ="
    BlockType.STATE_SET_REASON    -> "🧠 Стан = (причина)"
    BlockType.STATE_PREV          -> "🔙 Попередній стан"
    BlockType.STATE_IF            -> "🧠 Якщо стан ="
    BlockType.STATE_GET           -> "🧠 Поточний стан"
    BlockType.STATE_TIME_S        -> "⏱ Час у стані"
    BlockType.COOLDOWN_DO         -> "🕒 Не частіше ніж"
    BlockType.TIMEOUT_DO_UNTIL    -> "⌛ Робити до умови"
    BlockType.IF_TRUE_FOR         -> "📊 Якщо вірно N сек"
    BlockType.TIMER_RESET         -> "⏱ Скинути таймер"
    BlockType.TIMER_GET           -> "⏱ Значення таймера"
    BlockType.LATCH_SET           -> "🚩 Прапор = true"
    BlockType.LATCH_RESET         -> "🚩 Прапор = false"
    BlockType.LATCH_GET           -> "🚩 Значення прапора"
    BlockType.EDGE_DETECT         -> "📈 Детектор фронту"
    BlockType.SCHMITT_TRIGGER     -> "〜 Тригер Шмітта"
    BlockType.RECORD_START        -> "⏺ Записати трасу"
    BlockType.REPLAY_TRACK        -> "▶ Відтворити трасу"
    BlockType.REPLAY_LOOP         -> "🔁 Відтворити N разів"
    else                          -> type.label
}

// ───────────────────────────────────────────────────────────────
// BLOCK PATHS — zelos renderer форми
//
// Zelos відрізняється від Classic:
//   - Більші corner radius (15px для hat, 8px для statement)
//   - Плоский notch (не трапеція, а прямокутник з заокругленнями)
//   - Hat block: повна таблетка зверху
// ───────────────────────────────────────────────────────────────
object BlockPaths {
    private val D = BlockDimensions

    // ── Notch (виріз зверху) — zelos стиль ─────────────────────
    // В zelos notch плаский і ширший
    private fun Path.addTopNotch(left: Float, top: Float) {
        val nx = left + D.NOTCH_X
        lineTo(nx,              top)
        lineTo(nx + 3f,         top + D.NOTCH_H * 0.6f)
        lineTo(nx + D.NOTCH_W - 3f, top + D.NOTCH_H * 0.6f)
        lineTo(nx + D.NOTCH_W,  top)
    }

    // ── Tab (виступ знизу) ────────────────────────────────────
    private fun Path.addBottomTab(left: Float, bottom: Float) {
        val nx = left + D.NOTCH_X
        lineTo(nx + D.NOTCH_W,  bottom)
        lineTo(nx + D.NOTCH_W - 3f, bottom + D.NOTCH_H * 0.65f)
        lineTo(nx + 3f,         bottom + D.NOTCH_H * 0.65f)
        lineTo(nx,              bottom)
    }

    // ── STATEMENT BLOCK ──────────────────────────────────────
    fun statementBlock(
        left: Float, top: Float,
        width: Float, height: Float,
        hasPrev: Boolean, hasNext: Boolean,
    ): Path = Path().apply {
        val r      = D.CORNER_R
        val right  = left + width
        val bottom = top + height

        moveTo(left + r, top)
        if (hasPrev) addTopNotch(left, top)
        lineTo(right - r, top)
        quadraticBezierTo(right, top, right, top + r)
        lineTo(right, bottom - r)
        quadraticBezierTo(right, bottom, right - r, bottom)
        if (hasNext) addBottomTab(left, bottom)
        lineTo(left + r, bottom)
        quadraticBezierTo(left, bottom, left, bottom - r)
        lineTo(left, top + r)
        quadraticBezierTo(left, top, left + r, top)
        close()
    }

    // ── HAT BLOCK — СТАРТ ───────────────────────────────────
    // Zelos hat: повна таблетка (pill) зверху — великий radius
    fun hatBlock(
        left: Float, top: Float,
        width: Float, height: Float,
        hasNext: Boolean,
    ): Path = Path().apply {
        val r      = D.CORNER_R
        val hatR   = height * 0.45f   // великий радіус для pill-форми
        val right  = left + width
        val bottom = top + height

        moveTo(left + hatR, top)
        lineTo(right - hatR, top)
        // Верхній правий кут pill
        quadraticBezierTo(right, top, right, top + hatR)
        lineTo(right, bottom - r)
        quadraticBezierTo(right, bottom, right - r, bottom)
        if (hasNext) addBottomTab(left, bottom)
        lineTo(left + r, bottom)
        quadraticBezierTo(left, bottom, left, bottom - r)
        lineTo(left, top + hatR)
        // Верхній лівий кут pill
        quadraticBezierTo(left, top, left + hatR, top)
        close()
    }

    // ── C-BLOCK — цикл з тілом ──────────────────────────────
    fun cBlock(
        left: Float, top: Float, width: Float,
        headerH: Float, innerH: Float,
        hasPrev: Boolean, hasNext: Boolean,
        hasElse: Boolean, elseInnerH: Float,
    ): Path = Path().apply {
        val r         = D.CORNER_R
        val armW      = D.C_ARM_WIDTH
        val botH      = D.C_BOTTOM_H
        val right     = left + width
        val innerLeft = left + armW
        var y         = top

        // ── Заголовок зверху ────────────────────────────────
        moveTo(left + r, y)
        if (hasPrev) addTopNotch(left, y)
        lineTo(right - r, y)
        quadraticBezierTo(right, y, right, y + r)
        y += headerH
        lineTo(right, y - r)
        quadraticBezierTo(right, y, right - r, y)

        // ── DO-секція — верхній notch ────────────────────────
        val doNx = innerLeft + D.NOTCH_X
        lineTo(doNx + D.NOTCH_W, y)
        lineTo(doNx + D.NOTCH_W - 3f, y + D.NOTCH_H * 0.6f)
        lineTo(doNx + 3f, y + D.NOTCH_H * 0.6f)
        lineTo(doNx, y)
        lineTo(innerLeft + r, y)
        quadraticBezierTo(innerLeft, y, innerLeft, y + r)

        // Ліва стінка DO
        y += innerH
        lineTo(innerLeft, y - r)
        quadraticBezierTo(innerLeft, y, innerLeft + r, y)

        // Нижній tab DO
        lineTo(doNx + D.NOTCH_W, y)
        lineTo(doNx + D.NOTCH_W - 3f, y + D.NOTCH_H * 0.65f)
        lineTo(doNx + 3f, y + D.NOTCH_H * 0.65f)
        lineTo(doNx, y)

        if (hasElse) {
            // Нижня планка між DO і ELSE
            lineTo(right - r, y)
            quadraticBezierTo(right, y, right, y + r)
            y += botH
            lineTo(right, y - r)
            quadraticBezierTo(right, y, right - r, y)

            // ELSE notch зверху
            lineTo(doNx + D.NOTCH_W, y)
            lineTo(doNx + D.NOTCH_W - 3f, y + D.NOTCH_H * 0.6f)
            lineTo(doNx + 3f, y + D.NOTCH_H * 0.6f)
            lineTo(doNx, y)
            lineTo(innerLeft + r, y)
            quadraticBezierTo(innerLeft, y, innerLeft, y + r)

            // Ліва стінка ELSE
            y += elseInnerH
            lineTo(innerLeft, y - r)
            quadraticBezierTo(innerLeft, y, innerLeft + r, y)

            // Нижній tab ELSE
            lineTo(doNx + D.NOTCH_W, y)
            lineTo(doNx + D.NOTCH_W - 3f, y + D.NOTCH_H * 0.65f)
            lineTo(doNx + 3f, y + D.NOTCH_H * 0.65f)
            lineTo(doNx, y)
        }

        // ── Нижня планка C-блоку ─────────────────────────────
        lineTo(right - r, y)
        quadraticBezierTo(right, y, right, y + r)
        y += botH
        lineTo(right, y - r)
        quadraticBezierTo(right, y, right - r, y)
        if (hasNext) addBottomTab(left, y)
        lineTo(left + r, y)
        quadraticBezierTo(left, y, left, y - r)
        lineTo(left, top + r)
        quadraticBezierTo(left, top, left + r, top)
        close()
    }
}

// ───────────────────────────────────────────────────────────────
// DRAW WS BLOCK — рендер блоку на DrawScope
// ───────────────────────────────────────────────────────────────
fun DrawScope.drawWsBlock(
    block: WsBlock,
    allBlocks: Map<String, WsBlock>,
    textMeasurer: TextMeasurer,
    isSelected: Boolean,
    isExecuting: Boolean,
    isSnapHighlight: Boolean,
    scale: Float,
) {
    if (block.type.hasSub) {
        drawWsCBlock(
            block, allBlocks, textMeasurer,
            isSelected, isExecuting, isSnapHighlight,
        )
        return
    }

    val col  = blockColor(block.type)
    val colD = blockColorDark(block.type)
    val colL = blockColorLight(block.type)

    val bx   = block.x
    val by   = block.y
    val bw   = BlockDimensions.WIDTH
    val bh   = BlockDimensions.HEIGHT +
               if (!block.type.hasPrev) BlockDimensions.HAT_EXTRA else 0f

    // ── Форма блоку ─────────────────────────────────────────
    val bodyPath = if (!block.type.hasPrev)
        BlockPaths.hatBlock(bx, by, bw, bh, block.type.hasNext)
    else
        BlockPaths.statementBlock(bx, by, bw, bh, block.type.hasPrev, block.type.hasNext)

    // ── Тінь ────────────────────────────────────────────────
    val shadowPath = if (!block.type.hasPrev)
        BlockPaths.hatBlock(bx + 2f, by + 3f, bw, bh, block.type.hasNext)
    else
        BlockPaths.statementBlock(bx + 2f, by + 3f, bw, bh, block.type.hasPrev, block.type.hasNext)
    drawPath(shadowPath, Color(0x44000000))

    // ── Тіло — вертикальний градієнт (colLight → col) ───────
    drawPath(path = bodyPath, brush = Brush.verticalGradient(
        colors = listOf(colL, col),
        startY = by, endY = by + bh,
    ))

    // ── Ліва темна смужка (stripe) ──────────────────────────
    val stripePath = if (!block.type.hasPrev)
        BlockPaths.hatBlock(bx, by, BlockDimensions.STRIPE_W, bh, false)
    else
        BlockPaths.statementBlock(bx, by, BlockDimensions.STRIPE_W, bh, block.type.hasPrev, false)
    drawPath(stripePath, colD)

    // ── Обведення ────────────────────────────────────────────
    val strokeColor = when {
        isExecuting     -> Color.White
        isSelected      -> Color(0xFFFFFF88)
        isSnapHighlight -> Color(0xFF00FFEE)
        else            -> colD.copy(alpha = 0.7f)
    }
    val strokeW = when {
        isExecuting || isSelected || isSnapHighlight -> 2.5f
        else                                         -> 1f
    }
    drawPath(bodyPath, strokeColor, style = Stroke(strokeW))

    // ── Текст ───────────────────────────────────────────────
    drawWsBlockLabel(block, textMeasurer, bx, by, bw, bh)
}

// ───────────────────────────────────────────────────────────────
// C-BLOCK РЕНДЕР
// ───────────────────────────────────────────────────────────────
private fun DrawScope.drawWsCBlock(
    block: WsBlock,
    allBlocks: Map<String, WsBlock>,
    textMeasurer: TextMeasurer,
    isSelected: Boolean,
    isExecuting: Boolean,
    isSnapHighlight: Boolean,
) {
    val D    = BlockDimensions
    val col  = blockColor(block.type)
    val colD = blockColorDark(block.type)
    val colL = blockColorLight(block.type)

    val bx      = block.x
    val by      = block.y
    val bw      = D.WIDTH
    val headerH = D.HEIGHT + if (!block.type.hasPrev) D.HAT_EXTRA else 0f
    val sub1H   = max(chainHeight(block.subChainId, allBlocks), D.MIN_C_INNER.toFloat())
    val sub2H   = if (block.type.hasSub2)
        max(chainHeight(block.sub2ChainId, allBlocks), D.MIN_C_INNER.toFloat()) else 0f

    val path = BlockPaths.cBlock(
        left = bx, top = by, width = bw,
        headerH = headerH, innerH = sub1H,
        hasPrev = block.type.hasPrev, hasNext = block.type.hasNext,
        hasElse = block.type.hasSub2, elseInnerH = sub2H,
    )

    // Тінь
    val shadowPath = BlockPaths.cBlock(
        left = bx + 2f, top = by + 3f, width = bw,
        headerH = headerH, innerH = sub1H,
        hasPrev = block.type.hasPrev, hasNext = block.type.hasNext,
        hasElse = block.type.hasSub2, elseInnerH = sub2H,
    )
    drawPath(shadowPath, Color(0x33000000))

    // Тіло
    drawPath(path = path, brush = Brush.verticalGradient(
        colors = listOf(colL, col),
        startY = by, endY = by + headerH,
    ))

    // Внутрішній темний фон DO секції
    val innerLeft = bx + D.C_ARM_WIDTH
    drawRect(
        color   = Color(0x22000000),
        topLeft = Offset(innerLeft, by + headerH),
        size    = Size(bw - D.C_ARM_WIDTH, sub1H),
    )

    // ELSE секція якщо є
    if (block.type.hasSub2) {
        val y2 = by + headerH + sub1H + D.C_BOTTOM_H
        drawRect(
            color   = Color(0x22000000),
            topLeft = Offset(innerLeft, y2),
            size    = Size(bw - D.C_ARM_WIDTH, sub2H),
        )
    }

    // Ліва смужка
    val strip = BlockPaths.statementBlock(
        bx, by, D.STRIPE_W, headerH, block.type.hasPrev, false
    )
    drawPath(strip, colD)

    // Обведення
    val strokeColor = when {
        isExecuting     -> Color.White
        isSelected      -> Color(0xFFFFFF88)
        isSnapHighlight -> Color(0xFF00FFEE)
        else            -> colD.copy(0.7f)
    }
    drawPath(path, strokeColor,
        style = Stroke(if (isSelected || isExecuting) 2.5f else 1f))

    // Текст заголовку
    drawWsBlockLabel(block, textMeasurer, bx, by, bw, headerH)

    // Підказки DO / ELSE
    if (block.subChainId == null) {
        drawSectionLabel(textMeasurer, innerLeft + D.NOTCH_X + 6f,
            by + headerH + 8f, "do", colD)
    }
    if (block.type.hasSub2 && block.sub2ChainId == null) {
        val y2 = by + headerH + sub1H + D.C_BOTTOM_H
        drawSectionLabel(textMeasurer, innerLeft + D.NOTCH_X + 6f,
            y2 + 8f, "else", colD)
    }
}

// ───────────────────────────────────────────────────────────────
// LABEL — текст на блоці
// Blockly: Roboto/Segoe UI Bold 12sp, white
// ───────────────────────────────────────────────────────────────
private fun DrawScope.drawWsBlockLabel(
    block: WsBlock,
    textMeasurer: TextMeasurer,
    bx: Float, by: Float, bw: Float, bh: Float,
) {
    val label     = blockLabel(block.type)
    val hatOffset = if (!block.type.hasPrev) BlockDimensions.HAT_EXTRA * 0.3f else 0f
    val bodyH     = if (!block.type.hasPrev) bh - BlockDimensions.HAT_EXTRA else bh
    val cy        = by + hatOffset + bodyH / 2f

    // Основний текст — 12sp Bold (як у Blockly)
    val measured = textMeasurer.measure(
        AnnotatedString(label),
        TextStyle(
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
        )
    )
    drawText(
        measured,
        topLeft = Offset(
            bx + BlockDimensions.STRIPE_W + 10f,
            cy - measured.size.height / 2f,
        )
    )

    // Параметр справа (якщо є і влазить)
    val paramStr = shortParamText(block)
    if (paramStr.isNotEmpty()) {
        val pm = textMeasurer.measure(
            AnnotatedString(paramStr),
            TextStyle(fontSize = 10.sp, color = Color.White.copy(0.75f))
        )
        val px = bx + bw - pm.size.width - 12f
        val labelEnd = bx + BlockDimensions.STRIPE_W + 10f + measured.size.width + 6f
        if (px > labelEnd) {
            drawText(pm, topLeft = Offset(px, cy - pm.size.height / 2f))
        }
    }
}

private fun shortParamText(block: WsBlock): String =
    block.params
        .filterNot { it is BlockParam.SubProgram }
        .take(2)
        .joinToString(" ") { p ->
            when (p) {
                is BlockParam.NumberInput   -> {
                    val v = p.value
                    if (v == v.toLong().toFloat()) v.toInt().toString()
                    else "%.1f".format(v)
                }
                is BlockParam.DropdownInput ->
                    p.options.find { it.second == p.selected }?.first?.take(5) ?: ""
                is BlockParam.TextInput     -> "\"${p.value.take(7)}\""
                else -> ""
            }
        }.trim()

private fun DrawScope.drawSectionLabel(
    textMeasurer: TextMeasurer,
    x: Float, y: Float,
    label: String, color: Color,
) {
    val m = textMeasurer.measure(
        AnnotatedString(label),
        TextStyle(
            fontSize   = 10.sp,
            color      = color.copy(0.55f),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    )
    drawText(m, topLeft = Offset(x, y))
}

// ───────────────────────────────────────────────────────────────
// SNAP INDICATOR — синя мерехтлива крапка
// ───────────────────────────────────────────────────────────────
fun DrawScope.drawSnapIndicator(x: Float, y: Float, color: Color) {
    drawCircle(color.copy(0.75f), 14f, Offset(x, y))
    drawCircle(Color.White.copy(0.6f), 6f, Offset(x, y))
    drawCircle(Color.Transparent, 14f, Offset(x, y),
        style = Stroke(2f))
}

// ───────────────────────────────────────────────────────────────
// DRAG GHOST — напівпрозорий блок що тягнеться
// ───────────────────────────────────────────────────────────────
fun DrawScope.drawDragGhost(
    type: BlockType,
    screenX: Float, screenY: Float,
    textMeasurer: TextMeasurer,
) {
    val col  = blockColor(type).copy(alpha = 0.82f)
    val colD = blockColorDark(type).copy(alpha = 0.82f)
    val colL = blockColorLight(type).copy(alpha = 0.82f)
    val bw   = BlockDimensions.WIDTH
    val bh   = BlockDimensions.HEIGHT + if (!type.hasPrev) BlockDimensions.HAT_EXTRA else 0f
    val bx   = screenX - bw / 2f
    val by   = screenY - bh / 2f

    val shadow = if (!type.hasPrev)
        BlockPaths.hatBlock(bx + 3f, by + 4f, bw, bh, type.hasNext)
    else
        BlockPaths.statementBlock(bx + 3f, by + 4f, bw, bh, type.hasPrev, type.hasNext)
    drawPath(shadow, Color(0x55000000))

    val body = if (!type.hasPrev)
        BlockPaths.hatBlock(bx, by, bw, bh, type.hasNext)
    else
        BlockPaths.statementBlock(bx, by, bw, bh, type.hasPrev, type.hasNext)

    drawPath(path = body, brush = Brush.verticalGradient(
        colors = listOf(colL, col), startY = by, endY = by + bh,
    ))

    val stripe = if (!type.hasPrev)
        BlockPaths.hatBlock(bx, by, BlockDimensions.STRIPE_W, bh, false)
    else
        BlockPaths.statementBlock(bx, by, BlockDimensions.STRIPE_W, bh, type.hasPrev, false)
    drawPath(stripe, colD)
    drawPath(body, colD.copy(0.5f), style = Stroke(1.5f))

    val label = blockLabel(type)
    val hatOff = if (!type.hasPrev) BlockDimensions.HAT_EXTRA * 0.3f else 0f
    val bodyH  = if (!type.hasPrev) bh - BlockDimensions.HAT_EXTRA else bh
    val lm = textMeasurer.measure(
        AnnotatedString(label),
        TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White),
    )
    drawText(lm, topLeft = Offset(
        bx + BlockDimensions.STRIPE_W + 10f,
        by + hatOff + bodyH / 2f - lm.size.height / 2f,
    ))
}

// ───────────────────────────────────────────────────────────────
// WORKSPACE GRID — точкова сітка
// Оригінал: grid: { spacing: 50, length: 3, colour: '#475569' }
// ───────────────────────────────────────────────────────────────
fun DrawScope.drawWorkspaceGrid(
    panX: Float, panY: Float, scale: Float,
    canvasWidth: Float, canvasHeight: Float,
) {
    // spacing: 50, colour: #475569
    val spacing = 50f * scale
    val dotR    = 1.5f
    val dotColor = Color(0xFF475569).copy(alpha = 0.4f)

    val ox = ((panX % spacing) + spacing) % spacing
    val oy = ((panY % spacing) + spacing) % spacing

    var cx = ox
    while (cx < canvasWidth + spacing) {
        var cy = oy
        while (cy < canvasHeight + spacing) {
            drawCircle(dotColor, dotR, Offset(cx, cy))
            cy += spacing
        }
        cx += spacing
    }
}

// ───────────────────────────────────────────────────────────────
// EXECUTION GLOW — жовтий контур виконуваного блоку
// ───────────────────────────────────────────────────────────────
fun DrawScope.drawExecutionGlow(
    block: WsBlock,
    allBlocks: Map<String, WsBlock>,
    animValue: Float,
) {
    val bw  = BlockDimensions.WIDTH
    val bh  = BlockDimensions.HEIGHT + if (!block.type.hasPrev) BlockDimensions.HAT_EXTRA else 0f
    val off = 5f
    val glowPath = if (!block.type.hasPrev)
        BlockPaths.hatBlock(block.x - off, block.y - off, bw + off * 2, bh + off * 2, block.type.hasNext)
    else
        BlockPaths.statementBlock(block.x - off, block.y - off, bw + off * 2, bh + off * 2,
            block.type.hasPrev, block.type.hasNext)
    drawPath(
        glowPath,
        Color(1f, 1f, 0f, 0.25f + animValue * 0.75f),
        style = Stroke(3.5f),
    )
}
