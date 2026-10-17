package de.gamescollection.viewer

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import javax.net.ssl.SSLException

data class ViewerState(
    val server: String = "", val username: String? = null, val busy: Boolean = false,
    val games: List<Game> = emptyList(), val platforms: List<String> = emptyList(),
    val theme: String = "cinematic", val coverSize: Int = 150,
    val error: String? = null, val connected: Boolean = false,
    val summary: CollectionSummary = CollectionSummary()
)

class CollectionViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("viewer", Context.MODE_PRIVATE)
    private val persistence = SessionStore(application)
    private var api: ApiClient? = null
    private var owner: String? = null
    private var filterJob: Job? = null
    var state by mutableStateOf(ViewerState(server = preferences.getString("server", "").orEmpty(),
        theme = preferences.getString("native_theme", "cinematic").orEmpty(),
        coverSize = preferences.getInt("cover_size", 150).coerceIn(100, 240)))
        private set
    var searchResults by mutableStateOf<List<Game>>(emptyList())
        private set
    var editor by mutableStateOf(EditorState())
        private set
    var filter by mutableStateOf(CollectionFilter())
        private set
    var sort by mutableStateOf(GameSort.NEWEST)
        private set
    var visibleGames by mutableStateOf<List<Game>>(emptyList())
        private set

    init { if (state.server.isNotEmpty()) connect() }

    private fun action(writing: Boolean = false, block: suspend () -> Unit) {
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
                    resetAccount(preserveDraft = true)
                    state = state.copy(username = null, games = emptyList(), summary = CollectionSummary(),
                        error = "Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.")
                } else {
                    // A definite rejection did not commit. Transport/proxy failures remain ambiguous.
                    if (writing && error is ApiException && error.status in 400..499 && error.status != 408) {
                        updateEditor(editor.copy(pending = null))
                    }
                    state = state.copy(error = if (editor.pending != null)
                        "$message Der Ausgang der Änderung ist unklar. Nutze im Spieldialog ‚Vorgang erneut prüfen‘." else message)
                }
            } finally { state = state.copy(busy = false) }
        }
    }

    fun connect(address: String = state.server, allowHttp: Boolean = address.startsWith("http://")) {
        val normalized = try { ServerAddress.normalize(address, allowHttp) }
        catch (error: IllegalArgumentException) { state = state.copy(error = error.message); return }
        action {
            val changed = normalized != state.server
            if (changed || api == null) {
                if (changed) {
                    resetAccount()
                    state = ViewerState(server = normalized, busy = true, theme = state.theme, coverSize = state.coverSize)
                }
                val previous = api
                api = null
                api = withContext(Dispatchers.IO) {
                    if (changed) { previous?.clearSession(); persistence.write(emptyList()) }
                    ApiClient.create(normalized, persistence)
                }
                preferences.edit { putString("server", normalized) }
            }
            val session = withContext(Dispatchers.IO) { api!!.session() }
            applySession(session)
            if (state.username != null) loadCollection()
        }
    }

    private fun applySession(session: JSONObject) {
        val user = session.optJSONObject("user")
        val nextOwner = user?.let { "${state.server}|${it.getInt("id")}" }
        if (owner != nextOwner) {
            filterJob?.cancel(); filter = CollectionFilter(); sort = GameSort.NEWEST; visibleGames = emptyList()
            editor = EditorState.restoreForOwner(savedState["editor"], savedState["editor_owner"], nextOwner)
            owner = nextOwner
            // Keep a restored draft while anonymous; release it only after a verified account change or logout.
            if (nextOwner != null) updateEditor(editor)
        }
        state = state.copy(connected = true, username = user?.text("username"))
    }

    private fun resetAccount(preserveDraft: Boolean = false) {
        filterJob?.cancel(); owner = null; editor = EditorState(); searchResults = emptyList()
        filter = CollectionFilter(); sort = GameSort.NEWEST; visibleGames = emptyList()
        if (!preserveDraft) { savedState.remove<String>("editor"); savedState.remove<String>("editor_owner") }
    }

    private suspend fun loadCollection() {
        val result = withContext(Dispatchers.IO) { api!!.collection() }
        val (games, platforms, summary) = withContext(Dispatchers.Default) {
            val context = currentCoroutineContext()
            val games = result.getJSONArray("games").objects().mapIndexed { index, json ->
                if (index % 128 == 0) context.ensureActive()
                Game.fromJson(json)
            }
            Triple(games, result.getJSONArray("platforms").strings(), CollectionSummary.from(games) { context.ensureActive() })
        }
        state = state.copy(games = games, platforms = platforms, summary = summary)
        calculateVisible()
    }

    fun changeFilter(value: CollectionFilter) {
        val debounce = value.query != filter.query
        filter = value
        calculateVisible(debounce)
    }
    fun changeSort(value: GameSort) { sort = value; calculateVisible() }
    private fun calculateVisible(debounce: Boolean = false) {
        filterJob?.cancel()
        val games = state.games
        val selectedFilter = filter
        val selectedSort = sort
        filterJob = viewModelScope.launch {
            if (debounce) delay(120)
            visibleGames = withContext(Dispatchers.Default) {
                val context = currentCoroutineContext()
                games.filtered(selectedFilter, selectedSort) { context.ensureActive() }
            }
        }
    }

    private fun updateEditor(value: EditorState) {
        editor = value
        owner?.let { savedState["editor_owner"] = it; savedState["editor"] = value.encode() }
    }
    fun showSearch() {
        if (editor.pending != null) { updateEditor(editor.copy(open = true)); return }
        clearSearch(); updateEditor(EditorState(adding = true, open = true))
    }
    fun showGame(game: Game, editing: Boolean = false) {
        if (editor.pending != null) { updateEditor(editor.copy(open = true)); return }
        updateEditor(EditorState(game = game, editing = editing, open = true))
    }
    fun startEditing() { updateEditor(editor.copy(editing = true)) }
    fun changeDraft(game: Game) { if (!state.busy && editor.pending == null) updateEditor(editor.copy(game = game)) }
    fun dismissEditor() {
        if (!state.busy) updateEditor(if (editor.pending != null) editor.copy(open = false) else EditorState())
    }

    fun login(username: String, password: String, remember: Boolean) = action {
        val result = withContext(Dispatchers.IO) { api!!.login(username.trim(), password, remember) }
        applySession(result)
        loadCollection()
    }
    fun refresh() = action { loadCollection() }
    fun logout() = action {
        try { withContext(Dispatchers.IO) { api?.logout() } }
        finally { resetAccount(); state = state.copy(username = null, games = emptyList(), summary = CollectionSummary()) }
    }
    fun save(game: Game) { write(PendingGameWrite(game)) }
    fun delete(game: Game) { write(PendingGameWrite(game, deleting = true)) }
    fun retryWrite() { editor.pending?.let(::write) }
    private fun write(proposed: PendingGameWrite) = action(writing = true) {
        val pending = editor.pending ?: proposed
        updateEditor(editor.copy(pending = pending))
        withContext(Dispatchers.IO) {
            if (pending.deleting) api!!.delete(pending.game, pending.key) else api!!.save(pending.game, pending.key)
        }
        updateEditor(EditorState()) // The write succeeded; a failed reload must not offer to submit it again.
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
    fun setCoverSize(value: Int) {
        val size = value.coerceIn(100, 240)
        preferences.edit { putInt("cover_size", size) }
        state = state.copy(coverSize = size)
    }
    fun setTheme(value: String) {
        if (value !in setOf("cinematic", "editorial")) return
        preferences.edit { putString("native_theme", value) }
        state = state.copy(theme = value)
    }
}
