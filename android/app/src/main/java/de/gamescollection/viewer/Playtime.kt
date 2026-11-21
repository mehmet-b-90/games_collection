package de.gamescollection.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

internal fun formatPlaytime(minutes: Long): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0L -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

private val igdbDurations = listOf(
    "hastily" to "Hauptgeschichte", "normally" to "Mit Nebeninhalten", "completely" to "100 % Abschluss"
)

internal suspend fun loadIgdbPlaytime(client: ApiClient, gameId: Int): Result<JSONObject> =
    try {
        Result.success(client.igdbPlaytime(gameId))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

@Composable internal fun IgdbPlaytime(values: JSONObject?) {
    val durations = if (values == null) emptyList() else igdbDurations.mapNotNull { (field, label) ->
        val seconds = values.optLong(field)
        if (seconds <= 0) null else {
            val duration = if (seconds < 60) "< 1 min" else formatPlaytime(seconds / 60 + (seconds % 60 + 30) / 60)
            "$label · $duration"
        }
    }
    if (values != null && durations.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Spielzeit laut IGDB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (values == null) Text("IGDB-Spielzeiten sind derzeit nicht verfügbar.", style = MaterialTheme.typography.bodyMedium)
        durations.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable internal fun PersonalPlaytimeFields(
    hours: String, minutes: String, changeHours: (String) -> Unit, changeMinutes: (String) -> Unit,
    invalidMinutes: Boolean, enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Eigene Spielzeit (optional)", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val keyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
            FormTextField(hours, { if (it.length <= 6 && it.all { digit -> digit in '0'..'9' }) changeHours(it) },
                label = "Stunden", keyboardOptions = keyboard, enabled = enabled, modifier = Modifier.weight(1f))
            FormTextField(minutes, { if (it.length <= 2 && it.all { digit -> digit in '0'..'9' }) changeMinutes(it) },
                label = "Minuten", keyboardOptions = keyboard, enabled = enabled, modifier = Modifier.weight(1f))
        }
        if (invalidMinutes) Text("Minuten müssen zwischen 0 und 59 liegen.",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
