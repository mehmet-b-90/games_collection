package de.gamescollection.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PopularGame(val game: Game, val rank: Int, val genre: String, val igdbRating: Double?) {
    companion object {
        fun fromJson(json: JSONObject): PopularGame {
            val rating = if (json.isNull("rating")) null else json.getDouble("rating")
            return PopularGame(Game.fromIgdb(json), json.getInt("popularity_rank"),
                json.optJSONArray("genres").objects().joinToString(" · ") { it.text("name") },
                rating?.takeIf { it.isFinite() && it in 0.0..100.0 }?.div(10.0))
        }
    }
}

data class DiscoveryFeed(val games: List<PopularGame>, val stale: Boolean, val updatedAt: String) {
    companion object {
        fun fromJson(json: JSONObject) = DiscoveryFeed(
            json.getJSONArray("games").objects().map(PopularGame::fromJson),
            json.getBoolean("stale"), json.getString("updated_at"))
    }
}

data class DiscoveryState(val feed: DiscoveryFeed? = null, val loading: Boolean = false, val error: String? = null)

@Composable
fun DiscoveryScreen(discovery: DiscoveryState, state: ViewerState, retry: () -> Unit, add: (Game) -> Unit) {
    val owned = remember(state.games) { state.games.mapNotNull { it.igdbId }.toSet() }
    val feed = discovery.feed
    val stamp = remember(feed?.updatedAt) {
        runCatching { Instant.parse(feed?.updatedAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) }.getOrDefault("")
    }
    LazyVerticalGrid(columns = GridCells.Fixed(state.postersPerRow), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aktuell beliebt", style = MaterialTheme.typography.headlineSmall)
                Text("Die meistbesuchten Spiele auf IGDB · täglich aktualisiert", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (stamp.isNotEmpty()) Text("Stand: $stamp", style = MaterialTheme.typography.labelSmall)
                if (discovery.loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Beliebte Spiele werden geladen …", style = MaterialTheme.typography.bodySmall)
                }
                if (feed?.stale == true) Text("Aktualisierung derzeit nicht möglich. Gespeicherte Rangliste.", style = MaterialTheme.typography.bodySmall)
                discovery.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    if (feed != null) Text("Die zuletzt geladene Liste bleibt sichtbar.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = retry, enabled = !state.busy && !discovery.loading) { Text("Erneut versuchen") }
                }
                if (feed != null && feed.games.isEmpty() && !discovery.loading) Text("Zurzeit sind keine beliebten Titel verfügbar.")
            }
        }
        items(feed?.games.orEmpty(), key = { it.game.igdbId!! }) { popular ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(.75f).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(NativeIcons.Gamepad, "Cover", Modifier.align(Alignment.Center).size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (popular.game.cover.isNotEmpty()) AsyncImage(popular.game.cover, "Cover von ${popular.game.title}",
                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Surface(Modifier.padding(8.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                        Text("#${popular.rank}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(popular.game.title, style = MaterialTheme.typography.titleSmall)
                Text(popular.genre.ifEmpty { "Genre nicht angegeben" }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(popular.igdbRating?.let { "IGDB ★ ${String.format(Locale.GERMAN, "%.1f", it)} / 10" }
                    ?: "Noch keine IGDB-Bewertung", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (popular.game.igdbId in owned) Text("✓ In Sammlung", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 12.dp))
                else OutlinedButton(onClick = { add(popular.game) }, enabled = !state.busy,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("+ Hinzufügen", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Powered by IGDB PopScore", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
