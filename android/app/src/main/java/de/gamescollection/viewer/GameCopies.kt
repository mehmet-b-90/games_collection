package de.gamescollection.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable internal fun GameCopiesList(copies: List<GameCopy>, enabled: Boolean,
    add: () -> Unit, edit: (Int) -> Unit, remove: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Mein Besitz", style = MaterialTheme.typography.titleMedium)
            Text("${copies.size} ${if (copies.size == 1) "Exemplar" else "Exemplare"}", style = MaterialTheme.typography.bodySmall)
        }
        if (copies.isEmpty()) Text("Noch keine Exemplare erfasst.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                copies.forEachIndexed { index, copy ->
                    if (index > 0) HorizontalDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (copy.format == "Digital") NativeIcons.Download else NativeIcons.Disc,
                            contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(copy.platform, style = MaterialTheme.typography.titleSmall)
                            Text("${copy.format} · ${copy.edition.ifEmpty { "Ohne Ausgabenangabe" }}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { edit(index) }, enabled = enabled) { Icon(Icons.Default.Edit, "${copy.platform} ${copy.format} bearbeiten") }
                        IconButton(onClick = { remove(index) }, enabled = enabled) { Icon(Icons.Default.Delete, "${copy.platform} ${copy.format} entfernen") }
                    }
                }
            }
        }
        OutlinedButton(onClick = add, enabled = enabled && copies.size < 100, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Exemplar hinzufügen")
        }
        Text("Status und Bewertung gelten gemeinsam für dieses Spiel.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable internal fun GameCopyEditor(title: String, original: GameCopy?, platforms: List<String>,
    defaultFormat: String, enabled: Boolean, editingGame: Boolean, dismissible: Boolean,
    close: () -> Unit, save: (GameCopy) -> Unit) {
    var platform by rememberSaveable { mutableStateOf(original?.platform ?: platforms.firstOrNull().orEmpty()) }
    var format by rememberSaveable { mutableStateOf(original?.format ?: defaultFormat) }
    var edition by rememberSaveable { mutableStateOf(original?.edition.orEmpty()) }
    FullScreenDialog(if (original == null) "Exemplar hinzufügen" else "Exemplar bearbeiten", dismissible, close) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            ChoiceField("Plattform", platform, platforms, enabled = enabled) { platform = it }
            ChoiceField("Format", format, listOf("Digital", "Physisch"), enabled = enabled) { format = it }
            FormTextField(edition, { edition = it.take(80) }, "Ausgabe (optional)", Modifier.fillMaxWidth(),
                placeholder = "z. B. Collector’s Edition", enabled = enabled)
            if (editingGame) Text("Das Exemplar wird mit dem Spiel gespeichert.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { save(GameCopy(platform, format, edition.trim())) }, enabled = enabled && platform in platforms,
                modifier = Modifier.fillMaxWidth()) { Text(if (editingGame) "Übernehmen" else "Exemplar speichern") }
            TextButton(onClick = close, enabled = dismissible, modifier = Modifier.fillMaxWidth()) { Text("Abbrechen") }
        }
    }
}
