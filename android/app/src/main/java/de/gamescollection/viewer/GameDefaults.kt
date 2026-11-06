package de.gamescollection.viewer

import org.json.JSONArray
import org.json.JSONObject

/** Suggestions for new games; existing games and drafts keep their values. */
data class GameDefaults(val gameType: IgdbGameType = IgdbGameType.ALL,
    val format: String = "Digital", val status: String = "Nicht gespielt",
    val platforms: List<String> = emptyList()) {
    fun applyTo(game: Game, allowedPlatforms: List<String>): Game = if (game.id != 0) game else
        game.copy(format = format, status = status, platforms = platforms.filter { it in allowedPlatforms })

    fun json(): JSONObject = JSONObject().put("game_type", gameType.id ?: JSONObject.NULL)
        .put("format", format).put("status", status).put("platforms", JSONArray(platforms))

    companion object {
        fun fromJson(json: JSONObject?): GameDefaults {
            if (json == null) return GameDefaults()
            val type = if (json.isNull("game_type")) null else json.optInt("game_type", -1)
            return GameDefaults(IgdbGameType.entries.firstOrNull { it.id == type } ?: IgdbGameType.ALL,
                json.text("format").takeIf { it in listOf("Digital", "Physisch") } ?: "Digital",
                json.text("status").takeIf { it in GAME_STATUSES } ?: "Nicht gespielt",
                json.optJSONArray("platforms").strings().distinct())
        }
    }
}
