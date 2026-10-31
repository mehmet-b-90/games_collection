package de.gamescollection.viewer

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class IgdbException(message: String, val expired: Boolean = false) : Exception(message)

/** IGDB game_types, verified against the API on 2026-10-30; null keeps search unrestricted. */
enum class IgdbGameType(val id: Int?, val label: String) {
    ALL(null, "Alle Typen"),
    MAIN_GAME(0, "Spielevollversion"),
    DLC(1, "DLC"),
    EXPANSION(2, "Erweiterung"),
    BUNDLE(3, "Bundle"),
    STANDALONE_EXPANSION(4, "Eigenständige Erweiterung"),
    MOD(5, "Mod"),
    EPISODE(6, "Episode"),
    SEASON(7, "Season"),
    REMAKE(8, "Remake"),
    REMASTER(9, "Remaster"),
    EXPANDED_GAME(10, "Erweiterte Spielversion"),
    PORT(11, "Portierung"),
    FORK(12, "Abspaltung (Fork)"),
    PACK(13, "Paket / Add-on"),
    UPDATE(14, "Update"),
}

class IgdbCredentials(val owner: String, val clientId: String, val token: String, val expiresAt: Long) {
    fun valid(now: Long = System.currentTimeMillis()) = expiresAt - now > 60_000
    fun encode(): String = JSONObject().put("owner", owner).put("client_id", clientId)
        .put("access_token", token).put("expires_at", expiresAt).toString()
    companion object {
        fun fromServer(owner: String, json: JSONObject, requestedAt: Long): IgdbCredentials {
            val seconds = json.getLong("expires_in")
            require(seconds in 61..31_536_000)
            return IgdbCredentials(owner, json.getString("client_id"), json.getString("access_token"),
                requestedAt + seconds * 1000).also { require(it.clientId.isNotBlank() && it.token.isNotBlank()) }
        }
        fun restore(value: String?, owner: String): IgdbCredentials? = runCatching {
            val json = JSONObject(value ?: return null)
            IgdbCredentials(json.getString("owner"), json.getString("client_id"),
                json.getString("access_token"), json.getLong("expires_at")).takeIf { it.owner == owner }
        }.getOrNull()
    }
}

/** Separate client: never sends Games Collection cookies, CSRF tokens or the client secret. */
class IgdbClient internal constructor(private val endpoint: String = "https://api.igdb.com/v4/games") {
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(30, TimeUnit.SECONDS).build()

    suspend fun search(credentials: IgdbCredentials, query: String, year: String,
        gameType: IgdbGameType = IgdbGameType.ALL): List<Game> {
        if (!credentials.valid()) throw IgdbException("Der IGDB-Zugang ist abgelaufen. Verbinde dich zum Erneuern mit deinem Server.", true)
        val response = client.newCall(Request.Builder().url(endpoint)
            .header("Client-ID", credentials.clientId).header("Authorization", "Bearer ${credentials.token}")
            .post(queryBody(query, year, gameType).toRequestBody("text/plain; charset=utf-8".toMediaType())).build()).readBody()
        if (response.code == 401) throw IgdbException("IGDB hat den Zugang abgelehnt. Verbinde dich zum Erneuern mit deinem Server.", true)
        if (response.code == 429) throw IgdbException("Zu viele IGDB-Anfragen. Bitte warte kurz und versuche es erneut.")
        if (response.code !in 200..299) throw IgdbException("IGDB ist derzeit nicht verfügbar (HTTP ${response.code}).")
        return JSONArray(response.body).objects().map { json ->
            json.put("mapped_age_rating", preferredRating(json.optJSONArray("age_ratings")))
            Game.fromIgdb(json)
        }
    }

    companion object {
        fun queryBody(query: String, year: String, gameType: IgdbGameType = IgdbGameType.ALL): String {
            require(query.isNotBlank() && query.length <= 200)
            require(year.isEmpty() || year.toIntOrNull() in 1900..2100)
            val fields = "fields id,name,url,cover.image_id,first_release_date,platforms.name,involved_companies.company.name,involved_companies.developer,involved_companies.publisher,game_engines.name,age_ratings.organization.name,age_ratings.rating_category.rating;"
            val filters = mutableListOf<String>()
            gameType.id?.let { filters.add("game_type = $it") }
            if (query.all(Char::isDigit) && year.isEmpty()) {
                val id = query.toIntOrNull()
                require(id != null && id > 0)
                return "where " + (listOf("id = $id") + filters).joinToString(" & ") + "; $fields limit 1;"
            }
            val escaped = query.replace("\\", "\\\\").replace("\"", "\\\"")
            if (year.isNotEmpty()) {
                val start = LocalDate.of(year.toInt(), 1, 1).atStartOfDay().toEpochSecond(ZoneOffset.UTC)
                val end = LocalDate.of(year.toInt() + 1, 1, 1).atStartOfDay().toEpochSecond(ZoneOffset.UTC)
                filters.add("first_release_date >= $start & first_release_date < $end")
            }
            val where = if (filters.isEmpty()) "" else "where ${filters.joinToString(" & ")};"
            return "search \"$escaped\"; $where $fields limit 10;"
        }
        fun preferredRating(ratings: JSONArray?): String {
            for (name in listOf("USK", "PEGI")) {
                for (rating in ratings.objects()) {
                    val organization = rating.optJSONObject("organization")?.text("name")
                        ?: when (rating.optInt("organization")) { 4 -> "USK"; 2 -> "PEGI"; else -> "" }
                    val category = rating.optJSONObject("rating_category")?.text("rating").orEmpty().trim()
                    if (organization == name && category.isNotEmpty()) return "$name $category"
                }
            }
            return ""
        }
    }
}
