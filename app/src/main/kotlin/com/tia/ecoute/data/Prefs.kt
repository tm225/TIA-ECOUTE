package com.tia.ecoute.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tia_ecoute", Context.MODE_PRIVATE)

    private fun longs(key: String): List<Long> =
        (sp.getString(key, "") ?: "").split(",").mapNotNull { it.toLongOrNull() }

    // --- Favoris
    fun favorites(): Set<Long> = longs("fav").toSet()
    fun saveFavorites(s: Set<Long>) {
        sp.edit().putString("fav", s.joinToString(",")).apply()
    }

    // --- Playlists
    fun playlists(): List<Playlist> {
        val raw = sp.getString("playlists", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ids = o.getJSONArray("t")
                Playlist(o.getLong("id"), o.getString("n"), (0 until ids.length()).map { ids.getLong(it) })
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePlaylists(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { p ->
            val ids = JSONArray()
            p.trackIds.forEach { ids.put(it) }
            arr.put(JSONObject().put("id", p.id).put("n", p.name).put("t", ids))
        }
        sp.edit().putString("playlists", arr.toString()).apply()
    }

    // --- Statistiques d'écoute
    fun playCounts(): Map<Long, Int> {
        val raw = sp.getString("counts", "") ?: ""
        val m = HashMap<Long, Int>()
        raw.split(",").forEach { e ->
            val p = e.split(":")
            if (p.size == 2) {
                val k = p[0].toLongOrNull()
                val v = p[1].toIntOrNull()
                if (k != null && v != null) m[k] = v
            }
        }
        return m
    }

    fun savePlayCounts(m: Map<Long, Int>) {
        sp.edit().putString("counts", m.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()
    }

    fun recents(): List<Long> = longs("recent")
    fun saveRecents(l: List<Long>) {
        sp.edit().putString("recent", l.joinToString(",")).apply()
    }

    // --- Reprise de session
    fun saveSession(ids: List<Long>, currentId: Long, positionMs: Long) {
        sp.edit()
            .putString("s_ids", ids.joinToString(","))
            .putLong("s_cur", currentId)
            .putLong("s_pos", positionMs)
            .apply()
    }

    fun sessionIds(): List<Long> = longs("s_ids")
    fun sessionCurrent(): Long = sp.getLong("s_cur", 0L)
    fun sessionPosition(): Long = sp.getLong("s_pos", 0L)

    // --- Réglages
    var minDurationSec: Int
        get() = sp.getInt("min_dur", 30)
        set(v) {
            sp.edit().putInt("min_dur", v).apply()
        }

    var eqEnabled: Boolean
        get() = sp.getBoolean("eq_on", false)
        set(v) {
            sp.edit().putBoolean("eq_on", v).apply()
        }

    var bass: Int
        get() = sp.getInt("bass", 0)
        set(v) {
            sp.edit().putInt("bass", v).apply()
        }

    var virt: Int
        get() = sp.getInt("virt", 0)
        set(v) {
            sp.edit().putInt("virt", v).apply()
        }

    fun eqLevels(): List<Int> =
        (sp.getString("eq_levels", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }

    fun saveEqLevels(l: List<Int>) {
        sp.edit().putString("eq_levels", l.joinToString(",")).apply()
    }

    // --- Thème, volume, lecture, fichiers masqués
    var themeIndex: Int
        get() = sp.getInt("theme", 0)
        set(v) { sp.edit().putInt("theme", v).apply() }
    var boost: Int
        get() = sp.getInt("boost", 0)
        set(v) { sp.edit().putInt("boost", v).apply() }
    var resumeOnStart: Boolean
        get() = sp.getBoolean("resume", true)
        set(v) { sp.edit().putBoolean("resume", v).apply() }
    var keepScreenOn: Boolean
        get() = sp.getBoolean("keep_on", false)
        set(v) { sp.edit().putBoolean("keep_on", v).apply() }
    var skipSilence: Boolean
        get() = sp.getBoolean("skip_sil", false)
        set(v) { sp.edit().putBoolean("skip_sil", v).apply() }
    var pauseOnUnplug: Boolean
        get() = sp.getBoolean("noisy", true)
        set(v) { sp.edit().putBoolean("noisy", v).apply() }

    fun hiddenFolders(): Set<String> = (sp.getStringSet("hid_f", emptySet()) ?: emptySet()).toSet()
    fun saveHiddenFolders(s: Set<String>) { sp.edit().putStringSet("hid_f", HashSet(s)).apply() }
    fun hiddenIds(): Set<Long> = longs("hid_i").toSet()
    fun saveHiddenIds(s: Set<Long>) { sp.edit().putString("hid_i", s.joinToString(",")).apply() }

    var discStyle: Int
        get() = sp.getInt("disc_style", 0)
        set(v) { sp.edit().putInt("disc_style", v).apply() }
    var live: Boolean
        get() = sp.getBoolean("live", false)
        set(v) { sp.edit().putBoolean("live", v).apply() }
}
