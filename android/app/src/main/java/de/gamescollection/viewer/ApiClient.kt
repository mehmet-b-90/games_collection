package de.gamescollection.viewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(val status: Int, message: String) : IOException(message)

class ServerCookies(private val base: HttpUrl, private val persistence: CookiePersistence) : CookieJar {
    private val entries = mutableListOf<Cookie>()
    init {
        entries.addAll(persistence.read().mapNotNull { Cookie.parse(base, it) }
            .filter { it.persistent && it.expiresAt > System.currentTimeMillis() && it.matches(base) })
    }
    @Synchronized override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!ServerAddress.sameOrigin(base.toString(), url.toString())) return
        cookies.forEach { fresh ->
            entries.removeAll { it.name == fresh.name && it.domain == fresh.domain && it.path == fresh.path }
            if (fresh.expiresAt > System.currentTimeMillis()) entries.add(fresh)
        }
        persistence.write(entries.filter { it.persistent && it.expiresAt > System.currentTimeMillis() }.map { it.toString() })
    }
    @Synchronized override fun loadForRequest(url: HttpUrl): List<Cookie> =
        if (!ServerAddress.sameOrigin(base.toString(), url.toString())) emptyList()
        else entries.filter { it.expiresAt > System.currentTimeMillis() && it.matches(url) }

    @Synchronized fun clear() { entries.clear(); persistence.write(emptyList()) }
}

/** A single ViewModel serializes session-changing operations. */
class ApiClient private constructor(val server: String, persistence: CookiePersistence) {
    companion object {
        suspend fun create(server: String, persistence: CookiePersistence): ApiClient =
            withContext(Dispatchers.IO) { ApiClient(server, persistence) }
    }
    private val base = server.toHttpUrl()
    private val cookies = ServerCookies(base, persistence)
    private val client = OkHttpClient.Builder().cookieJar(cookies)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).build()
    private var csrf = ""
    private var idempotentWrites = false

    fun clearSession() { cookies.clear(); csrf = ""; idempotentWrites = false }
    suspend fun session(): JSONObject {
        val value = JSONObject(request("GET", "/api/session"))
        if (value.optInt("api_version") != 1 || value.optString("csrf_token").isEmpty()) {
            throw ApiException(426, "Der Server unterstützt diese App noch nicht. Bitte aktualisiere Games Collection.")
        }
        csrf = value.getString("csrf_token")
        idempotentWrites = value.optJSONObject("capabilities")?.optBoolean("idempotent_game_writes") == true
        return value
    }
    suspend fun login(username: String, password: String, remember: Boolean): JSONObject {
        if (csrf.isEmpty()) session()
        val result = JSONObject(request("POST", "/api/session", JSONObject()
            .put("username", username).put("password", password).put("remember", remember)))
        csrf = result.getString("csrf_token")
        idempotentWrites = result.optJSONObject("capabilities")?.optBoolean("idempotent_game_writes") == true
        return result
    }
    suspend fun logout() {
        try { request("DELETE", "/api/session") } finally { clearSession() }
    }
    suspend fun collection(): JSONObject = JSONObject(request("GET", "/api/games"))
    suspend fun save(game: Game, operationKey: String) {
        requireIdempotency()
        request(if (game.id == 0) "POST" else "PUT",
            if (game.id == 0) "/api/games" else "/api/games/${game.id}", game.payload(), operationKey = operationKey)
    }
    suspend fun delete(game: Game, operationKey: String) {
        requireIdempotency()
        request("DELETE", "/api/games/${game.id}", operationKey = operationKey)
    }
    private suspend fun requireIdempotency() {
        if (!idempotentWrites) session()
        if (!idempotentWrites) throw ApiException(426,
            "Bitte aktualisiere den Server, damit Spieländerungen sicher wiederholt werden können.")
    }
    suspend fun search(query: String, year: String): List<Game> = JSONArray(request("POST", "/api/search",
        JSONObject().put("query", query).put("release_year", year.ifEmpty { null } ?: JSONObject.NULL))).objects().map(Game::fromIgdb)

    private suspend fun request(method: String, path: String, data: JSONObject? = null, retryCsrf: Boolean = true, operationKey: String? = null): String {
        val url = base.resolve(path) ?: throw IOException("Ungültige Serveradresse.")
        check(ServerAddress.sameOrigin(server, url.toString()))
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        if (operationKey != null) builder.header("Idempotency-Key", operationKey)
        if (method != "GET") builder.header("X-CSRFToken", csrf).header("Referer", server)
        builder.method(method, if (method == "GET") null else (data?.toString() ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType()))
        val response = client.newCall(builder.build()).readBody()
        val body = response.body
        if (response.csrfError && retryCsrf && method != "GET") {
            session()
            return request(method, path, data, false, operationKey)
        }
        if (response.code in 300..399) throw ApiException(response.code,
            "Der Server leitet die Anfrage um. Prüfe HTTPS und die vollständige Serveradresse.")
        if (response.code !in 200..299) {
            val message = runCatching { JSONObject(body).getString("error") }.getOrNull()
            throw ApiException(response.code, message ?: if (response.code == 404) "API nicht gefunden. Bitte aktualisiere den Games-Collection-Server." else "Der Server antwortet mit HTTP ${response.code}.")
        }
        if (response.contentType?.contains("application/json") != true) {
            throw ApiException(502, "Keine gültige API-Antwort. Der Ausgang der Anfrage ist möglicherweise unklar.")
        }
        return body
    }
}
