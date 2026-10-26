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
    val theme: String = "cinematic", val postersPerRow: Int = 2,
    val error: String? = null, val connected: Boolean = false,
    val igdbStatus: String = "IGDB-Zugang noch nicht eingerichtet. Einmal mit dem Server verbinden.",
    val queue: List<QueuedWrite> = emptyList(),
    val summary: CollectionSummary = CollectionSummary()
)

class CollectionViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("viewer", Context.MODE_PRIVATE)
    private val persistence = SessionStore(application)
    private val igdbStore = SessionStore(application, "igdb_access", strict = true)
    private val igdb = IgdbClient()
    private val offlineStore = OfflineStore(application)
    private var offline: OfflineCollection? = null
    private var api: ApiClient? = null
    private var owner: String? = null
    private var sessionVerified = false
    private var discoveryJob: Job? = null
    private var discoveryLoadedAt = 0L
    var discovery by mutableStateOf(DiscoveryState())
        private set
    private var filterJob: Job? = null
    var state by mutableStateOf(ViewerState(server = preferences.getString("server", "").orEmpty(),
        theme = preferences.getString("native_theme", "cinematic").orEmpty(),
        postersPerRow = preferences.getInt("posters_per_row", 2).coerceIn(1, 3)))
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

    init {
        state = state.copy(busy = true)
        viewModelScope.launch {
            try {
                val cachedOwner = preferences.getString("offline_owner", null)
                if (cachedOwner != null && cachedOwner.startsWith(state.server + "|")) {
                    offline = withContext(Dispatchers.IO) { offlineStore.read(cachedOwner) }
                    offline?.let { owner = it.owner; state = state.copy(username = it.username); publishOffline() }
                    cachedIgdb()?.let { showIgdbExpiry(it) }
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                state = state.copy(busy = false, error = "Der lokale Sammlungsspeicher konnte nicht gelesen werden. Er wurde nicht überschrieben.")
                return@launch
            }
            state = state.copy(busy = false)
            if (state.server.isNotEmpty()) connect()
        }
    }

    private fun action(block: suspend () -> Unit) {
        if (state.busy) return
        state = state.copy(busy = true, error = null)
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (error is IOException && error !is ApiException) state = state.copy(connected = false)
                val message = when (error) {
                    is SSLException -> "Das HTTPS-Zertifikat wird nicht als vertrauenswürdig erkannt. Prüfe das Serverzertifikat."
                    is ApiException -> error.message.orEmpty()
                    is IOException -> "Server nicht erreichbar. Lokal gespeicherte Änderungen bleiben erhalten. Synchronisiere später erneut."
                    is IllegalStateException -> error.message.orEmpty()
                    else -> "Die Serverantwort konnte nicht verarbeitet werden. Prüfe, ob Server und App aktuell sind."
                }
                if (error is ApiException && error.status == 401 && state.username != null) {
                    withContext(Dispatchers.IO) { api?.clearSession() }
                    resetAccount(preserveDraft = true)
                    state = state.copy(username = null, games = emptyList(), summary = CollectionSummary(),
                        error = "Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.")
                } else {
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
                    state = ViewerState(server = normalized, busy = true, theme = state.theme, postersPerRow = state.postersPerRow)
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
            if (state.username != null) synchronize()
        }
    }

    private suspend fun applySession(session: JSONObject) {
        val user = session.optJSONObject("user")
        val nextOwner = user?.let { "${state.server}|${it.getInt("id")}" }
        if (owner != nextOwner) {
            resetDiscovery()
            val nextOffline = if (nextOwner == null) null else withContext(Dispatchers.IO) { offlineStore.read(nextOwner) }
            searchResults = emptyList()
            filterJob?.cancel(); filter = CollectionFilter(); sort = GameSort.NEWEST; visibleGames = emptyList()
            editor = EditorState.restoreForOwner(savedState["editor"], savedState["editor_owner"], nextOwner)
            if (owner != null) withContext(Dispatchers.IO) { igdbStore.write(emptyList()) }
            owner = nextOwner
            offline = nextOffline
            state = state.copy(games = emptyList(), queue = emptyList(), summary = CollectionSummary())
            // Keep a restored draft while anonymous; release it only after a verified account change or logout.
            if (nextOwner != null) updateEditor(editor)
        }
        if (!sessionVerified && nextOwner != null) {
            editor = EditorState.restoreForOwner(savedState["editor"], savedState["editor_owner"], nextOwner)
        }
        sessionVerified = nextOwner != null
        state = state.copy(connected = true, username = user?.text("username"))
        if (nextOwner != null) {
            if (offline == null) offline = OfflineCollection(nextOwner, user!!.text("username"))
            preferences.edit { putString("offline_owner", nextOwner) }
            publishOffline()
        } else preferences.edit { remove("offline_owner") }
    }

    private suspend fun resetAccount(preserveDraft: Boolean = false) {
        resetDiscovery()
        withContext(Dispatchers.IO) { igdbStore.write(emptyList()) }
        state = state.copy(igdbStatus = "IGDB-Zugang noch nicht eingerichtet.")
        offline = null
        sessionVerified = false
        preferences.edit { remove("offline_owner") }
        state = state.copy(queue = emptyList())
        filterJob?.cancel(); owner = null; editor = EditorState(); searchResults = emptyList()
        filter = CollectionFilter(); sort = GameSort.NEWEST; visibleGames = emptyList()
        if (!preserveDraft) { savedState.remove<String>("editor"); savedState.remove<String>("editor_owner") }
    }

    private suspend fun persist(value: OfflineCollection) {
        try { withContext(Dispatchers.IO) { offlineStore.write(value) } }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { throw IllegalStateException("Die Änderung konnte nicht lokal gespeichert werden. Prüfe den freien Gerätespeicher und versuche es erneut.", error) }
        offline = value
        publishOffline()
    }

    private suspend fun publishOffline() {
        val value = offline ?: return
        val games = withContext(Dispatchers.Default) { value.visible() }
        val summary = withContext(Dispatchers.Default) { CollectionSummary.from(games) }
        state = state.copy(games = games, platforms = value.platforms, queue = value.queue, summary = summary)
        calculateVisible()
    }

    private suspend fun loadCollection(completedKey: String? = null) {
        val result = withContext(Dispatchers.IO) { api!!.collection() }
        val games = withContext(Dispatchers.Default) { result.getJSONArray("games").objects().map(Game::fromJson) }
        val value = offline ?: return
        persist(value.copy(games = games, platforms = result.getJSONArray("platforms").strings(),
            queue = value.queue.filterNot { it.write.key == completedKey }))
        state = state.copy(connected = true)
    }

    private suspend fun synchronize() {
        // Always verify the server account before sending any account-bound outbox entry.
        val expectedOwner = owner
        applySession(withContext(Dispatchers.IO) { api!!.session() })
        if (owner == null || (expectedOwner != null && owner != expectedOwner)) return
        val keys = offline?.queue.orEmpty().map { it.write.key }
        for (key in keys) {
            val value = offline ?: return
            val entry = value.queue.first { it.write.key == key }
            if (entry.rejection != null) continue
            persist(value.copy(queue = value.queue.map { if (it.write.key == key) it.copy(attempted = true) else it }))
            try {
                val write = entry.write
                if (write.deleting) api!!.delete(write.game, key) else api!!.save(write.game, key)
            } catch (error: ApiException) {
                if (error.status in listOf(400, 403, 404, 409, 422)) {
                    val current = offline!!
                    persist(current.copy(queue = current.queue.map {
                        if (it.write.key == key) it.copy(rejection = error.message) else it
                    }))
                    continue
                } else throw error
            }
            // A failed reload must never classify an already accepted write as rejected.
            loadCollection(completedKey = key)
        }
        loadCollection()
        prepareIgdb()
        if (state.queue.any { it.rejection != null }) state = state.copy(error =
            "Einige Änderungen wurden abgelehnt. Öffne die ausstehenden Änderungen, um die Meldung zu prüfen. Der lokale Stand bleibt erhalten.")
    }

    fun discardQueued(key: String) = action {
        val value = offline ?: return@action
        val entry = value.queue.firstOrNull { it.write.key == key } ?: return@action
        check(!entry.attempted || entry.rejection != null) { "Der Ausgang ist unklar. Bitte zuerst synchronisieren." }
        persist(value.copy(queue = value.queue.filterNot { it.write.key == key }))
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
        val queued = offline?.queue?.find { it.localId == game.id }
        if (queued?.attempted == true) {
            state = state.copy(error = queued.rejection ?: "Für diesen Titel ist eine Übertragung offen. Bitte zuerst synchronisieren; Details stehen unter den ausstehenden Änderungen.")
            return
        }
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
        synchronize()
    }
    fun refresh() = action { synchronize() }
    fun logout() = action {
        try { withContext(Dispatchers.IO) { api?.logout() } }
        finally { resetAccount(); state = state.copy(username = null, games = emptyList(), summary = CollectionSummary()) }
    }
    fun save(game: Game) { write(PendingGameWrite(game)) }
    fun delete(game: Game) { write(PendingGameWrite(game, deleting = true)) }
    fun retryWrite() { editor.pending?.let(::write) }
    private fun write(proposed: PendingGameWrite) = action {
        val value = offline ?: error("Bitte lade deine Sammlung zunächst einmal online.")
        val legacy = editor.pending
        val next = if (legacy != null) {
            val id = if (legacy.game.id == 0) value.nextId else legacy.game.id
            value.copy(queue = value.queue + QueuedWrite(legacy, id, attempted = true),
                nextId = if (legacy.game.id == 0) value.nextId - 1 else value.nextId)
        } else value.enqueue(proposed.game, proposed.deleting)
        persist(next)
        updateEditor(EditorState())
        if (state.connected) synchronize()
    }

    private suspend fun cachedIgdb(): IgdbCredentials? {
        val account = owner ?: return null
        return withContext(Dispatchers.IO) { IgdbCredentials.restore(igdbStore.read().firstOrNull(), account) }
    }

    private fun showIgdbExpiry(value: IgdbCredentials) {
        val date = java.time.Instant.ofEpochMilli(value.expiresAt).atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        state = state.copy(igdbStatus = if (value.valid()) "Direkte IGDB-Suche verfügbar bis $date. Internet erforderlich."
            else "IGDB-Zugang abgelaufen. Zum Erneuern mit dem Server verbinden.")
    }

    private suspend fun prepareIgdb(): IgdbCredentials? {
        val cached = cachedIgdb()
        // Renew near expiry while connected, but retain the existing token on provisioning failure.
        if (cached != null && cached.expiresAt - System.currentTimeMillis() > 86_400_000) {
            showIgdbExpiry(cached)
            return cached
        }
        return try {
            val account = owner ?: return null
            val requestedAt = System.currentTimeMillis()
            val json = api!!.igdbCredentials()
            val credentials = IgdbCredentials.fromServer(account, json, requestedAt)
            withContext(Dispatchers.IO) { igdbStore.write(listOf(credentials.encode())) }
            showIgdbExpiry(credentials)
            credentials
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            if (cached?.valid() == true) { showIgdbExpiry(cached); cached }
            else {
                state = state.copy(igdbStatus = if (error is IllegalStateException) error.message.orEmpty()
                    else "IGDB-Zugang nicht verfügbar. Server verbinden/aktualisieren und Twitch-Konfiguration prüfen.")
                null
            }
        }
    }

    fun search(query: String, year: String) {
        if (query.isBlank() || query.trim().length > 200 || (year.isNotEmpty() && (year.toIntOrNull() !in 1900..2100))) {
            state = state.copy(error = "Bitte gib einen Titel (maximal 200 Zeichen) und optional ein gültiges Release-Jahr von 1900 bis 2100 ein.")
            return
        }
        action {
            searchResults = emptyList()
            val cached = cachedIgdb()
            val credentials = if (cached?.valid() == true) cached else prepareIgdb()
            if (credentials == null) {
                state = state.copy(error = state.igdbStatus)
                return@action
            }
            showIgdbExpiry(credentials)
            try {
                searchResults = withContext(Dispatchers.IO) { igdb.search(credentials, query.trim(), year) }
            } catch (error: CancellationException) { throw error }
            catch (error: IgdbException) {
                if (error.expired) {
                    withContext(Dispatchers.IO) { igdbStore.write(emptyList()) }
                    state = state.copy(igdbStatus = error.message.orEmpty())
                }
                state = state.copy(error = error.message)
            } catch (_: IOException) {
                state = state.copy(error = "IGDB ist nicht erreichbar. Prüfe deine Internetverbindung. Dein Sammlungs-Login bleibt erhalten.")
            } catch (_: Exception) {
                state = state.copy(error = "Die IGDB-Suche konnte nicht verarbeitet werden. Prüfe Titel oder numerische IGDB-ID.")
            }
        }
    }
    private fun resetDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        discoveryLoadedAt = 0
        discovery = DiscoveryState()
    }

    fun loadDiscovery(force: Boolean = false) {
        if (state.busy || discovery.loading || owner == null) return
        if (!force && (discovery.error != null || System.currentTimeMillis() - discoveryLoadedAt < 86_400_000)) return
        val client = api
        if (client == null) {
            discovery = discovery.copy(error = "Bitte verbinde dich zuerst mit dem Server.")
            return
        }
        val requestedOwner = owner
        discovery = discovery.copy(loading = true, error = null)
        discoveryJob = viewModelScope.launch {
            try {
                val feed = withContext(Dispatchers.IO) { client.discover() }
                if (owner == requestedOwner && api === client) {
                    discoveryLoadedAt = System.currentTimeMillis()
                    discovery = DiscoveryState(feed = feed)
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (owner == requestedOwner && api === client) {
                    val message = when (error) {
                        is ApiException -> if (error.status == 401) "Deine Sitzung ist abgelaufen. Bitte verbinde dich erneut mit dem Server." else error.message.orEmpty()
                        is SSLException -> "Das Serverzertifikat konnte nicht geprüft werden."
                        is IOException -> "Aktuell beliebte Spiele sind nicht erreichbar. Bitte versuche es später erneut."
                        else -> "Die Rangliste konnte nicht verarbeitet werden. Bitte aktualisiere den Server."
                    }
                    discovery = discovery.copy(loading = false, error = message)
                }
            }
        }
    }

    fun addDiscovered(game: Game) {
        if (state.busy) return
        val existing = state.games.firstOrNull { it.igdbId == game.igdbId }
        if (existing != null) showGame(existing) else showGame(game, editing = true)
    }

    fun clearSearch() { searchResults = emptyList() }
    fun clearError() { state = state.copy(error = null) }
    fun setPostersPerRow(value: Int) {
        val count = value.coerceIn(1, 3)
        preferences.edit { putInt("posters_per_row", count) }
        state = state.copy(postersPerRow = count)
    }
    fun setTheme(value: String) {
        if (value !in setOf("cinematic", "editorial")) return
        preferences.edit { putString("native_theme", value) }
        state = state.copy(theme = value)
    }
}
