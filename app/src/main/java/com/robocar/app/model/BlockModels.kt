package com.robocar.app.model

import java.util.UUID

// ===== Категорії =====
enum class BlockCategory(val label: String, val color: Long) {
    // 6 категорій — як в оригінальному HTML
    CAR       ("Машинка",    0xFF4C97FFL),   // colour="#4C97FF"
    LOGIC     ("Логіка",     0xFF5CB1D6L),   // colour="%{BKY_LOGIC_HUE}"
    LOOPS     ("Цикли",      0xFF5BA55BL),   // colour="%{BKY_LOOPS_HUE}"
    MATH      ("Математика", 0xFF9966FFL),   // colour="%{BKY_MATH_HUE}"
    CONTROL   ("Час",        0xFFFFBF00L),   // colour="#FFBF00"
    VARIABLES ("Змінні",     0xFFFF8C1AL),   // colour="%{BKY_VARIABLES_HUE}"
}

// ===== Тип параметру блоку =====
sealed class BlockParam {
    data class NumberInput(val label: String, val value: Float, val min: Float = -100f, val max: Float = 100f) : BlockParam()
    data class DropdownInput(val label: String, val options: List<Pair<String,String>>, val selected: String) : BlockParam()
    data class TextInput(val label: String, val value: String) : BlockParam()
    data class SubProgram(val label: String, val blocks: List<ProgramBlock> = emptyList()) : BlockParam()
}

// ===== Один блок у програмі =====
data class ProgramBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType,
    val params: List<BlockParam> = emptyList(),
    val subBlocks: List<ProgramBlock> = emptyList(), // для циклів/умов
    val subBlocks2: List<ProgramBlock> = emptyList(), // для else
)

// ===== Всі типи блоків =====
enum class BlockType(
    val label: String,
    val emoji: String,
    val category: BlockCategory,
    val color: Long,
    val hasNext: Boolean = true,
    val hasPrev: Boolean = true,
    val hasSub: Boolean = false,
    val hasSub2: Boolean = false,
) {
    // === 🚗 МАШИНКА ===
    START_HAT       ("СТАРТ",              "", BlockCategory.CAR,     0xFF2E7D32L, hasPrev = false),
    ROBOT_MOVE      ("Їхати L/R",         "", BlockCategory.CAR,     0xFF0062BAL),
    ROBOT_MOVE_SOFT ("Плавний старт",      "", BlockCategory.CAR,     0xFF0062BAL),
    ROBOT_TURN      ("Поворот",             "", BlockCategory.CAR,     0xFF0062BAL),
    ROBOT_SET_SPEED ("Швидкість",           "", BlockCategory.CAR,     0xFF0062BAL),
    ROBOT_STOP      ("Стоп",                "", BlockCategory.CAR,     0xFFB71C1CL),
    MOTOR_SINGLE    ("Мотор A/B/C/D",       "", BlockCategory.CAR,     0xFF4527A0L),
    GO_HOME         ("Додому (Назад)",      "", BlockCategory.CAR,     0xFF0062BAL),
    RECORD_START    ("Запис траси",          "", BlockCategory.CAR,     0xFF6A1B9AL),
    REPLAY_TRACK    ("Відтворити трасу",    "", BlockCategory.CAR,     0xFF6A1B9AL),
    REPLAY_LOOP     ("Відтворити N разів",  "", BlockCategory.CAR,     0xFF6A1B9AL),
    WAIT_START      ("Чекати Старт",         "", BlockCategory.CAR,     0xFF37474FL),
    STOP_AT_START   ("Зупинитись на старті", "", BlockCategory.CAR,     0xFF37474FL),
    COUNT_LAPS      ("Лічити кола",           "", BlockCategory.CAR,     0xFF37474FL),
    AUTOPILOT       ("Автопілот",             "", BlockCategory.CAR,     0xFFE65100L),

    // === 🔁 КЕРУВАННЯ ===
    WAIT_SECONDS    ("Чекати (сек)",          "", BlockCategory.CONTROL, 0xFF37474FL),
    LOOP_FOREVER    ("Цикл назавжди",         "", BlockCategory.CONTROL, 0xFF2E7D32L, hasSub = true),
    LOOP_REPEAT     ("Повторити N разів",     "", BlockCategory.CONTROL, 0xFF2E7D32L, hasSub = true),
    LOOP_REPEAT_PAUSE("Повторити з паузою",    "", BlockCategory.CONTROL, 0xFF2E7D32L, hasSub = true),
    LOOP_EVERY_SEC  ("Кожні N секунд",        "", BlockCategory.CONTROL, 0xFF2E7D32L, hasSub = true),
    TIMER_RESET     ("Скинути таймер",        "", BlockCategory.CONTROL, 0xFF37474FL),

    // === 📡 СЕНСОРИ ===
    WAIT_UNTIL_SENSOR("Чекати поки сенсор",    "", BlockCategory.LOGIC, 0xFF00695CL),

    // === 📐 МАТЕМАТИКА ===
    TIMER_GET       ("Таймер (с)",             "", BlockCategory.MATH,    0xFF283593L),
    MATH_PID        ("PID Регулятор",          "", BlockCategory.MATH,    0xFF283593L),
    MATH_SMOOTH     ("Згладити",               "", BlockCategory.MATH,    0xFF283593L),
    MATH_PYTHAGORAS ("Піфагор (діагональ)",   "", BlockCategory.MATH,    0xFF283593L),
    MATH_PATH_VT    ("Довжина шляху v×t",     "", BlockCategory.MATH,    0xFF283593L),
    MATH_SPEED_CMS  ("Швидкість (см/с)",      "", BlockCategory.MATH,    0xFF283593L),
    CALIBRATE_SPEED ("Калібрувати",             "", BlockCategory.MATH,    0xFF283593L),

    // === 🧠 СТАН (State Machine) ===
    STATE_SET       ("Стан =",                  "", BlockCategory.LOGIC,   0xFF6A1B9AL),
    STATE_SET_REASON("Стан = (з причиною)",    "", BlockCategory.LOGIC,   0xFF6A1B9AL),
    STATE_PREV      ("Повернутись у попер.",   "", BlockCategory.LOGIC,   0xFF6A1B9AL),
    STATE_IF        ("Якщо стан =",             "", BlockCategory.LOGIC,   0xFF6A1B9AL, hasSub = true, hasSub2 = true),

    // === ⚡ РОЗУМНІ УМОВИ ===
    WAIT_UNTIL_TRUE_FOR("Чекати поки умова",      "", BlockCategory.LOOPS, 0xFFBF360CL),
    TIMEOUT_DO_UNTIL("Робити до умови",          "",  BlockCategory.LOOPS, 0xFFBF360CL, hasSub = true),
    COOLDOWN_DO     ("Не частіше ніж N с",      "",  BlockCategory.LOOPS, 0xFFBF360CL, hasSub = true),
    LATCH_SET       ("Прапор встановити",       "",  BlockCategory.LOOPS, 0xFFBF360CL),
    MOTOR_4         ("4 Мотори (ABCD)",          "", BlockCategory.CAR,     0xFF4527A0L),
    STATE_GET       ("Поточний стан",             "", BlockCategory.LOGIC,   0xFF6A1B9AL),
    STATE_TIME_S    ("Час у стані (с)",           "", BlockCategory.LOGIC,   0xFF6A1B9AL),
    STATE_ENTER_COUNT("Скільки разів у стан",     "", BlockCategory.LOGIC,   0xFF6A1B9AL),
    LATCH_GET       ("Прапор встановлено?",       "", BlockCategory.LOOPS,   0xFFBF360CL),
    IF_TRUE_FOR     ("Якщо умова тримається",     "", BlockCategory.LOOPS,   0xFFBF360CL, hasSub = true, hasSub2 = true),
    EDGE_DETECT     ("Сигнал став активним (0→1)","", BlockCategory.LOOPS,   0xFFBF360CL),
    SCHMITT_TRIGGER ("Тригер Шмітта",             "", BlockCategory.LOOPS,   0xFFBF360CL),
        LATCH_RESET     ("Прапор скинути",          "",  BlockCategory.LOOPS, 0xFFBF360CL),
    // === Додаткові типи що були в WsExecutor ===
    // === 📝 ЛОГО / ЛОГ ===
    CONSOLE_LOG   ("Лог повідомлення",     "", BlockCategory.CAR,    0xFF607D8BL),

    // === 🔄 СТАНДАРТНІ ЦИКЛИ (як в Blockly) ===
    LOOP_WHILE    ("Поки / До",            "", BlockCategory.LOOPS,  0xFF5BA55BL, hasSub = true),
    LOOP_FOR      ("Для від до",           "", BlockCategory.LOOPS,  0xFF5BA55BL, hasSub = true),
    LOOP_FOR_EACH ("Для кожного",          "", BlockCategory.LOOPS,  0xFF5BA55BL, hasSub = true),

    // === 🧠 ЛОГІКА (як в Blockly) ===
    LOGIC_IF      ("Якщо",                 "", BlockCategory.LOGIC,  0xFF5CB1D6L, hasSub = true, hasSub2 = true),
    LOGIC_COMPARE ("Порівняти",            "", BlockCategory.LOGIC,  0xFF5CB1D6L, hasNext = false, hasPrev = false),
    LOGIC_AND_OR  ("І / АБО",             "", BlockCategory.LOGIC,  0xFF5CB1D6L, hasNext = false, hasPrev = false),
    LOGIC_NOT     ("НЕ",                  "", BlockCategory.LOGIC,  0xFF5CB1D6L, hasNext = false, hasPrev = false),
    LOGIC_BOOL    ("True / False",         "", BlockCategory.LOGIC,  0xFF5CB1D6L, hasNext = false, hasPrev = false),
    SENSOR_GET    ("Значення сенсора",     "", BlockCategory.LOGIC, 0xFF00897BL, hasNext = false, hasPrev = false),

    // === 📐 MATH (як в Blockly) ===
    MATH_NUMBER   ("Число",               "", BlockCategory.MATH,   0xFF9966FFL, hasNext = false, hasPrev = false),
    MATH_ARITH    ("Арифметика ± × ÷",   "", BlockCategory.MATH,   0xFF9966FFL, hasNext = false, hasPrev = false),
    MATH_RANDOM   ("Випадкове число",     "", BlockCategory.MATH,   0xFF9966FFL, hasNext = false, hasPrev = false),
    MATH_ROUND    ("Заокруглити",         "", BlockCategory.MATH,   0xFF9966FFL, hasNext = false, hasPrev = false),
    MATH_MODULO   ("Залишок %",           "", BlockCategory.MATH,   0xFF9966FFL, hasNext = false, hasPrev = false),

    // === 📦 ЗМІННІ (як в Blockly VARIABLE category) ===
    VAR_SET       ("Встановити змінну",   "", BlockCategory.VARIABLES, 0xFFFF8C1AL),
    VAR_GET       ("Значення змінної",    "", BlockCategory.VARIABLES, 0xFFFF8C1AL, hasNext = false, hasPrev = false),
    VAR_CHANGE    ("Змінити на",          "", BlockCategory.VARIABLES, 0xFFFF8C1AL),
}

// ===== Фабрика блоків з дефолтними параметрами =====
fun createBlock(type: BlockType): ProgramBlock {
    val params = mutableListOf<BlockParam>()
    when (type) {
        BlockType.ROBOT_MOVE -> {
            params += BlockParam.NumberInput("L", 100f, -100f, 100f)
            params += BlockParam.NumberInput("R", 100f, -100f, 100f)
        }
        BlockType.ROBOT_MOVE_SOFT -> {
            params += BlockParam.NumberInput("Ціль", 100f, -100f, 100f)
            params += BlockParam.NumberInput("Сек", 1f, 0f, 10f)
        }
        BlockType.ROBOT_TURN -> {
            params += BlockParam.DropdownInput("Напрям", listOf("Ліворуч ⬅️" to "LEFT", "Праворуч ➡️" to "RIGHT"), "LEFT")
            params += BlockParam.NumberInput("Сек", 0.5f, 0f, 10f)
        }
        BlockType.ROBOT_SET_SPEED -> {
            params += BlockParam.NumberInput("Швидкість %", 50f, 0f, 100f)
        }
        BlockType.MOTOR_SINGLE -> {
            params += BlockParam.DropdownInput("Мотор", listOf("A" to "1","B" to "2","C" to "3","D" to "4"), "1")
            params += BlockParam.NumberInput("Шв", 100f, -100f, 100f)
        }
        BlockType.REPLAY_LOOP -> {
            params += BlockParam.NumberInput("Разів", 1f, 1f, 99f)
        }
        BlockType.COUNT_LAPS -> {
            params += BlockParam.NumberInput("Кіл", 3f, 1f, 99f)
        }
        BlockType.AUTOPILOT -> {
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
            params += BlockParam.DropdownInput("Поворот", listOf("RIGHT" to "RIGHT","LEFT" to "LEFT"), "RIGHT")
            params += BlockParam.NumberInput("Поріг <", 40f, 0f, 255f)
            params += BlockParam.NumberInput("Швидк.", 60f, 0f, 100f)
        }
        BlockType.WAIT_SECONDS -> {
            params += BlockParam.NumberInput("Сек", 1f, 0f, 60f)
        }
        BlockType.LOOP_REPEAT -> {
            params += BlockParam.NumberInput("Разів", 3f, 1f, 99f)
        }
        BlockType.LOOP_REPEAT_PAUSE -> {
            params += BlockParam.NumberInput("Разів", 3f, 1f, 99f)
            params += BlockParam.NumberInput("Пауза (с)", 1f, 0f, 10f)
        }
        BlockType.LOOP_EVERY_SEC -> {
            params += BlockParam.NumberInput("Кожні (с)", 1f, 0.1f, 60f)
        }
        BlockType.WAIT_UNTIL_SENSOR -> {
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
            params += BlockParam.DropdownInput("Умова", listOf("< менше" to "LT","> більше" to "GT"), "LT")
            params += BlockParam.NumberInput("Значення", 25f, 0f, 255f)
        }
        BlockType.MATH_PID -> {
            params += BlockParam.NumberInput("Kp", 1f, 0f, 100f)
            params += BlockParam.NumberInput("Ki", 0f, 0f, 100f)
            params += BlockParam.NumberInput("Kd", 0f, 0f, 100f)
        }
        BlockType.MATH_SMOOTH -> {
            params += BlockParam.NumberInput("К-сть", 5f, 2f, 50f)
        }
        BlockType.CALIBRATE_SPEED -> {
            params += BlockParam.NumberInput("Відстань (см)", 50f, 1f, 500f)
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
            params += BlockParam.NumberInput("Поріг", 30f, 0f, 255f)
            params += BlockParam.NumberInput("Швидк.", 60f, 0f, 100f)
        }
        BlockType.STATE_SET -> {
            params += BlockParam.TextInput("Стан", "SEARCH")
        }
        BlockType.STATE_SET_REASON -> {
            params += BlockParam.TextInput("Стан", "ATTACK")
            params += BlockParam.TextInput("Причина", "sensor")
        }
        BlockType.STATE_IF -> {
            params += BlockParam.TextInput("Стан", "SEARCH")
        }
        BlockType.MOTOR_4 -> {
            params += BlockParam.NumberInput("A", 100f, -100f, 100f)
            params += BlockParam.NumberInput("B", 100f, -100f, 100f)
            params += BlockParam.NumberInput("C", 0f, -100f, 100f)
            params += BlockParam.NumberInput("D", 0f, -100f, 100f)
        }
        BlockType.LATCH_GET -> {
            params += BlockParam.TextInput("Назва", "A")
        }
        BlockType.IF_TRUE_FOR -> {
            params += BlockParam.NumberInput("Сек", 0.2f, 0f, 10f)
        }
        BlockType.SCHMITT_TRIGGER -> {
            params += BlockParam.NumberInput("Вкл >", 60f, 0f, 1023f)
            params += BlockParam.NumberInput("Викл <", 40f, 0f, 1023f)
        }
        BlockType.STATE_ENTER_COUNT -> {
            params += BlockParam.TextInput("Стан", "SEARCH")
        }
                BlockType.WAIT_UNTIL_TRUE_FOR -> {
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
            params += BlockParam.DropdownInput("Умова", listOf("< менше" to "LT","> більше" to "GT"), "LT")
            params += BlockParam.NumberInput("Значення", 25f, 0f, 255f)
            params += BlockParam.NumberInput("Час (с)", 0.2f, 0f, 10f)
        }
        BlockType.TIMEOUT_DO_UNTIL -> {
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
            params += BlockParam.DropdownInput("Умова", listOf("< менше" to "LT","> більше" to "GT"), "LT")
            params += BlockParam.NumberInput("Значення", 25f, 0f, 255f)
            params += BlockParam.NumberInput("Макс (с)", 3f, 0f, 30f)
        }
        BlockType.COOLDOWN_DO -> {
            params += BlockParam.NumberInput("Пауза (с)", 1f, 0f, 30f)
        }
        BlockType.LATCH_SET, BlockType.LATCH_RESET -> {
            params += BlockParam.TextInput("Прапор", "flag1")
        }

        BlockType.MOTOR_4 -> {
            params += BlockParam.NumberInput("A", 0f, -100f, 100f)
            params += BlockParam.NumberInput("B", 0f, -100f, 100f)
            params += BlockParam.NumberInput("C", 0f, -100f, 100f)
            params += BlockParam.NumberInput("D", 0f, -100f, 100f)
        }
        BlockType.LOOP_REPEAT_PAUSE -> {
            params += BlockParam.NumberInput("Разів", 4f, 1f, 100f)
            params += BlockParam.NumberInput("Пауза (сек)", 1f, 0f, 60f)
        }
        BlockType.WAIT_UNTIL_SENSOR -> {
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
            params += BlockParam.DropdownInput("Умова", listOf("< менше" to "LT","> більше" to "GT"), "LT")
            params += BlockParam.NumberInput("Значення", 30f, 0f, 1023f)
        }
        BlockType.WAIT_UNTIL_TRUE_FOR -> {
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
            params += BlockParam.DropdownInput("Умова", listOf("< менше" to "LT","> більше" to "GT"), "LT")
            params += BlockParam.NumberInput("Значення", 30f, 0f, 1023f)
            params += BlockParam.NumberInput("Тривалість (сек)", 1f, 0f, 30f)
        }
        BlockType.STATE_SET_REASON -> {
            params += BlockParam.TextInput("Новий стан", "IDLE")
            params += BlockParam.TextInput("Причина", "timeout")
        }
        BlockType.TIMEOUT_DO_UNTIL -> {
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
            params += BlockParam.DropdownInput("Умова", listOf("< менше" to "LT","> більше" to "GT"), "LT")
            params += BlockParam.NumberInput("Значення", 30f, 0f, 1023f)
            params += BlockParam.NumberInput("Таймаут (сек)", 5f, 0f, 60f)
        }
        BlockType.CONSOLE_LOG -> {
            params += BlockParam.TextInput("Повідомлення", "Привіт!")
        }
        BlockType.LOOP_WHILE -> {
            params += BlockParam.DropdownInput("Режим", listOf("Поки" to "WHILE", "До" to "UNTIL"), "WHILE")
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
            params += BlockParam.DropdownInput("Умова", listOf("< менше" to "LT","> більше" to "GT","= рівно" to "EQ"), "LT")
            params += BlockParam.NumberInput("Значення", 30f, 0f, 1023f)
        }
        BlockType.LOOP_FOR -> {
            params += BlockParam.TextInput("Змінна", "i")
            params += BlockParam.NumberInput("Від", 1f, -999f, 999f)
            params += BlockParam.NumberInput("До", 10f, -999f, 999f)
            params += BlockParam.NumberInput("Крок", 1f, -999f, 999f)
        }
        BlockType.LOOP_FOR_EACH -> {
            params += BlockParam.TextInput("Змінна", "item")
        }
        BlockType.LOGIC_IF -> {
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
            params += BlockParam.DropdownInput("Умова", listOf("< менше" to "LT","> більше" to "GT","= рівно" to "EQ"), "LT")
            params += BlockParam.NumberInput("Значення", 30f, 0f, 1023f)
        }
        BlockType.LOGIC_COMPARE -> {
            params += BlockParam.DropdownInput("Оператор", listOf(
                "=" to "EQ", "≠" to "NEQ", "<" to "LT", "≤" to "LTE", ">" to "GT", "≥" to "GTE"
            ), "EQ")
            params += BlockParam.NumberInput("Значення A", 0f, -999f, 999f)
            params += BlockParam.NumberInput("Значення B", 0f, -999f, 999f)
        }
        BlockType.LOGIC_AND_OR -> {
            params += BlockParam.DropdownInput("Оператор", listOf("І (AND)" to "AND", "АБО (OR)" to "OR"), "AND")
        }
        BlockType.LOGIC_NOT -> { /* no params */ }
        BlockType.LOGIC_BOOL -> {
            params += BlockParam.DropdownInput("Значення", listOf("True" to "TRUE", "False" to "FALSE"), "TRUE")
        }
        BlockType.SENSOR_GET -> {
            params += BlockParam.DropdownInput("Тип", listOf(
                "Відстань" to "DIST", "Світло" to "LIGHT", "Дотик" to "TOUCH"
            ), "DIST")
            params += BlockParam.DropdownInput("Порт", listOf("1" to "0","2" to "1","3" to "2","4" to "3"), "0")
        }
        BlockType.MATH_NUMBER -> {
            params += BlockParam.NumberInput("Число", 0f, -9999f, 9999f)
        }
        BlockType.MATH_ARITH -> {
            params += BlockParam.DropdownInput("Оператор", listOf(
                "+" to "ADD", "-" to "MINUS", "×" to "MUL", "÷" to "DIV", "^" to "POW"
            ), "ADD")
            params += BlockParam.NumberInput("A", 0f, -9999f, 9999f)
            params += BlockParam.NumberInput("B", 0f, -9999f, 9999f)
        }
        BlockType.MATH_RANDOM -> {
            params += BlockParam.NumberInput("Від", 1f, -9999f, 9999f)
            params += BlockParam.NumberInput("До", 100f, -9999f, 9999f)
        }
        BlockType.MATH_ROUND -> {
            params += BlockParam.DropdownInput("Тип", listOf(
                "Заокруглити" to "ROUND", "Вниз" to "FLOOR", "Вгору" to "CEIL", "Абс. знач." to "ABS"
            ), "ROUND")
            params += BlockParam.NumberInput("Число", 0f, -9999f, 9999f)
        }
        BlockType.MATH_MODULO -> {
            params += BlockParam.NumberInput("Ділене", 10f, -9999f, 9999f)
            params += BlockParam.NumberInput("Дільник", 3f, 1f, 9999f)
        }
        BlockType.VAR_SET -> {
            params += BlockParam.TextInput("Назва змінної", "Швидкість")
            params += BlockParam.NumberInput("Значення", 0f, -9999f, 9999f)
        }
        BlockType.VAR_GET -> {
            params += BlockParam.TextInput("Назва змінної", "Швидкість")
        }
        BlockType.VAR_CHANGE -> {
            params += BlockParam.TextInput("Назва змінної", "Швидкість")
            params += BlockParam.NumberInput("Змінити на", 1f, -9999f, 9999f)
        }
        else -> {}
    }
    return ProgramBlock(type = type, params = params)
}

// ─────────────────────────────────────────────────────────────
// НОВІ ТИПИ БЛОКІВ — є в оригінальному веб-Blockly
// ─────────────────────────────────────────────────────────────
// Додаткові типи (Variables, Logic, Math, Standard Loops)
// Вони відображені в enum BlockType нижче як розширення
