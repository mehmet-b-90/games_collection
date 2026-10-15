package de.gamescollection.viewer

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import javax.net.ssl.SSLException

data class ViewerState(
    val server: String = "", val username: String? = null, val busy: Boolean = false,
    val games: List<Game> = emptyList(), val platforms: List<String> = emptyList(),
    val theme: String = "cinematic", val error: String? = null, val connected: Boolean = false
)

class CollectionViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("viewer", Context.MODE_PRIVATE)
    private val persistence = SessionStore(application)
    private var api: ApiClient? = null
    var state by mutableStateOf(ViewerState(server = preferences.getString("server", "").orEmpty(),
        theme = preferences.getString("native_theme", "cinematic").orEmpty()))
        private set
    var searchResults by mutableStateOf<List<Game>>(emptyList())
        private set

    init {
        if (state.server.isNotEmpty()) {
            api = ApiClient(state.server, persistence)
            connect()
        }
    }

    private fun action(block: suspend () -> Unit) {
        if (state.busy) return
        state = state.copy(busy = true, error = null)
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                val message = when (error) {
                    is SSLException -> "Das HTTPS-Zertifikat wird nicht als vertrauenswürdig erkannt. Prüfe das Serverzertifikat."
                    is ApiException -> error.message.orEmpty()
                    is IOException -> "Verbindung fehlgeschlagen. Prüfe Serveradresse, WLAN oder VPN und versuche es erneut."
                    else -> "Die Serverantwort konnte nicht verarbeitet werden. Prüfe, ob Server und App aktuell sind."
                }
                if (error is ApiException && error.status == 401 && state.username != null) {
                    withContext(Dispatchers.IO) { api?.clearSession() }
                    state = state.copy(username = null, games = emptyList(), error = "Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.")
                } else state = state.copy(error = message)
            } finally { state = state.copy(busy = false) }
        }
    }

    fun connect(address: String = state.server, allowHttp: Boolean = address.startsWith("http://")) {
        val normalized = try { ServerAddress.normalize(address, allowHttp) }
        catch (error: IllegalArgumentException) { state = state.copy(error = error.message); return }
        action {
            if (normalized != state.server || api == null) {
                withContext(Dispatchers.IO) { api?.clearSession(); persistence.write(emptyList()) }
                api = ApiClient(normalized, persistence)
                preferences.edit { putString("server", normalized) }
                state = ViewerState(server = normalized, busy = true, theme = state.theme)
                searchResults = emptyList()
            }
            val session = withContext(Dispatchers.IO) { api!!.session() }
            applySession(session)
            if (state.username != null) loadCollection()
        }
    }

    private fun applySession(session: JSONObject) {
        state = state.copy(connected = true, username = session.optJSONObject("user")?.text("username"))
    }

    private suspend fun loadCollection() {
        val result = withContext(Dispatchers.IO) { api!!.collection() }
        val games = result.getJSONArray("games").objects().map(Game::fromJson)
        state = state.copy(games = games, platforms = result.getJSONArray("platforms").strings())
    }

    fun login(username: String, password: String, remember: Boolean) = action {
        val result = withContext(Dispatchers.IO) { api!!.login(username.trim(), password, remember) }
        applySession(result)
        loadCollection()
    }

    fun refresh() = action { loadCollection() }

    fun logout() = action {
        try { withContext(Dispatchers.IO) { api?.logout() } }
        finally { state = state.copy(username = null, games = emptyList()); searchResults = emptyList() }
    }

    fun save(game: Game, onSuccess: () -> Unit) = action {
        withContext(Dispatchers.IO) { api!!.save(game) }
        onSuccess() // The write succeeded; a failed reload must not offer to submit it again.
        loadCollection()
    }

    fun delete(game: Game, onSuccess: () -> Unit) = action {
        withContext(Dispatchers.IO) { api!!.delete(game) }
        onSuccess()
        loadCollection()
    }

    fun search(query: String, year: String) {
        if (query.isBlank() || (year.isNotEmpty() && (year.toIntOrNull() !in 1900..2100))) {
            state = state.copy(error = "Bitte gib einen Titel und optional ein gültiges Release-Jahr von 1900 bis 2100 ein.")
            return
        }
        action {
            searchResults = emptyList()
            searchResults = withContext(Dispatchers.IO) { api!!.search(query.trim(), year) }
        }
    }
    fun clearSearch() { searchResults = emptyList() }
    fun clearError() { state = state.copy(error = null) }
    fun setTheme(value: String) {
        if (value !in setOf("cinematic", "editorial")) return
        preferences.edit { putString("native_theme", value) }
        state = state.copy(theme = value)
    }
}
