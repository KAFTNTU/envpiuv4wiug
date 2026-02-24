package com.robocar.app.scratch

// ═══════════════════════════════════════════════════════════════════════
// UNDO / REDO MANAGER — Command Pattern
// Зберігає знімки WorkspaceState для undo/redo (до 50 кроків)
// Використовується у WorkspaceViewModel:
//   undoManager.push(state)  — перед кожною мутацією
//   undoManager.undo(current) → WorkspaceState?
//   undoManager.redo(current) → WorkspaceState?
// ═══════════════════════════════════════════════════════════════════════

class UndoManager(private val maxHistory: Int = 50) {

    // Стек скасування (останній елемент = найновіший стан)
    private val undoStack = ArrayDeque<WorkspaceSnapshot>()

    // Стек повтору
    private val redoStack = ArrayDeque<WorkspaceSnapshot>()

    // ─── Публічний стан ───────────────────────────────────
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int   get() = undoStack.size
    val redoCount: Int   get() = redoStack.size

    // ─── Зберегти поточний стан перед мутацією ────────────
    fun push(state: WorkspaceState) {
        undoStack.addLast(WorkspaceSnapshot.from(state))
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        // Будь-яка нова дія очищає redo-стек
        redoStack.clear()
    }

    // ─── Скасувати (Undo) ─────────────────────────────────
    // Повертає стан до якого треба перейти, або null
    fun undo(current: WorkspaceState): WorkspaceState? {
        if (!canUndo) return null
        val prev = undoStack.removeLast()
        // Зберегти поточний у redo
        redoStack.addLast(WorkspaceSnapshot.from(current))
        if (redoStack.size > maxHistory) redoStack.removeFirst()
        return prev.toState()
    }

    // ─── Повторити (Redo) ─────────────────────────────────
    fun redo(current: WorkspaceState): WorkspaceState? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(WorkspaceSnapshot.from(current))
        return next.toState()
    }

    // ─── Очистити обидва стеки ────────────────────────────
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    // ─── Мітки (опис дії) для UI ─────────────────────────
    fun undoLabel(): String = undoStack.lastOrNull()?.label ?: ""
    fun redoLabel(): String = redoStack.lastOrNull()?.label ?: ""
}

// ─────────────────────────────────────────────────────────────
// SNAPSHOT — знімок блоків workspace для збереження в стеку
// Зберігаємо тільки blocks + selectedId (pan/zoom не undo-ються)
// ─────────────────────────────────────────────────────────────
data class WorkspaceSnapshot(
    val blocks: Map<String, WsBlock>,
    val selectedId: String?,
    val label: String,
    val timestampMs: Long = System.currentTimeMillis(),
) {
    companion object {
        fun from(state: WorkspaceState, label: String = ""): WorkspaceSnapshot =
            WorkspaceSnapshot(
                blocks     = state.blocks.toMap(),
                selectedId = state.selectedId,
                label      = label,
            )
    }

    fun toState(): WorkspaceState =
        WorkspaceState(
            blocks      = blocks.toMap(),
            selectedId  = selectedId,
        )
}

// ─────────────────────────────────────────────────────────────
// UNDO PUSH HELPER — зручна обгортка для ViewModel
// Зберігає стан з описом дії перед будь-якою мутацією блоків
// ─────────────────────────────────────────────────────────────
enum class UndoAction(val label: String) {
    ADD_BLOCK    ("Додати блок"),
    DELETE_BLOCK ("Видалити блок"),
    MOVE_BLOCK   ("Перемістити блок"),
    SNAP_BLOCK   ("З'єднати блок"),
    EDIT_PARAM   ("Редагувати параметр"),
    CLEAR_ALL    ("Очистити все"),
    DUPLICATE    ("Дублювати блок"),
    LOAD_EXAMPLE ("Завантажити приклад"),
    LOAD_SAVE    ("Завантажити збереження"),
}
