package de.gamescollection.viewer

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NativeClientTest {
    private class MemoryCookies : CookiePersistence {
        var values = emptyList<String>()
        override fun read() = values
        override fun write(cookies: List<String>) { values = cookies }
    }

    @Test fun platformFilteringIsExactAndUnratedGamesDoNotMatchMinimum() {
        val steam = Game(title = "Test", platforms = listOf("Steam"), rating = 0)
        assertTrue(CollectionFilter(platform = "Steam", minimumRating = 0).matches(steam))
        assertFalse(CollectionFilter(platform = "Steam").matches(steam.copy(platforms = listOf("Steam Deck"))))
        assertFalse(CollectionFilter(minimumRating = 0).matches(steam.copy(rating = null)))
        assertTrue(CollectionFilter(query = "massive").matches(steam.copy(developer = "Massive Entertainment")))
    }

    @Test fun updatesCarryVersionButCreatesNeverDo() {
        val game = Game(id = 22, version = 7, title = "Test", platforms = listOf("Steam"), rating = null)
        assertEquals(7, game.payload().getInt("version"))
        assertTrue(game.payload().isNull("rating"))
        assertFalse(game.copy(id = 0).payload().has("version"))
        val server = game.payload().put("id", game.id)
        assertEquals(game, Game.fromJson(server))
    }

    @Test fun cookiesAreOriginBoundAndOnlyPersistentCookiesSurviveRestart() {
        val base = "https://games.example.org/".toHttpUrl()
        val persistence = MemoryCookies()
        val jar = ServerCookies(base, persistence)
        jar.saveFromResponse(base, listOf(Cookie.parse(base, "session=private; Path=/; Secure")!!,
            Cookie.parse(base, "remember_token=persistent; Max-Age=600; Path=/; Secure")!!))
        assertEquals(2, jar.loadForRequest(base).size)
        assertEquals(1, persistence.values.size)
        assertTrue(jar.loadForRequest("https://other.example.org/".toHttpUrl()).isEmpty())
        assertTrue(jar.loadForRequest("http://games.example.org/".toHttpUrl()).isEmpty())
        assertEquals("remember_token", ServerCookies(base, persistence).loadForRequest(base).single().name)
        jar.clear()
        assertTrue(persistence.values.isEmpty())
    }

    @Test fun csrfFailuresRefreshAndRetryOnceWithNewToken() {
        MockWebServer().use { server ->
            server.enqueue(json("""{"api_version":1,"csrf_token":"old","authenticated":false}"""))
            server.enqueue(json("""{"error":"CSRF"}""").setResponseCode(400).addHeader("X-CSRF-Error", "1"))
            server.enqueue(json("""{"api_version":1,"csrf_token":"new","authenticated":false}"""))
            server.enqueue(json("""{"csrf_token":"signed-in","authenticated":true}"""))
            val api = ApiClient(server.url("/").toString(), MemoryCookies())
            api.session()
            api.login("Tester", "password", false)
            assertEquals("GET", server.takeRequest().method)
            val failed = server.takeRequest()
            assertEquals("old", failed.getHeader("X-CSRFToken"))
            assertEquals(server.url("/").toString(), failed.getHeader("Referer"))
            assertEquals("GET", server.takeRequest().method)
            val retry = server.takeRequest()
            assertEquals("new", retry.getHeader("X-CSRFToken"))
            assertEquals("Tester", JSONObject(retry.body.readUtf8()).getString("username"))
        }
    }

    @Test fun redirectsAreNotFollowedAndConflictsRemainVisible() {
        MockWebServer().use { server ->
            val api = ApiClient(server.url("/").toString(), MemoryCookies())
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "https://example.org/"))
            val redirect = assertThrows(ApiException::class.java) { api.session() }
            assertEquals(302, redirect.status)
            assertEquals(1, server.requestCount)
            server.enqueue(json("""{"error":"Zwischenzeitlich geändert"}""").setResponseCode(409))
            val conflict = assertThrows(ApiException::class.java) { api.save(Game(id = 1, title = "Test", version = 1)) }
            assertEquals(409, conflict.status)
            assertEquals("Zwischenzeitlich geändert", conflict.message)
        }
    }

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
}
