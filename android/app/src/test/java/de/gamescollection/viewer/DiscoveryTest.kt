package de.gamescollection.viewer

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DiscoveryTest {
    @Test fun popularityAndIgdbRatingNeverBecomePersonalRatingOrOwnership() {
        val entry = PopularGame.fromJson(JSONObject("""{
            "id":42,"name":"Popular game","popularity_rank":3,"rating":84,
            "genres":[{"name":"Adventure"}],"platforms":[{"name":"PC"}],
            "cover":{"image_id":"co123"},"mapped_age_rating":"USK 12"
        }"""))
        assertEquals(3, entry.rank)
        assertEquals(8.4, entry.igdbRating!!, .001)
        assertNull(entry.game.rating)
        assertTrue(entry.game.platforms.isEmpty())
        assertEquals("PC", entry.game.available)
        assertEquals("USK 12", entry.game.ageRating)
        assertEquals(0, entry.game.id)
        assertEquals(42, entry.game.igdbId)
        assertTrue(entry.game.payload().isNull("rating"))
    }

    @Test fun missingOptionalDetailsAndStaleEmptyFeedAreSupported() {
        val entry = PopularGame.fromJson(JSONObject("""{"id":1,"name":"No details","popularity_rank":1}"""))
        assertNull(entry.igdbRating)
        assertEquals("", entry.genre)
        val feed = DiscoveryFeed.fromJson(JSONObject("""{"games":[],"stale":true,"updated_at":"2026-10-24T12:00:00+00:00"}"""))
        assertTrue(feed.games.isEmpty())
        assertTrue(feed.stale)
    }
}
