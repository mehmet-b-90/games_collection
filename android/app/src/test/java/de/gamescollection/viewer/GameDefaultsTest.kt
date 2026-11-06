package de.gamescollection.viewer

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GameDefaultsTest {
    @Test fun newGameGetsDefaultsWithoutLosingMetadata() {
        val defaults = GameDefaults(IgdbGameType.MAIN_GAME, "Physisch", "Gespielt", listOf("Steam", "Obsolete"))
        val game = Game(title = "Titel", igdbId = 123, available = "PC", cover = "cover")
        val result = defaults.applyTo(game, listOf("Steam"))
        assertEquals(game.copy(format = "Physisch", status = "Gespielt", platforms = listOf("Steam")), result)
        assertNull(result.rating)
    }

    @Test fun savedAndOfflineGamesAreNotOverwritten() {
        val defaults = GameDefaults(format = "Physisch", status = "Gespielt", platforms = listOf("Steam"))
        for (id in listOf(10, -1)) {
            val game = Game(id = id, title = "Bearbeitet", platforms = listOf("Switch"))
            assertEquals(game, defaults.applyTo(game, listOf("Steam", "Switch")))
        }
    }

    @Test fun accountCacheRoundTripsAllTypesAndEmptyPlatforms() {
        for (type in IgdbGameType.entries) {
            val defaults = GameDefaults(gameType = type)
            assertEquals(defaults, GameDefaults.fromJson(JSONObject(defaults.json().toString())))
        }
        assertEquals(GameDefaults(), GameDefaults.fromJson(null))
    }
}
