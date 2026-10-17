package de.gamescollection.viewer

import org.json.JSONObject
import java.util.UUID

internal fun Game.savedJson(): JSONObject = payload().put("id", id).put("version", version)

data class PendingGameWrite(val game: Game, val deleting: Boolean = false, val key: String = UUID.randomUUID().toString()) {
    fun json(): JSONObject = JSONObject().put("game", game.savedJson()).put("deleting", deleting).put("key", key)
    companion object {
        fun restore(json: JSONObject) = PendingGameWrite(Game.fromJson(json.getJSONObject("game")),
            json.getBoolean("deleting"), UUID.fromString(json.getString("key")).toString())
    }
}

/** One account-bound draft, never the collection or login credentials. */
data class EditorState(val game: Game? = null, val editing: Boolean = false, val adding: Boolean = false,
    val open: Boolean = false, val pending: PendingGameWrite? = null) {
    fun encode(): String = JSONObject().put("game", game?.savedJson() ?: JSONObject.NULL)
        .put("editing", editing).put("adding", adding).put("open", open)
        .put("pending", pending?.json() ?: JSONObject.NULL).toString()

    companion object {
        fun restoreForOwner(value: String?, savedOwner: String?, verifiedOwner: String?): EditorState =
            if (verifiedOwner != null && verifiedOwner == savedOwner && value != null) restore(value) else EditorState()

        fun restore(value: String): EditorState = runCatching {
            val json = JSONObject(value)
            EditorState(json.optJSONObject("game")?.let(Game::fromJson), json.getBoolean("editing"),
                json.getBoolean("adding"), json.getBoolean("open"),
                json.optJSONObject("pending")?.let(PendingGameWrite::restore))
        }.getOrDefault(EditorState())
    }
}
