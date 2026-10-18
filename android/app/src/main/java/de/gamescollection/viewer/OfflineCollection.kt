package de.gamescollection.viewer

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Persist the server snapshot and outbox together, before showing a successful local save. */
data class QueuedWrite(val write: PendingGameWrite, val localId: Int,
    val attempted: Boolean = false, val rejection: String? = null) {
    fun json() = JSONObject().put("write", write.json()).put("localId", localId)
        .put("attempted", attempted).put("rejection", rejection ?: JSONObject.NULL)
}

data class OfflineCollection(val owner: String, val username: String,
    val games: List<Game> = emptyList(), val platforms: List<String> = emptyList(),
    val queue: List<QueuedWrite> = emptyList(), val nextId: Int = -1) {
    fun visible(): List<Game> {
        val result = games.associateBy { it.id }.toMutableMap()
        queue.forEach { entry ->
            if (entry.write.deleting) result.remove(entry.localId)
            else result[entry.localId] = entry.write.game.copy(id = entry.localId)
        }
        return result.values.toList()
    }

    fun enqueue(game: Game, deleting: Boolean): OfflineCollection {
        val previous = queue.find { it.localId == game.id }
        check(previous == null || !previous.attempted) { "Bitte synchronisiere zuerst den offenen Vorgang für diesen Titel." }
        val id = if (game.id == 0) nextId else game.id
        val rest = queue.filterNot { it.localId == id }
        // An unsent local creation can be removed without a server request.
        if (deleting && id < 0) return copy(queue = rest)
        val wireGame = game.copy(id = if (id < 0) 0 else id)
        return copy(queue = rest + QueuedWrite(PendingGameWrite(wireGame, deleting), id),
            nextId = if (game.id == 0) nextId - 1 else nextId)
    }

    fun encode(): String = JSONObject().put("owner", owner).put("username", username)
        .put("games", JSONArray(games.map { it.savedJson() })).put("platforms", JSONArray(platforms))
        .put("queue", JSONArray(queue.map { it.json() })).put("nextId", nextId).toString()

    companion object {
        fun decode(value: String): OfflineCollection {
            val json = JSONObject(value)
            return OfflineCollection(json.getString("owner"), json.getString("username"),
                json.getJSONArray("games").objects().map(Game::fromJson), json.getJSONArray("platforms").strings(),
                json.getJSONArray("queue").objects().map {
                    QueuedWrite(PendingGameWrite.restore(it.getJSONObject("write")), it.getInt("localId"),
                        it.getBoolean("attempted"), if (it.isNull("rejection")) null else it.getString("rejection"))
                }, json.getInt("nextId"))
        }
    }
}

class OfflineStore(context: Context) {
    private val directory = File(context.filesDir, "offline_collections")
    private fun file(owner: String): AtomicFile {
        check(directory.isDirectory || directory.mkdirs()) { "Lokaler Speicher ist nicht verfügbar." }
        val name = MessageDigest.getInstance("SHA-256").digest(owner.toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory, "$name.json"))
    }
    fun read(owner: String): OfflineCollection? {
        val file = file(owner)
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        return OfflineCollection.decode(file.readFully().toString(Charsets.UTF_8)).also { check(it.owner == owner) }
    }
    fun write(value: OfflineCollection) {
        val file = file(value.owner)
        val stream = file.startWrite()
        try { stream.write(value.encode().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (error: Exception) { file.failWrite(stream); throw error }
    }
}
