package de.gamescollection.viewer

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

internal const val PLAYED_STATUS = "Gespielt"
val GAME_STATUSES = listOf("Nicht gespielt", PLAYED_STATUS, "Abgebrochen")

data class GameCopy(val platform: String, val format: String = "Digital", val edition: String = "") {
    fun json() = JSONObject().put("platform", platform).put("format", format).put("edition", edition)
    companion object {
        fun fromJson(json: JSONObject) = GameCopy(json.getString("platform"), json.getString("format"), json.getString("edition"))
    }
}

data class Game(
    val id: Int = 0, val version: Int = 1, val title: String,
    val cover: String = "", val release: String = "", val developer: String = "",
    val publisher: String = "", val engine: String = "", val available: String = "",
    val ageRating: String = "", val platforms: List<String> = emptyList(),
    val format: String = "Digital", val status: String = "Nicht gespielt",
    val rating: Int? = null, val igdbId: Int? = null, val igdbUrl: String = "",
    val customCoverUrl: String = "", val customCoverEdit: String? = null,
    // null preserves the exact legacy payload of already attempted offline writes.
    val copies: List<GameCopy>? = null,
    val playtimeMinutes: Int? = null,
    val playtimeEdited: Boolean = false
) {
    val ownedCopies: List<GameCopy> get() = copies ?: platforms.map { GameCopy(it, format) }
    val ownedPlatforms: List<String> get() = ownedCopies.map { it.platform }.distinct()
    val writesPlaytime: Boolean get() = playtimeEdited && status == PLAYED_STATUS
    fun withCopies(values: List<GameCopy>) = copy(copies = values, platforms = values.map { it.platform }.distinct(),
        format = values.firstOrNull()?.format ?: "Digital")

    fun payload() = JSONObject().apply {
        put("title", title); put("cover_url", cover); put("release_date", release)
        put("developer", developer); put("publisher", publisher); put("game_engine", engine)
        put("available_platforms", available); put("age_rating", ageRating)
        if (copies == null) {
            put("owned_platform", platforms.joinToString(", ")); put("format", format)
        } else put("copies", JSONArray(copies.map { it.json() }))
        put("status", status); put("rating", rating ?: JSONObject.NULL)
        if (writesPlaytime) put("playtime_minutes", playtimeMinutes ?: JSONObject.NULL)
        put("igdb_id", igdbId ?: JSONObject.NULL); put("igdb_url", igdbUrl)
        customCoverEdit?.let { put("custom_cover", it.ifEmpty { null } ?: JSONObject.NULL) }
        if (id != 0) put("version", version)
    }

    companion object {
        fun fromJson(json: JSONObject) = Game(
            id = json.getInt("id"), version = json.getInt("version"), title = json.getString("title"),
            cover = json.text("cover_url"), release = json.text("release_date"),
            developer = json.text("developer"), publisher = json.text("publisher"),
            engine = json.text("game_engine"), available = json.text("available_platforms"),
            ageRating = json.text("age_rating"), platforms = if (json.has("copies")) json.getJSONArray("copies").objects().map { it.getString("platform") }.distinct()
                else json.text("owned_platform").split(',').map { it.trim() }.filter { it.isNotEmpty() },
            format = if (json.has("copies")) json.getJSONArray("copies").objects().firstOrNull()?.getString("format") ?: "Digital"
                else json.text("format"), status = json.text("status"),
            rating = if (json.isNull("rating")) null else json.getInt("rating"),
            igdbId = if (json.isNull("igdb_id")) null else json.getInt("igdb_id"), igdbUrl = json.text("igdb_url"),
            customCoverUrl = json.text("custom_cover_url"),
            customCoverEdit = if (json.has("custom_cover")) json.text("custom_cover") else null,
            copies = if (json.has("copies")) json.getJSONArray("copies").objects().map(GameCopy::fromJson) else null,
            playtimeMinutes = if (json.isNull("playtime_minutes")) null else json.getInt("playtime_minutes"),
            playtimeEdited = json.optBoolean("playtime_edited")
        )

        fun fromIgdb(json: JSONObject): Game {
            val companies = json.optJSONArray("involved_companies").objects()
            val image = json.optJSONObject("cover")?.text("image_id").orEmpty()
            return Game(
                title = json.getString("name"), igdbId = json.getInt("id"), igdbUrl = json.text("url"),
                cover = if (image.isEmpty()) "" else "https://images.igdb.com/igdb/image/upload/t_cover_big/$image.jpg",
                release = if (json.isNull("first_release_date")) "" else Instant.ofEpochSecond(json.getLong("first_release_date")).atZone(ZoneOffset.UTC).year.toString(),
                developer = companies.filter { it.optBoolean("developer") }.joinToString(", ") { it.optJSONObject("company")?.text("name").orEmpty() },
                publisher = companies.filter { it.optBoolean("publisher") }.joinToString(", ") { it.optJSONObject("company")?.text("name").orEmpty() },
                engine = json.optJSONArray("game_engines").objects().joinToString(", ") { it.text("name") },
                available = json.optJSONArray("platforms").objects().joinToString(", ") { it.text("name") },
                ageRating = json.text("mapped_age_rating")
            )
        }
    }
}

data class CollectionFilter(
    val query: String = "", val platform: String = "", val status: String = "",
    val format: String = "", val engine: String = "", val ageRating: String = "", val minimumRating: Int? = null
) {
    val active: Boolean get() = platform.isNotEmpty() || status.isNotEmpty() || format.isNotEmpty()
            || engine.isNotEmpty() || ageRating.isNotEmpty() || minimumRating != null

    fun matches(game: Game): Boolean =
        listOf(game.title, game.developer, game.publisher).any { it.contains(query.trim(), ignoreCase = true) }
                && ((platform.isEmpty() && format.isEmpty()) || game.ownedCopies.any {
                    (platform.isEmpty() || it.platform == platform) && (format.isEmpty() || it.format == format)
                })
                && (status.isEmpty() || game.status == status)
                && (engine.isEmpty() || game.engine.split(',').map { it.trim() }.contains(engine))
                && (ageRating.isEmpty() || game.ageRating == ageRating)
                && (minimumRating == null || game.rating?.let { it >= minimumRating } == true)
}

enum class GameSort(val label: String) {
    NEWEST("Zuletzt hinzugefügt"), TITLE("Titel A–Z"), RELEASE("Release absteigend"), RATING("Beste Bewertung")
}

fun List<Game>.filtered(filter: CollectionFilter, sort: GameSort, checkpoint: () -> Unit = {}): List<Game> {
    val matching = filterIndexed { index, game ->
        if (index % 128 == 0) checkpoint()
        filter.matches(game)
    }
    val comparator = when (sort) {
        GameSort.NEWEST -> compareByDescending<Game> { it.id }
        GameSort.TITLE -> compareBy { it.title.lowercase(Locale.GERMAN) }
        GameSort.RELEASE -> compareByDescending { it.release.toIntOrNull() ?: 0 }
        GameSort.RATING -> compareByDescending { it.rating ?: -1 }
    }
    var comparisons = 0
    return matching.sortedWith { left, right ->
        if (comparisons++ % 128 == 0) checkpoint()
        comparator.compare(left, right)
    }
}

internal fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key, "")
internal fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
internal fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }
