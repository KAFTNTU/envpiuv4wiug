package com.robocar.app.scratch

// ═══════════════════════════════════════════════════════════════════════
// WORKSPACE SAVE / LOAD
// JSON серіалізація workspace у рядок і назад.
// Зберігання у DataStore Preferences (до 6 іменованих слотів).
// Зовнішній export/import через Intent (share як .json файл).
// ═══════════════════════════════════════════════════════════════════════

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.robocar.app.model.BlockParam
import com.robocar.app.model.BlockType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── DataStore extension ──────────────────────────────────
private val Context.workspaceDataStore by preferencesDataStore("workspace_saves")

// ─────────────────────────────────────────────────────────────
// SAVE SLOT — один збережений слот
// ─────────────────────────────────────────────────────────────
data class SaveSlot(
    val index: Int,               // 0..5
    val name: String,             // ім'я дане користувачем
    val savedAt: Long,            // timestamp ms
    val blockCount: Int,          // скільки блоків
    val json: String,             // серіалізований workspace
) {
    val isEmpty: Boolean get() = json.isEmpty()
    val formattedDate: String get() {
        val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        return if (savedAt > 0) sdf.format(Date(savedAt)) else ""
    }
}

// ─────────────────────────────────────────────────────────────
// WORKSPACE SERIALIZER
// ─────────────────────────────────────────────────────────────
object WorkspaceSerializer {

    fun toJson(state: WorkspaceState): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("savedAt", System.currentTimeMillis())

        val blocksArr = JSONArray()
        for ((_, block) in state.blocks) {
            val bObj = JSONObject().apply {
                put("id",           block.id)
                put("type",         block.type.name)
                put("x",            block.x)
                put("y",            block.y)
                put("nextId",       block.nextId ?: JSONObject.NULL)
                put("subChainId",   block.subChainId ?: JSONObject.NULL)
                put("sub2ChainId",  block.sub2ChainId ?: JSONObject.NULL)
                put("isSubBlock",   block.isSubBlock)

                val paramsArr = JSONArray()
                for (p in block.params) {
                    val pObj = when (p) {
                        is BlockParam.NumberInput -> JSONObject().apply {
                            put("kind",  "num")
                            put("label", p.label)
                            put("value", p.value)
                            put("min",   p.min)
                            put("max",   p.max)
                        }
                        is BlockParam.DropdownInput -> JSONObject().apply {
                            put("kind",     "drop")
                            put("label",    p.label)
                            put("selected", p.selected)
                            val optsArr = JSONArray()
                            for ((l, v) in p.options) {
                                optsArr.put(JSONObject().apply { put("l", l); put("v", v) })
                            }
                            put("options", optsArr)
                        }
                        is BlockParam.TextInput -> JSONObject().apply {
                            put("kind",  "text")
                            put("label", p.label)
                            put("value", p.value)
                        }
                        else -> continue
                    }
                    paramsArr.put(pObj)
                }
                put("params", paramsArr)
            }
            blocksArr.put(bObj)
        }
        root.put("blocks", blocksArr)
        return root.toString(2)
    }

    fun fromJson(json: String): WorkspaceState? {
        return try {
            val root = JSONObject(json)
            val blocksArr = root.getJSONArray("blocks")
            val blocks = mutableMapOf<String, WsBlock>()

            for (i in 0 until blocksArr.length()) {
                val bObj = blocksArr.getJSONObject(i)
                val typeName = bObj.getString("type")
                val type = runCatching { BlockType.valueOf(typeName) }.getOrNull() ?: continue

                val paramsArr = bObj.getJSONArray("params")
                val params = mutableListOf<BlockParam>()
                for (j in 0 until paramsArr.length()) {
                    val pObj = paramsArr.getJSONObject(j)
                    val p = when (pObj.getString("kind")) {
                        "num" -> BlockParam.NumberInput(
                            label = pObj.getString("label"),
                            value = pObj.getDouble("value").toFloat(),
                            min   = pObj.optDouble("min", -100.0).toFloat(),
                            max   = pObj.optDouble("max", 100.0).toFloat(),
                        )
                        "drop" -> {
                            val optsArr = pObj.getJSONArray("options")
                            val options = (0 until optsArr.length()).map {
                                val o = optsArr.getJSONObject(it)
                                o.getString("l") to o.getString("v")
                            }
                            BlockParam.DropdownInput(
                                label    = pObj.getString("label"),
                                options  = options,
                                selected = pObj.getString("selected"),
                            )
                        }
                        "text" -> BlockParam.TextInput(
                            label = pObj.getString("label"),
                            value = pObj.getString("value"),
                        )
                        else -> continue
                    }
                    params.add(p)
                }

                val block = WsBlock(
                    id           = bObj.getString("id"),
                    type         = type,
                    params       = params,
                    x            = bObj.getDouble("x").toFloat(),
                    y            = bObj.getDouble("y").toFloat(),
                    nextId       = bObj.optString("nextId").takeIf { it.isNotEmpty() && it != "null" },
                    subChainId   = bObj.optString("subChainId").takeIf { it.isNotEmpty() && it != "null" },
                    sub2ChainId  = bObj.optString("sub2ChainId").takeIf { it.isNotEmpty() && it != "null" },
                    isSubBlock   = bObj.optBoolean("isSubBlock", false),
                )
                blocks[block.id] = block
            }
            WorkspaceState(blocks = blocks)
        } catch (e: Exception) {
            null
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SAVE MANAGER — збереження/завантаження слотів у DataStore
// ─────────────────────────────────────────────────────────────
class WorkspaceSaveManager(private val context: Context) {

    companion object {
        const val NUM_SLOTS = 6
        private fun slotKey(i: Int)       = stringPreferencesKey("slot_json_$i")
        private fun slotNameKey(i: Int)   = stringPreferencesKey("slot_name_$i")
        private fun slotTimeKey(i: Int)   = stringPreferencesKey("slot_time_$i")
        private fun slotCountKey(i: Int)  = stringPreferencesKey("slot_count_$i")
    }

    // ─── Потік усіх слотів ────────────────────────────────
    val slotsFlow: Flow<List<SaveSlot>> =
        context.workspaceDataStore.data.map { prefs ->
            (0 until NUM_SLOTS).map { i ->
                SaveSlot(
                    index      = i,
                    name       = prefs[slotNameKey(i)] ?: "",
                    savedAt    = prefs[slotTimeKey(i)]?.toLongOrNull() ?: 0L,
                    blockCount = prefs[slotCountKey(i)]?.toIntOrNull() ?: 0,
                    json       = prefs[slotKey(i)] ?: "",
                )
            }
        }

    // ─── Зберегти у слот ─────────────────────────────────
    suspend fun save(slotIndex: Int, name: String, state: WorkspaceState) {
        val json = WorkspaceSerializer.toJson(state)
        context.workspaceDataStore.edit { prefs ->
            prefs[slotKey(slotIndex)]      = json
            prefs[slotNameKey(slotIndex)]  = name.ifBlank { "Програма ${slotIndex + 1}" }
            prefs[slotTimeKey(slotIndex)]  = System.currentTimeMillis().toString()
            prefs[slotCountKey(slotIndex)] = state.blocks.size.toString()
        }
    }

    // ─── Завантажити зі слоту ─────────────────────────────
    suspend fun load(slotIndex: Int): WorkspaceState? {
        var json = ""
        context.workspaceDataStore.data.map { it[slotKey(slotIndex)] ?: "" }
            .collect { json = it; return@collect }
        return if (json.isNotEmpty()) WorkspaceSerializer.fromJson(json) else null
    }

    // ─── Очистити слот ────────────────────────────────────
    suspend fun clear(slotIndex: Int) {
        context.workspaceDataStore.edit { prefs ->
            prefs.remove(slotKey(slotIndex))
            prefs.remove(slotNameKey(slotIndex))
            prefs.remove(slotTimeKey(slotIndex))
            prefs.remove(slotCountKey(slotIndex))
        }
    }

    // ─── Автозбереження (slot 5 зарезервовано) ───────────
    suspend fun autoSave(state: WorkspaceState) {
        save(NUM_SLOTS - 1, "Автозбереження", state)
    }

    // ─── Отримати одразу (без Flow) ──────────────────────
    suspend fun loadDirect(slotIndex: Int): WorkspaceState? {
        var result: WorkspaceState? = null
        context.workspaceDataStore.data.map { prefs ->
            prefs[slotKey(slotIndex)] ?: ""
        }.collect { json ->
            if (json.isNotEmpty()) result = WorkspaceSerializer.fromJson(json)
            return@collect
        }
        return result
    }

    // ─── Export як файл (share intent) ───────────────────
    fun exportToFile(context: Context, state: WorkspaceState, name: String): Intent? {
        return try {
            val json = WorkspaceSerializer.toJson(state)
            val sdf  = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
            val fname = "${name.replace(" ", "_")}_${sdf.format(Date())}.roboscr"
            val file = File(context.cacheDir, fname)
            file.writeText(json)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "RoboScratch: $name")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) { null }
    }

    // ─── Import з URI (після file picker) ────────────────
    fun importFromUri(context: Context, uri: Uri): WorkspaceState? {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return null
            WorkspaceSerializer.fromJson(json)
        } catch (e: Exception) { null }
    }
}
