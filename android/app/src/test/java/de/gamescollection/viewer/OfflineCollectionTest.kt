package de.gamescollection.viewer

import org.junit.Assert.*
import org.junit.Test

class OfflineCollectionTest {
    private val game = Game(id = 12, version = 3, title = "Original", platforms = listOf("PC"))
    private val collection = OfflineCollection("https://server/|7", "User", listOf(game), listOf("PC"))

    @Test fun offlineChangesSurviveSerializationWithStableKeysAndVersion() {
        val queued = collection.enqueue(game.copy(title = "Geändert"), false)
            .enqueue(Game(title = "Neu", platforms = listOf("PC")), false)
        val restored = OfflineCollection.decode(queued.encode())
        assertEquals(queued, restored)
        assertEquals(listOf("Geändert", "Neu"), restored.visible().map { it.title })
        assertEquals(3, restored.queue.first().write.game.version)
        assertEquals(0, restored.queue.last().write.game.id)
        assertTrue(restored.visible().last().id < 0)
    }

    @Test fun unsentCreationCanBeEditedAndDeletedWithoutServerOperation() {
        val created = collection.enqueue(Game(title = "Neu"), false)
        val local = created.visible().last()
        val edited = created.enqueue(local.copy(title = "Korrigiert"), false)
        assertEquals(1, edited.queue.size)
        assertEquals("Korrigiert", edited.queue.single().write.game.title)
        assertEquals(0, edited.queue.single().write.game.id)
        val deleted = edited.enqueue(local, true)
        assertTrue(deleted.queue.isEmpty())
        assertEquals(listOf(game), deleted.visible())
    }

    @Test fun deleteHidesTitleAndDiscardRestoresSnapshot() {
        val deleted = collection.enqueue(game, true)
        assertTrue(deleted.visible().isEmpty())
        assertEquals(listOf(game), deleted.copy(queue = emptyList()).visible())
    }

    @Test fun attemptedRequestCannotBeMutatedAfterResponseLoss() {
        val queued = collection.enqueue(game.copy(title = "Geändert"), false)
        val sent = queued.copy(queue = queued.queue.map { it.copy(attempted = true) })
        assertThrows(IllegalStateException::class.java) { sent.enqueue(game.copy(title = "Andere Änderung"), false) }
        assertThrows(IllegalStateException::class.java) { sent.enqueue(game, true) }
        assertEquals(sent, OfflineCollection.decode(sent.encode()))
    }

    @Test fun multipleNewTitlesHaveDistinctLocalIdsAndRejectionsSurviveRestart() {
        val queued = collection.enqueue(Game(title = "A"), false).enqueue(Game(title = "B"), false)
        assertEquals(2, queued.queue.map { it.localId }.distinct().size)
        val rejected = queued.copy(queue = queued.queue.map { it.copy(attempted = true, rejection = "Konflikt") })
        assertEquals(rejected, OfflineCollection.decode(rejected.encode()))
        assertEquals(collection.owner, rejected.owner)
    }
}
