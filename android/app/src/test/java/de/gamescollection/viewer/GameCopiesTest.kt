package de.gamescollection.viewer

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GameCopiesTest {
    private val copies = listOf(GameCopy("PlayStation 4", "Digital"), GameCopy("Steam", "Physisch", "Special"))

    @Test fun platformAndFormatMustMatchTheSameCopy() {
        val game = Game(title = "Test").withCopies(copies)
        assertFalse(CollectionFilter(platform = "PlayStation 4", format = "Physisch").matches(game))
        assertTrue(CollectionFilter(platform = "Steam", format = "Physisch").matches(game))
        assertTrue(CollectionFilter(format = "Digital").matches(game))
        assertTrue(CollectionFilter().matches(game.withCopies(emptyList())))
        assertFalse(CollectionFilter(format = "Digital").matches(game.withCopies(emptyList())))
    }

    @Test fun copiesSurviveOfflineDraftAndQueueRoundTripIncludingDuplicates() {
        val game = Game(id = 5, title = "Test").withCopies(copies + copies.last())
        val local = OfflineCollection("owner", "Tester", games = listOf(game))
            .enqueue(game.copy(status = "Gespielt"), deleting = false)
        val restored = OfflineCollection.decode(local.encode())
        assertEquals(local, restored)
        assertEquals(game.ownedCopies, restored.visible().single().ownedCopies)
        assertEquals(game, EditorState.restore(EditorState(game = game).encode()).game)
        val payload = game.payload()
        assertFalse(payload.has("owned_platform")); assertFalse(payload.has("format"))
        assertEquals(3, payload.getJSONArray("copies").length())
        val empty = game.withCopies(emptyList())
        assertEquals(empty, Game.fromJson(empty.savedJson()))
    }

    @Test fun attemptedLegacyWriteKeepsPayloadAndKeyAcrossUpgrade() {
        val game = Game(id = 5, title = "Legacy", platforms = listOf("Steam", "Switch"), format = "Physisch")
        val queue = QueuedWrite(PendingGameWrite(game), 5, attempted = true)
        val old = OfflineCollection("owner", "Tester", games = listOf(game), queue = listOf(queue))
        val restored = OfflineCollection.decode(old.encode())
        val write = restored.queue.single().write
        assertEquals(queue.write.key, write.key)
        assertEquals(game.payload().toString(), write.game.payload().toString())
        assertFalse(write.game.payload().has("copies"))
        assertEquals(2, write.game.ownedCopies.size)
        assertTrue(restored.queue.single().attempted)
    }

    @Test fun serverCopiesOverrideTheLegacyProjectionAndStatisticsCountCopies() {
        val game = Game(id = 3, title = "Test", rating = 8).withCopies(copies + copies.last())
        val json = game.savedJson().put("owned_platform", "Wrong").put("format", "Wrong")
        val restored = Game.fromJson(JSONObject(json.toString()))
        assertEquals(game, restored)
        val summary = CollectionSummary.from(listOf(restored))
        assertEquals(1, summary.count); assertEquals(2, summary.platformCount)
        assertEquals(8.0, summary.averageRating)
        assertEquals(3, summary.distributions.first { it.title == "Format · Exemplare" }.total)
    }
    @Test fun newCopiesRequireCapabilityButLegacyRetriesKeepTheirBody() = kotlinx.coroutines.runBlocking {
        okhttp3.mockwebserver.MockWebServer().use { server ->
            val cookies = object : CookiePersistence {
                override fun read() = emptyList<String>()
                override fun write(cookies: List<String>) = Unit
            }
            server.enqueue(okhttp3.mockwebserver.MockResponse().setHeader("Content-Type", "application/json")
                .setBody("""{"api_version":1,"csrf_token":"token","capabilities":{"idempotent_game_writes":true}}"""))
            val api = ApiClient.create(server.url("/").toString(), cookies)
            api.session()
            try {
                api.save(Game(title = "New").withCopies(copies), java.util.UUID.randomUUID().toString())
                fail("An older server must not receive copies")
            } catch (error: ApiException) { assertEquals(426, error.status) }
            assertEquals(1, server.requestCount)
            server.enqueue(okhttp3.mockwebserver.MockResponse().setHeader("Content-Type", "application/json").setBody("{}"))
            val legacy = Game(id = 5, title = "Legacy", platforms = listOf("Steam"))
            val key = java.util.UUID.randomUUID().toString()
            api.save(legacy, key)
            server.takeRequest()
            val request = server.takeRequest()
            assertEquals(key, request.getHeader("Idempotency-Key"))
            assertEquals(legacy.payload().toString(), request.body.readUtf8())
        }
    }
}
