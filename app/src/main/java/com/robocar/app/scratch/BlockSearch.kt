package com.robocar.app.scratch

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.robocar.app.model.BlockCategory
import com.robocar.app.model.BlockType

// ─────────────────────────────────────────────────────────────
// BLOCK SEARCH — пошук блоків за назвою
// ─────────────────────────────────────────────────────────────
@Composable
fun BlockSearchPanel(
    onSelectBlock: (BlockType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<BlockCategory?>(null) }

    val allBlocks = BlockType.values()
    val filtered = remember(query, selectedCategory) {
        allBlocks.filter { type ->
            val matchesQuery = query.isBlank() ||
                type.label.contains(query, ignoreCase = true) ||
                type.category.label.contains(query, ignoreCase = true)
            val matchesCat = selectedCategory == null || type.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color    = Color(0xFF0F172A),
        shape    = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Хендл
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF334155))
            )

            Spacer(Modifier.height(12.dp))

            // Заголовок
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    "ПОШУК БЛОКІВ",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color(0xFF94A3B8),
                    letterSpacing = 1.5.sp,
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close, null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Поле пошуку
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Search, null,
                    tint     = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                BasicTextField(
                    value         = query,
                    onValueChange = { query = it },
                    modifier      = Modifier.weight(1f),
                    textStyle     = TextStyle(
                        color      = Color.White,
                        fontSize   = 14.sp,
                        fontFamily = FontFamily.Default,
                    ),
                    cursorBrush   = SolidColor(Color(0xFF3B82F6)),
                    singleLine    = true,
                    decorationBox = { inner ->
                        Box {
                            if (query.isBlank()) {
                                Text(
                                    "Введіть назву блоку...",
                                    fontSize = 14.sp,
                                    color    = Color(0xFF475569),
                                )
                            }
                            inner()
                        }
                    }
                )
                if (query.isNotBlank()) {
                    Icon(
                        Icons.Default.Clear, null,
                        tint     = Color(0xFF64748B),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { query = "" },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Фільтр категорій
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    CategoryFilterChip(
                        label      = "Всі",
                        isSelected = selectedCategory == null,
                        color      = Color(0xFF94A3B8),
                        onClick    = { selectedCategory = null }
                    )
                }
                items(BlockCategory.values()) { cat ->
                    CategoryFilterChip(
                        label      = cat.label,
                        isSelected = selectedCategory == cat,
                        color      = categoryColor(cat),
                        onClick    = { selectedCategory = if (selectedCategory == cat) null else cat }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Кількість результатів
            Text(
                "${filtered.size} блоків",
                fontSize = 10.sp,
                color    = Color(0xFF475569),
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(4.dp))

            // Список блоків
            LazyColumn(
                contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp),
                modifier              = Modifier.heightIn(max = 300.dp),
            ) {
                items(filtered) { type ->
                    SearchResultRow(
                        type    = type,
                        onClick = { onSelectBlock(type); onDismiss() },
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Нічого не знайдено",
                                fontSize = 13.sp,
                                color    = Color(0xFF334155),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CATEGORY FILTER CHIP
// ─────────────────────────────────────────────────────────────
@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSelected) color.copy(0.25f) else Color(0xFF1E293B))
            .border(
                width = 1.dp,
                color = if (isSelected) color else Color(0xFF334155),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            fontSize   = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color      = if (isSelected) color else Color(0xFF64748B),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// SEARCH RESULT ROW — один рядок результату пошуку
// ─────────────────────────────────────────────────────────────
@Composable
private fun SearchResultRow(
    type: BlockType,
    onClick: () -> Unit,
) {
    val color = categoryColor(type.category)
    val colorD = categoryColorDark(type.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Кольорова ліва смужка (як пазл)
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )

        // Іконка типу блоку
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                blockTypeIcon(type), null,
                tint     = color,
                modifier = Modifier.size(18.dp)
            )
        }

        // Назва + категорія
        Column(modifier = Modifier.weight(1f)) {
            Text(
                type.label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
            Text(
                type.category.label,
                fontSize = 10.sp,
                color    = color.copy(0.8f),
            )
        }

        // Тип з'єднання
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (!type.hasPrev) {
                ConnectionBadge("HAT", Color(0xFF2E7D32))
            }
            if (type.hasSub) {
                ConnectionBadge("C", Color(0xFF1565C0))
            }
            if (!type.hasNext) {
                ConnectionBadge("END", Color(0xFF7B1FA2))
            }
        }

        // Стрілка вправо
        Icon(
            Icons.Default.ChevronRight, null,
            tint     = Color(0xFF334155),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ConnectionBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(0.2f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            label,
            fontSize   = 8.sp,
            fontWeight = FontWeight.Bold,
            color      = color,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// BLOCK TYPE ICON — іконка для кожного типу блоку
// ─────────────────────────────────────────────────────────────
fun blockTypeIcon(type: BlockType): androidx.compose.ui.graphics.vector.ImageVector = when (type.category) {
    BlockCategory.CAR       -> Icons.Default.DirectionsCar
    BlockCategory.CONTROL   -> Icons.Default.Loop
    BlockCategory.SENSORS   -> Icons.Default.Sensors
    BlockCategory.MATH      -> Icons.Default.Calculate
    BlockCategory.STATE     -> Icons.Default.Psychology
    BlockCategory.SMART     -> Icons.Default.AutoFixHigh
    BlockCategory.LOGIC     -> Icons.Default.DeviceHub
    BlockCategory.VARIABLES -> Icons.Default.DataObject
    BlockCategory.LOOPS     -> Icons.Default.Repeat
}

// ─────────────────────────────────────────────────────────────
// RECENT BLOCKS — останні використані блоки
// ─────────────────────────────────────────────────────────────
@Composable
fun RecentBlocksRow(
    recentTypes: List<BlockType>,
    onSelect: (BlockType) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recentTypes.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            "НЕЩОДАВНІ",
            fontSize      = 9.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = Color(0xFF475569),
            letterSpacing = 1.sp,
            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        LazyRow(
            contentPadding        = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(recentTypes.take(5)) { type ->
                val color = categoryColor(type.category)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(0.15f))
                        .border(1.dp, color.copy(0.3f), RoundedCornerShape(8.dp))
                        .clickable { onSelect(type) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        type.label,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = color,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BLOCK QUICK INFO — коротка довідка про блок при наведенні
// ─────────────────────────────────────────────────────────────
@Composable
fun BlockQuickInfo(
    type: BlockType,
    modifier: Modifier = Modifier,
) {
    val color = categoryColor(type.category)

    Surface(
        shape  = RoundedCornerShape(10.dp),
        color  = Color(0xFF1E293B),
        shadowElevation = 6.dp,
        modifier = modifier.widthIn(max = 220.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Заголовок
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    type.label,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
            }

            // Опис
            Text(
                blockDescription(type),
                fontSize  = 11.sp,
                color     = Color(0xFF94A3B8),
                lineHeight = 15.sp,
            )

            // З'єднання
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!type.hasPrev)  ConnectionBadge("HAT", Color(0xFF2E7D32))
                if (type.hasSub)    ConnectionBadge("C-блок", Color(0xFF1565C0))
                if (!type.hasNext)  ConnectionBadge("Кінець", Color(0xFF7B1FA2))
                if (type.hasPrev && type.hasNext && !type.hasSub)
                    ConnectionBadge("Statement", Color(0xFF455A64))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BLOCK DESCRIPTIONS — текстовий опис кожного блоку
// ─────────────────────────────────────────────────────────────
fun blockDescription(type: BlockType): String = when (type) {
    BlockType.START_HAT          -> "Початок програми. Тут все починається."
    BlockType.ROBOT_MOVE         -> "Встановити швидкість лівого та правого моторів."
    BlockType.ROBOT_MOVE_SOFT    -> "Плавно розігнатись до заданої швидкості за N секунд."
    BlockType.ROBOT_TURN         -> "Повернути ліворуч або праворуч на час."
    BlockType.ROBOT_SET_SPEED    -> "Задати глобальний ліміт швидкості у відсотках."
    BlockType.ROBOT_STOP         -> "Зупинити всі мотори (відправити 0,0,0,0)."
    BlockType.MOTOR_SINGLE       -> "Керувати одним конкретним мотором (A/B/C/D)."
    BlockType.MOTOR_4            -> "Задати швидкість усім 4 моторам одночасно."
    BlockType.WAIT_SECONDS       -> "Зачекати вказану кількість секунд."
    BlockType.LOOP_FOREVER       -> "Виконувати вкладені блоки нескінченно."
    BlockType.LOOP_REPEAT        -> "Виконати вкладені блоки N разів."
    BlockType.LOOP_REPEAT_PAUSE  -> "Виконати N разів із паузою між ітераціями."
    BlockType.LOOP_EVERY_SEC     -> "Виконувати блоки з інтервалом N секунд."
    BlockType.TIMER_RESET        -> "Скинути внутрішній таймер програми."
    BlockType.WAIT_UNTIL_SENSOR  -> "Чекати доки сенсор не досягне умови."
    BlockType.AUTOPILOT          -> "Автоматичне рулювання на основі сенсора."
    BlockType.STATE_SET          -> "Перейти в новий стан State Machine."
    BlockType.STATE_IF           -> "Якщо поточний стан — виконати DO, інакше ELSE."
    BlockType.LATCH_SET          -> "Встановити прапор (булева змінна)."
    BlockType.LATCH_RESET        -> "Скинути прапор у false."
    BlockType.MATH_PID           -> "PID-регулятор для стабілізації руху."
    BlockType.MATH_SMOOTH        -> "Усереднення значень для плавності."
    BlockType.COOLDOWN_DO        -> "Виконати блоки не частіше ніж раз на N секунд."
    BlockType.TIMEOUT_DO_UNTIL   -> "Виконувати блоки до умови, але не довше N сек."
    BlockType.IF_TRUE_FOR        -> "Якщо умова тримається N сек — виконати DO."
    BlockType.RECORD_START       -> "Почати запис маршруту для подальшого відтворення."
    BlockType.REPLAY_TRACK       -> "Відтворити записаний маршрут."
    BlockType.REPLAY_LOOP        -> "Відтворити маршрут N разів по колу."
    else                         -> "Блок категорії ${type.category.label}."
}
