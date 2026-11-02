package de.gamescollection.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.json.JSONObject

data class Friend(val id: Int, val username: String, val status: String, val incoming: Boolean) {
    companion object {
        fun fromJson(json: JSONObject) = Friend(json.getInt("id"), json.getString("username"),
            json.getString("status"), json.getBoolean("incoming"))
    }
}

data class FriendLibrary(val username: String, val games: List<Game>) {
    companion object {
        fun fromJson(json: JSONObject) = FriendLibrary(json.getJSONObject("user").getString("username"),
            json.getJSONArray("games").objects().map(Game::fromJson))
    }
}

data class FriendsState(val friends: List<Friend> = emptyList(), val loading: Boolean = false,
    val loaded: Boolean = false, val error: String? = null, val message: String? = null,
    val library: FriendLibrary? = null)

@Composable
fun FriendsScreen(state: FriendsState, refresh: () -> Unit, invite: (String) -> Unit,
    accept: (Int) -> Unit, remove: (Int) -> Unit, view: (Int) -> Unit, closeLibrary: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable(state.library?.username) { mutableStateOf("") }
    var removing by remember { mutableStateOf<Friend?>(null) }
    val library = state.library
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nach Annahme einer Anfrage könnt ihr gegenseitig eure gesamte gespeicherte Sammlung ansehen.")
                OutlinedButton(onClick = refresh, enabled = !state.loading) { Text("Aktualisieren") }
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
        if (library == null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormTextField(username, { if (it.length <= 150) username = it }, "Genauer Benutzername", Modifier.fillMaxWidth(), enabled = !state.loading)
                    Button(onClick = { invite(username) }, enabled = !state.loading && username.trim().length in 3..150) { Text("Anfrage senden") }
                }
            }
            listOf("Freunde" to state.friends.filter { it.status == "accepted" },
                "Erhaltene Anfragen" to state.friends.filter { it.status == "pending" && it.incoming },
                "Gesendete Anfragen" to state.friends.filter { it.status == "pending" && !it.incoming }).forEach { (heading, entries) ->
                item { Text(heading, style = MaterialTheme.typography.titleLarge) }
                if (entries.isEmpty() && state.loaded) item { Text("Noch keine Einträge.") }
                items(entries, key = { it.id }) { friend ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(friend.username, style = MaterialTheme.typography.titleMedium)
                            if (friend.status == "accepted") OutlinedButton(onClick = { view(friend.id) }, enabled = !state.loading) { Text("Sammlung ansehen") }
                            if (friend.status == "pending" && friend.incoming) Button(onClick = { accept(friend.id) }, enabled = !state.loading) { Text("Annehmen") }
                            TextButton(onClick = { if (friend.status == "accepted") removing = friend else remove(friend.id) }, enabled = !state.loading) {
                                Text(if (friend.status == "accepted") "Freundschaft beenden" else if (friend.incoming) "Ablehnen" else "Zurückziehen")
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sammlung von ${library.username}", style = MaterialTheme.typography.titleLarge)
                    Text("${library.games.size} Spiele · Nur ansehen", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = closeLibrary, enabled = !state.loading) { Text("Zurück zu Freunden") }
                    FormTextField(query, { query = it }, "Sammlung durchsuchen", Modifier.fillMaxWidth())
                }
            }
            val games = library.games.filter { it.title.contains(query.trim(), ignoreCase = true) }
            if (games.isEmpty()) item { Text("Keine Spiele gefunden.") }
            items(games, key = { it.id }) { game ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (game.cover.isNotEmpty()) AsyncImage(game.cover, "Cover von ${game.title}",
                            Modifier.width(76.dp).height(104.dp), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(game.title, style = MaterialTheme.typography.titleMedium)
                            Text("${game.release.ifEmpty { "Jahr unbekannt" }} · ${game.ageRating.ifEmpty { "Keine Altersfreigabe" }}", style = MaterialTheme.typography.bodySmall)
                            Text(game.platforms.joinToString(", ").ifEmpty { "Keine Besitzplattform" }, style = MaterialTheme.typography.bodySmall)
                            Text("${game.format} · ${game.status} · ★ ${game.rating ?: "–"}/10", style = MaterialTheme.typography.bodySmall)
                            var expanded by rememberSaveable(game.id) { mutableStateOf(false) }
                            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Weniger anzeigen" else "Weitere Spieldaten") }
                            if (expanded) listOf("Entwickler" to game.developer, "Publisher" to game.publisher,
                                "Engine" to game.engine, "Verfügbare Plattformen" to game.available).filter { it.second.isNotEmpty() }.forEach { (label, value) ->
                                Text("$label: $value", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
    removing?.let { friend ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("Freundschaft beenden?") },
            text = { Text("Die gegenseitige Freigabe der Sammlungen mit ${friend.username} endet damit.") },
            confirmButton = { TextButton(onClick = { removing = null; remove(friend.id) }) { Text("Beenden") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Abbrechen") } })
    }
}
