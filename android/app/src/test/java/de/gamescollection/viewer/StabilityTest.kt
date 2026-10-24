package de.gamescollection.viewer

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StabilityTest {
    private class MemoryCookies : CookiePersistence {
        override fun read() = emptyList<String>()
        override fun write(cookies: List<String>) {}
    }
    private suspend fun MockWebServer.api() = ApiClient.create(url("/").newBuilder().host("127.0.0.1").build().toString(), MemoryCookies())
    private fun session(supported: Boolean = true) = MockResponse().setHeader("Content-Type", "application/json")
        .setBody("""{"api_version":1,"csrf_token":"token","capabilities":{"idempotent_game_writes":$supported}}""")

    @Test fun draftAndPendingWriteSurviveSerializationWithSameOperationKey() {
        val game = Game(id = 12, version = 7, title = "Entwurf", release = "2025",
            platforms = listOf("Steam", "Nintendo Switch"), rating = 0)
        val editor = EditorState(game, editing = true, open = true, pending = PendingGameWrite(game))
        assertEquals(editor, EditorState.restore(editor.encode()))
        val manual = editor.copy(game = game.copy(id = 0), pending = PendingGameWrite(game.copy(id = 0)))
        assertEquals(manual, EditorState.restore(manual.encode()))
        assertEquals(EditorState(), EditorState.restore("broken"))
    }

    @Test fun draftRestorationNeverCrossesServerOrAccount() {
        val editor = EditorState(Game(title = "Privater Entwurf"), editing = true, open = true)
        val savedOwner = "https://one.example/|1"
        assertEquals(editor, EditorState.restoreForOwner(editor.encode(), savedOwner, savedOwner))
        assertEquals(EditorState(), EditorState.restoreForOwner(editor.encode(), savedOwner, null))
        assertEquals(EditorState(), EditorState.restoreForOwner(editor.encode(), savedOwner, "https://one.example/|2"))
        assertEquals(EditorState(), EditorState.restoreForOwner(editor.encode(), savedOwner, "https://two.example/|1"))
    }

    @Test fun apiFactoryAlwaysLoadsCookiePersistenceOffCallerThread() = runBlocking {
        val caller = Thread.currentThread()
        var reader: Thread? = null
        val persistence = object : CookiePersistence {
            override fun read(): List<String> { reader = Thread.currentThread(); return emptyList() }
            override fun write(cookies: List<String>) {}
        }
        ApiClient.create("https://games.example.org/", persistence)
        assertNotNull(reader)
        assertNotSame(caller, reader)
    }

    @Test fun cancellationClosesCallBeforeDelayedHeadersAndBody() = runBlocking {
        for (delayHeaders in listOf(true, false)) {
            MockWebServer().use { server ->
                val response = session()
                if (delayHeaders) response.setHeadersDelay(3, TimeUnit.SECONDS)
                else response.setBodyDelay(3, TimeUnit.SECONDS)
                server.enqueue(response)
                val call = OkHttpClient().newCall(Request.Builder()
                    .url(server.url("/").newBuilder().host("127.0.0.1").build()).build())
                val job = launch(Dispatchers.IO) { call.readBody() }
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
                withTimeout(1000) { job.cancelAndJoin() }
                assertTrue(job.isCancelled)
                assertTrue("Underlying HTTP call must be cancelled too", call.isCanceled())
            }
        }
    }

    @Test fun lostSaveResponseCanBeRetriedWithExactlySameKeyAndPayload() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(session())
            val api = server.api()
            api.session()
            server.takeRequest()
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
            server.enqueue(MockResponse().setResponseCode(201).setHeader("Content-Type", "application/json")
                .setBody("""{"status":"success"}"""))
            val pending = PendingGameWrite(Game(title = "Manuell", platforms = listOf("Steam")))
            try { api.save(pending.game, pending.key); fail("Expected lost response") } catch (_: IOException) {}
            assertEquals("Writes must wait for explicit reconciliation", 2, server.requestCount)
            val restored = PendingGameWrite.restore(JSONObject(pending.json().toString()))
            api.save(restored.game, restored.key)
            val first = server.takeRequest(3, TimeUnit.SECONDS)!!
            val retry = server.takeRequest(3, TimeUnit.SECONDS)!!
            assertEquals(pending.key, first.getHeader("Idempotency-Key"))
            assertEquals(first.getHeader("Idempotency-Key"), retry.getHeader("Idempotency-Key"))
            assertEquals(first.body.readUtf8(), retry.body.readUtf8())
        }
    }

    @Test fun sessionRecoversFromTemporaryConnectionFailure() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
            server.enqueue(session())
            assertEquals("token", server.api().session().getString("csrf_token"))
            assertEquals(2, server.requestCount)
            repeat(2) { assertEquals("/api/session", server.takeRequest().path) }
        }
    }

    @Test fun collectionRecoversFromTruncatedResponseBody() = runBlocking {
        MockWebServer().use { server ->
            val body = """{"games":[],"platforms":["Steam"]}"""
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
                .setBody(body).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body))
            assertEquals("Steam", server.api().collection().getJSONArray("platforms").getString(0))
            assertEquals(2, server.requestCount)
        }
    }

    @Test fun persistentConnectionFailureStopsAfterThreeAttempts() = runBlocking {
        MockWebServer().use { server ->
            repeat(3) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)) }
            try { server.api().session(); fail("Expected connection failure") } catch (_: IOException) {}
            assertEquals(3, server.requestCount)
        }
    }

    @Test fun serverErrorsAreNotRetriedAsConnectionFailures() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503).setHeader("Content-Type", "application/json")
                .setBody("""{"error":"Wartung"}"""))
            try { server.api().session(); fail("Expected API error") }
            catch (error: ApiException) { assertEquals(503, error.status) }
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun cancellationDuringRetryDelayDoesNotSendAnotherRequest() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
            val api = server.api()
            val job = launch { api.session() }
            // Yield to the request and its failure; cancellation must also interrupt retry backoff.
            kotlinx.coroutines.delay(200)
            assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
            withTimeout(1000) { job.cancelAndJoin() }
            assertTrue(job.isCancelled)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun csrfRetryPreservesOperationKey() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(session())
            server.enqueue(MockResponse().setResponseCode(400).setHeader("Content-Type", "application/json")
                .setHeader("X-CSRF-Error", "1").setBody("""{"error":"CSRF"}"""))
            server.enqueue(session())
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("{}"))
            val api = server.api()
            api.session()
            val write = PendingGameWrite(Game(title = "Test"))
            api.save(write.game, write.key)
            server.takeRequest()
            val first = server.takeRequest()
            server.takeRequest()
            val retry = server.takeRequest()
            assertEquals(write.key, first.getHeader("Idempotency-Key"))
            assertEquals(write.key, retry.getHeader("Idempotency-Key"))
        }
    }

    @Test fun oldServerCannotSilentlyAcceptUnsafeGameWrite() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(session(false))
            try { server.api().save(Game(title = "Test"), PendingGameWrite(Game(title = "Test")).key); fail("Expected server upgrade error") }
            catch (error: ApiException) { assertEquals(426, error.status) }
            assertEquals(1, server.requestCount)
            assertEquals("GET", server.takeRequest().method)
        }
    }

    @Test fun aggregatesAndCancellableFilteringHandleLargeCollections() {
        val games = (1..10000).map { Game(id = it, title = "Spiel $it", engine = "Engine ${it % 500}, Shared",
            ageRating = "USK 12", platforms = listOf("Steam"), rating = if (it % 2 == 0) 8 else null) }
        val summary = CollectionSummary.from(games)
        assertEquals(10000, summary.count)
        assertEquals(8.0, summary.averageRating!!, 0.0)
        assertEquals(501, summary.engines.size)
        assertEquals(500, summary.distributions.last().rows.size)
        assertEquals(10000, summary.distributions.last().total)
        assertEquals(listOf("USK 12"), summary.ageRatings)
        var calls = 0
        val visible = games.filtered(CollectionFilter(minimumRating = 8), GameSort.NEWEST) { calls++ }
        assertEquals(5000, visible.size)
        assertEquals(10000, visible.first().id)
        assertTrue(calls > 1)
        assertThrows(kotlinx.coroutines.CancellationException::class.java) {
            games.filtered(CollectionFilter(), GameSort.TITLE) { throw kotlinx.coroutines.CancellationException() }
        }
    }
}
