package de.gamescollection.viewer

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class IgdbClientTest {
    @Test fun directSearchUsesOnlyIgdbCredentialsAndMapsResults() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""[{"id":123,"name":"Testspiel","age_ratings":[{"organization":{"name":"USK"},"rating_category":{"rating":"12"}}]}]"""))
            val client = IgdbClient(server.url("/v4/games").toString())
            val token = IgdbCredentials("owner", "client", "token", System.currentTimeMillis() + 3600000)
            val game = client.search(token, "Testspiel", "", IgdbGameType.DLC).single()
            assertEquals("USK 12", game.ageRating)
            assertEquals(123, game.igdbId)
            val request = server.takeRequest()
            assertEquals("Bearer token", request.getHeader("Authorization"))
            assertEquals("client", request.getHeader("Client-ID"))
            assertNull(request.getHeader("Cookie"))
            assertNull(request.getHeader("X-CSRFToken"))
            assertTrue(request.body.readUtf8().contains("where game_type = 1;"))
            server.enqueue(MockResponse().setResponseCode(401))
            try { client.search(token, "Testspiel", ""); fail("Expected expiry") }
            catch (error: IgdbException) { assertTrue(error.expired) }
        } finally { server.shutdown() }
    }

    @Test fun tokenIsBoundToOwnerAndUsesActualLifetime() {
        val credentials = IgdbCredentials.fromServer("https://server/|1", JSONObject()
            .put("client_id", "client").put("access_token", "test-token").put("expires_in", 3600), 1000)
        assertEquals(3601000L, credentials.expiresAt)
        assertTrue(credentials.valid(1000))
        assertFalse(credentials.valid(3541000))
        assertNull(IgdbCredentials.restore(credentials.encode(), "https://other/|1"))
        assertNull(IgdbCredentials.restore(credentials.encode(), "https://server/|2"))
        assertEquals(credentials.token, IgdbCredentials.restore(credentials.encode(), credentials.owner)?.token)
        assertNull(IgdbCredentials.restore("broken", credentials.owner))
    }
    @Test fun queryEscapesTitlesAndPreservesYearAndIdSearch() {
        assertTrue(IgdbClient.queryBody("A\"; \\B", "2024").contains("search \"A\\\"; \\\\B\";"))
        assertTrue(IgdbClient.queryBody("2024", "2024").startsWith("search"))
        assertTrue(IgdbClient.queryBody("123", "").startsWith("where id = 123;"))
        assertTrue(IgdbClient.queryBody("Spiel", "2024").contains("first_release_date >= 1704067200"))
        assertThrows(IllegalArgumentException::class.java) { IgdbClient.queryBody("Spiel", "1800") }
    }
    @Test fun typeFilterCombinesWithYearAndIdBeforeLimitingResults() {
        val fullGame = IgdbClient.queryBody("Battlefield 6", "2025", IgdbGameType.MAIN_GAME)
        assertTrue(fullGame.contains("where game_type = 0 & first_release_date >= 1735689600 & first_release_date < 1767225600;"))
        assertTrue(fullGame.endsWith("limit 10;"))
        assertEquals(1, Regex("where ").findAll(fullGame).count())
        assertTrue(IgdbClient.queryBody("317407", "", IgdbGameType.MAIN_GAME)
            .startsWith("where id = 317407 & game_type = 0;"))
        assertFalse(IgdbClient.queryBody("Spiel", "").contains("game_type"))
        assertFalse(IgdbClient.queryBody("Spiel", "2025").contains("game_type"))
        for (type in IgdbGameType.entries.filter { it.id != null }) {
            assertTrue(IgdbClient.queryBody("Spiel", "", type).contains("where game_type = ${type.id};"))
        }
    }
    @Test fun ratingsPreferUskThenPegiAndIgnoreOtherSystems() {
        val ratings = JSONArray("""[{"organization":{"name":"PEGI"},"rating_category":{"rating":"18"}},
            {"organization":{"name":"USK"},"rating_category":{"rating":"16"}}]""")
        assertEquals("USK 16", IgdbClient.preferredRating(ratings))
        ratings.remove(1)
        assertEquals("PEGI 18", IgdbClient.preferredRating(ratings))
        assertEquals("", IgdbClient.preferredRating(JSONArray("""[{"organization":{"name":"ESRB"},"rating_category":{"rating":"M"}}]""")))
    }
}
