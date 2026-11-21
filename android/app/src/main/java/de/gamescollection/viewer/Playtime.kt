package de.gamescollection.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

internal fun formatPlaytime(minutes: Long): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0L -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

data class IgdbDurations(val hastily: Int? = null, val normally: Int? = null, val completely: Int? = null) {
    fun json(): JSONObject = JSONObject().put("hastily", hastily).put("normally", normally).put("completely", completely)

    companion object {
        fun fromJson(json: JSONObject) = IgdbDurations(
            json.optInt("hastily").takeIf { it > 0 },
            json.optInt("normally").takeIf { it > 0 },
            json.optInt("completely").takeIf { it > 0 },
        )
    }
}

internal suspend fun loadIgdbPlaytime(client: ApiClient, gameId: Int): Result<IgdbDurations> =
    try {
        Result.success(client.igdbPlaytime(gameId))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

@Composable internal fun IgdbPlaytime(values: IgdbDurations?, loading: Boolean) {
    val durations = listOf(
        "Hauptgeschichte" to values?.hastily,
        "Mit Nebeninhalten" to values?.normally,
        "100 % Abschluss" to values?.completely,
    )
    val message = when {
        loading -> "IGDB-Spielzeiten werden geladen …"
        values == null -> "IGDB-Spielzeiten sind derzeit nicht verfügbar."
        durations.all { it.second == null } -> "Für diesen Titel sind bei IGDB keine Spielzeiten hinterlegt."
        else -> null
    }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Spielzeit laut IGDB", style = MaterialTheme.typography.titleSmall)
            if (message != null) Text(message, style = MaterialTheme.typography.bodySmall)
            durations.forEach { (label, seconds) ->
                if (seconds != null) {
                    val duration = if (seconds < 60) "< 1 min" else formatPlaytime((seconds.toLong() + 30) / 60)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(duration, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                    }
                }
            }
        }
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
