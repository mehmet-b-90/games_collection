@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.gamescollection.viewer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toUri
import coil.compose.AsyncImage
import java.util.Locale

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAA9BFF), onPrimary = Color(0xFF251457),
    primaryContainer = Color(0xFF493694), onPrimaryContainer = Color(0xFFECE7FF),
    background = Color(0xFF101720), onBackground = Color(0xFFF3F4FC),
    surface = Color(0xFF1A2534), onSurface = Color(0xFFF3F4FC),
    surfaceVariant = Color(0xFF263349), onSurfaceVariant = Color(0xFFB8C2D5),
    outline = Color(0xFF526079)
)
private val EditorialColors = lightColorScheme(
    primary = Color(0xFFA63E29), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD0), onPrimaryContainer = Color(0xFF5C1708),
    background = Color(0xFFF5F0E7), onBackground = Color(0xFF29251F),
    surface = Color(0xFFFFFBF5), onSurface = Color(0xFF29251F),
    surfaceVariant = Color(0xFFECE4D7), onSurfaceVariant = Color(0xFF655C51),
    outline = Color(0xFF897F72)
)

@Composable fun CollectionApp(model: CollectionViewModel) {
    val state = model.state
    val activity = LocalActivity.current as? ComponentActivity
    SideEffect {
        val style = if (state.theme == "editorial") SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    MaterialTheme(colorScheme = if (state.theme == "editorial") EditorialColors else DarkColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var serverDialog by rememberSaveable { mutableStateOf(false) }
            var settingsOpen by rememberSaveable(state.server, state.username) { mutableStateOf(false) }
            // Never retain a previous account's detail view or drafts across a login/server change.
            var tab by rememberSaveable(state.server, state.username) { mutableIntStateOf(0) }
            var filtersOpen by rememberSaveable(state.server, state.username) { mutableStateOf(false) }
            var searchOpen by rememberSaveable(state.server, state.username) { mutableStateOf(false) }
            var filter by remember(state.server, state.username) { mutableStateOf(CollectionFilter()) }
            var sort by remember(state.server, state.username) { mutableStateOf(GameSort.NEWEST) }
            var selected by remember(state.server, state.username) { mutableStateOf<Game?>(null) }
            var edit by remember(state.server, state.username) { mutableStateOf(false) }
            var adding by remember(state.server, state.username) { mutableStateOf(false) }

            if (state.server.isEmpty()) {
                ServerScreen(state, onConnect = { address, http -> model.connect(address, http) })
            } else if (state.username == null) {
                LoginScreen(state, model::login, { model.connect() }, { serverDialog = true })
            } else {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(title = {
                            Column {
                                Text("Games Collection", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Hallo, ${state.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }, actions = {
                            if (tab == 0) IconButton(onClick = { searchOpen = true }) {
                                Icon(Icons.Default.Search, "Sammlung durchsuchen",
                                    tint = if (filter.query.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = model::refresh, enabled = !state.busy) { Icon(Icons.Default.Refresh, "Sammlung aktualisieren") }
                        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
                    },
                    bottomBar = {
                        Surface(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 6.dp)
                            .widthIn(max = 560.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp, shadowElevation = 8.dp) {
                            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                                NavItem("Sammlung", NativeIcons.Library, tab == 0, Modifier.weight(1f)) { tab = 0 }
                                NavItem("Filter", NativeIcons.Filters, filter.active, Modifier.weight(1f)) { filtersOpen = true }
                                NavItem("Statistik", NativeIcons.Statistics, tab == 1, Modifier.weight(1f)) { tab = 1 }
                                NavItem("Profil", Icons.Default.Person, tab == 2, Modifier.weight(1f)) { tab = 2 }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (tab == 0) FloatingActionButton(onClick = { model.clearSearch(); adding = true },
                            containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.Add, "Spiel hinzufügen") }
                    }
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize()) {
                        when (tab) {
                            0 -> LibraryScreen(state, filter, sort, { sort = it }) { selected = it; edit = false }
                            1 -> StatisticsScreen(state.games)
                            else -> ProfileScreen(state, { settingsOpen = true }, {
                                selected = null; adding = false; model.logout()
                            })
                        }
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    }
                }
            }

            if (settingsOpen && state.username != null) SettingsDialog(state, model::setTheme, model::setCoverSize,
                { serverDialog = true }, { settingsOpen = false })
            if (serverDialog) Dialog(onDismissRequest = { if (!state.busy) serverDialog = false }) {
                Surface(shape = RoundedCornerShape(24.dp)) {
                    ServerScreen(state, { address, http -> model.connect(address, http); serverDialog = false },
                        { serverDialog = false })
                }
            }
            if (filtersOpen && state.username != null) ModalBottomSheet(onDismissRequest = { filtersOpen = false }) {
                FilterPanel(state, filter, { filter = it }, { filtersOpen = false })
            }
            if (searchOpen && state.username != null && tab == 0) CollectionSearchPopup(
                query = filter.query,
                changeQuery = { filter = filter.copy(query = it) },
                matches = state.games.count { filter.matches(it) },
                close = { searchOpen = false }
            )
            if (adding && state.username != null) SearchDialog(state, model.searchResults, model::search,
                { adding = false }, { selected = it; edit = true; adding = false })
            selected?.takeIf { state.username != null }?.let { game ->
                GameDialog(game, state, edit, { edit = true }, { if (!state.busy) selected = null },
                    { updated -> model.save(updated) { selected = null } },
                    { model.delete(game) { selected = null } })
            }
            state.error?.let { message ->
                AlertDialog(onDismissRequest = model::clearError, title = { Text("Hinweis") },
                    text = { Text(message) }, confirmButton = { TextButton(onClick = model::clearError) { Text("OK") } })
            }
            BackHandler(enabled = state.username != null && tab != 0 && selected == null && !adding && !filtersOpen) { tab = 0 }
        }
    }
}

@Composable private fun NavItem(label: String, icon: ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Text(label, color = color, fontSize = 11.sp, lineHeight = 12.sp)
    }
}

@Composable private fun CollectionSearchPopup(query: String, changeQuery: (String) -> Unit, matches: Int, close: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp
    val offset = with(LocalDensity.current) { top.roundToPx() }
    Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, offset), onDismissRequest = { keyboard?.hide(); close() },
        properties = PopupProperties(focusable = true)) {
        Surface(modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 560.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp, shadowElevation = 16.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FormTextField(query, changeQuery, label = "Sammlung durchsuchen",
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { changeQuery("") }) {
                            Icon(Icons.Default.Clear, "Suchtext löschen")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); close() }))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("$matches Titel", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { keyboard?.hide(); close() }) { Text("Fertig") }
                }
            }
        }
        LaunchedEffect(Unit) {
            // The popup has its own composition; wait until its input is attached before requesting focus.
            withFrameNanos { }
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
}

@Composable private fun ServerScreen(state: ViewerState, onConnect: (String, Boolean) -> Unit, onCancel: (() -> Unit)? = null) {
    var address by rememberSaveable { mutableStateOf(state.server) }
    var allowHttp by rememberSaveable { mutableStateOf(state.server.startsWith("http://")) }
    Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp)
        .widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Spacer(Modifier.height(20.dp))
        Icon(NativeIcons.Gamepad, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text("Deine Sammlung.\nÜberall dabei.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Verbinde die App mit deinem Games-Collection-Server.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FormTextField(address, { address = it }, modifier = Modifier.fillMaxWidth(), label = "Serveradresse",
            placeholder = "https://games.example.org", singleLine = true, enabled = !state.busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(allowHttp, { allowHttp = it }, enabled = !state.busy)
            Text("HTTP für lokale Tests zulassen", style = MaterialTheme.typography.bodyMedium)
        }
        Text("Bei HTTP werden Anmeldedaten unverschlüsselt übertragen. Nutze möglichst HTTPS. Gib im WLAN die Adresse deines Servers an, nicht localhost.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { onConnect(address, allowHttp) }, enabled = !state.busy && address.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Verbinden") }
        if (onCancel != null) TextButton(onClick = onCancel, enabled = !state.busy) { Text("Abbrechen") }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable private fun LoginScreen(state: ViewerState, login: (String, String, Boolean) -> Unit, retry: () -> Unit, changeServer: () -> Unit) {
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    // Password is deliberately not saveable or persisted.
    var password by remember { mutableStateOf("") }
    var rememberLogin by rememberSaveable { mutableStateOf(true) }
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(NativeIcons.Gamepad, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
            Text("Willkommen zurück", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(state.server.toUri().authority.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.connected) {
                FormTextField(username, { username = it }, label = "Benutzername", singleLine = true,
                    enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                FormTextField(password, { password = it }, label = "Passwort", singleLine = true,
                    enabled = !state.busy, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(rememberLogin, { rememberLogin = it }, enabled = !state.busy)
                    Text("Eingeloggt bleiben")
                }
                Button(onClick = { login(username, password, rememberLogin); password = "" }, modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy && username.isNotBlank() && password.isNotEmpty()) { Text("Anmelden") }
                TextButton(onClick = { openBrowser(context, state.server + "register") }, enabled = !state.busy) { Text("Konto im Browser anlegen") }
            } else {
                Text("Verbindung zum Server herstellen. Der Server muss die native App-API unterstützen.")
                Button(onClick = retry, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Erneut verbinden") }
            }
            TextButton(onClick = changeServer, enabled = !state.busy) { Text("Serveradresse ändern") }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable private fun LibraryScreen(state: ViewerState, filter: CollectionFilter,
    sort: GameSort, changeSort: (GameSort) -> Unit, select: (Game) -> Unit) {
    val visible = remember(state.games, filter, sort) { state.games.filtered(filter, sort) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("DEINE BIBLIOTHEK", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${visible.size} Titel", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            ChoiceField("Sortierung", sort.label, GameSort.entries.map { it.label }, modifier = Modifier.width(200.dp), compact = true) { label ->
                changeSort(GameSort.entries.first { it.label == label })
            }
        }
        if (visible.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (state.games.isEmpty()) "Deine Sammlung wartet auf das erste Spiel.\nTippe auf +, um einen Titel hinzuzufügen."
                else "Keine Spiele für diese Filter gefunden.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else LazyVerticalGrid(columns = GridCells.Adaptive(state.coverSize.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
            items(visible, key = { it.id }) { game -> GameCard(game, select) }
        }
    }
}

@Composable private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun GameCard(game: Game, select: (Game) -> Unit) {
    // Round only the cover. Clipping the entire card also clips the lower text corners.
    Column(Modifier.clickable { select(game) }) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (game.cover.isNotEmpty()) AsyncImage(game.cover, "Cover von ${game.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(NativeIcons.Gamepad, null, Modifier.size(48.dp).align(Alignment.Center))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .75f)), startY = 140f)))
            Surface(modifier = Modifier.padding(8.dp).align(Alignment.TopStart), shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = .72f)) {
                Text(game.status, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            if (game.rating != null) Text("★ ${game.rating}", color = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp), fontSize = 13.sp)
        }
        Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(game.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
            Text(game.platforms.joinToString(" · "), maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun ChoiceField(label: String, value: String, options: List<String>, modifier: Modifier = Modifier,
    enabled: Boolean = true, compact: Boolean = false, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, shape = FormFieldShape,
            contentPadding = if (compact) PaddingValues(horizontal = 12.dp, vertical = 4.dp) else FormFieldPadding,
            modifier = Modifier.fillMaxWidth().then(if (compact) Modifier.semantics { contentDescription = label }
                else Modifier.heightIn(min = FormFieldMinHeight))) {
            if (compact) Text(value.ifEmpty { "Alle" }, modifier = Modifier.weight(1f), maxLines = 1,
                overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            else Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value.ifEmpty { "Alle" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(20.dp))
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 340.dp)) {
            options.forEach { item -> DropdownMenuItem(text = { Text(item.ifEmpty { "Alle" }) }, onClick = { select(item); expanded = false }) }
        }
    }
}

@Composable private fun FilterPanel(state: ViewerState, filter: CollectionFilter, change: (CollectionFilter) -> Unit, close: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Deine Sammlung filtern", style = MaterialTheme.typography.headlineSmall)
        ChoiceField("Plattform", filter.platform, listOf("") + state.platforms) { change(filter.copy(platform = it)) }
        ChoiceField("Status", filter.status, listOf("", "Nicht gespielt", "Gespielt", "Abgebrochen")) { change(filter.copy(status = it)) }
        ChoiceField("Format", filter.format, listOf("", "Digital", "Physisch")) { change(filter.copy(format = it)) }
        ChoiceField("Engine", filter.engine, listOf("") + state.games.flatMap { it.engine.split(',') }.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()) { change(filter.copy(engine = it)) }
        ChoiceField("Altersfreigabe", filter.ageRating, listOf("") + state.games.map { it.ageRating }.filter { it.isNotEmpty() }.distinct().sorted()) { change(filter.copy(ageRating = it)) }
        ChoiceField("Min. Bewertung", filter.minimumRating?.toString().orEmpty(), listOf("") + (0..10).map { it.toString() }) { change(filter.copy(minimumRating = it.toIntOrNull())) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { change(CollectionFilter(query = filter.query)) }) { Text("Zurücksetzen") }
            Button(onClick = close) { Text("${state.games.count { filter.matches(it) }} Titel anzeigen") }
        }
    }
}

@Composable private fun StatisticsScreen(games: List<Game>) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Text("Deine Sammlung in Zahlen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard("Spiele", games.size.toString(), Modifier.weight(1f))
                val ratings = games.mapNotNull { it.rating }
                SummaryCard("Ø Bewertung", if (ratings.isEmpty()) "–" else String.format(Locale.GERMAN, "%.1f", ratings.average()), Modifier.weight(1f))
                SummaryCard("Plattformen", games.flatMap { it.platforms }.distinct().size.toString(), Modifier.weight(1f))
            }
        }
        item { Distribution("Plattformen · Besitzzuordnungen", games.flatMap { it.platforms }.groupingBy { it }.eachCount()) }
        item { Distribution("Status", games.groupingBy { it.status }.eachCount()) }
        item { Distribution("Format", games.groupingBy { it.format }.eachCount()) }
        item { Distribution("Engine", games.map { it.engine.ifEmpty { "Unbekannt" } }.groupingBy { it }.eachCount()) }
    }
}

@Composable private fun Distribution(title: String, counts: Map<String, Int>) {
    val total = counts.values.sum()
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (total == 0) Text("Noch keine Daten", color = MaterialTheme.colorScheme.onSurfaceVariant)
            counts.entries.sortedByDescending { it.value }.forEach { (label, count) ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("$count (${count * 100 / total} %)", style = MaterialTheme.typography.labelSmall)
                    }
                    LinearProgressIndicator(progress = { count.toFloat() / total }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape))
                }
            }
        }
    }
}

@Composable private fun ProfileScreen(state: ViewerState, settings: () -> Unit, logout: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(state.username.orEmpty().take(1).uppercase(), style = MaterialTheme.typography.headlineMedium) }
        }
        Text(state.username.orEmpty(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Dein Profil", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Server", style = MaterialTheme.typography.titleMedium)
        Text(state.server, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            ProfileAction("Einstellungen", Icons.Default.Settings, true, onClick = settings)
            HorizontalDivider(Modifier.padding(start = 52.dp, end = 16.dp))
            ProfileAction("Konto im Browser verwalten", Icons.Default.AccountCircle, !state.busy,
                onClick = { openBrowser(context, state.server) })
            HorizontalDivider(Modifier.padding(start = 52.dp, end = 16.dp))
            ProfileAction("Abmelden", Icons.AutoMirrored.Filled.ExitToApp, !state.busy, emphasize = true, onClick = logout)
        }
    }
}

@Composable private fun SettingsDialog(state: ViewerState, setTheme: (String) -> Unit,
    setCoverSize: (Int) -> Unit, server: () -> Unit, close: () -> Unit) {
    var coverSize by remember(state.coverSize) { mutableFloatStateOf(state.coverSize.toFloat()) }
    FullScreenDialog("Einstellungen", true, close) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Darstellung auf diesem Gerät", style = MaterialTheme.typography.titleMedium)
            listOf("cinematic" to "Cinematic Dark", "editorial" to "Editorial Shelf").forEach { (key, label) ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { setTheme(key) },
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(state.theme == key, { setTheme(key) })
                    Text(label)
                }
            }
            HorizontalDivider()
            Text("Covergröße", style = MaterialTheme.typography.titleMedium)
            Text("Kleinere Cover zeigen mehr Titel nebeneinander. Die Spalten passen sich automatisch an die Bildschirmbreite an.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = coverSize, onValueChange = { coverSize = it },
                onValueChangeFinished = { setCoverSize(coverSize.toInt()) }, valueRange = 100f..240f, steps = 13,
                modifier = Modifier.semantics { contentDescription = "Covergröße" })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Klein", style = MaterialTheme.typography.labelSmall)
                Text("Groß", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { setCoverSize(150) }) { Text("Standardgröße wiederherstellen") }
            HorizontalDivider()
            Text("Server", style = MaterialTheme.typography.titleMedium)
            Text(state.server, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                ProfileAction("Serveradresse ändern", Icons.Default.Settings, !state.busy, onClick = server)
            }
        }
    }
}

@Composable private fun ProfileAction(label: String, icon: ImageVector, enabled: Boolean,
    emphasize: Boolean = false, onClick: () -> Unit) {
    val color = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
        emphasize -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (!emphasize) Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable private fun SearchDialog(state: ViewerState, results: List<Game>, search: (String, String) -> Unit,
    close: () -> Unit, select: (Game) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var searched by rememberSaveable { mutableStateOf(false) }
    FullScreenDialog("Spiel hinzufügen", !state.busy, close) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Suche nach einem Titel in IGDB.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FormTextField(query, { query = it }, label = "Spieletitel", singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            FormTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = "Release-Jahr (optional)", singleLine = true,
                enabled = !state.busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Button(onClick = { searched = true; search(query, year) }, enabled = !state.busy && query.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("IGDB durchsuchen")
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (searched && results.isEmpty() && !state.busy) Text("Keine Treffer. Ändere die Suche oder lege den Titel manuell an.")
            TextButton(onClick = { select(Game(title = query.trim())) }, enabled = !state.busy) { Text("Spiel manuell anlegen") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.igdbId ?: it.title }) { game ->
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !state.busy) { select(game) }) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(game.cover, null, modifier = Modifier.size(48.dp, 66.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                                Text(game.title, fontWeight = FontWeight.Bold)
                                Text(game.release, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun FullScreenDialog(title: String, dismissible: Boolean, close: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = { if (dismissible) close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
                TopAppBar(title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = {
                    IconButton(onClick = close, enabled = dismissible) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
                })
            }) { padding -> Box(Modifier.padding(padding).imePadding()) { content() } }
        }
    }
}

@Composable private fun GameDialog(game: Game, state: ViewerState, edit: Boolean, startEditing: () -> Unit,
    close: () -> Unit, save: (Game) -> Unit, delete: () -> Unit) {
    val context = LocalContext.current
    var draft by remember(game) { mutableStateOf(game) }
    var platformDialog by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val valid = draft.title.isNotBlank() && draft.title.length <= 200 && draft.platforms.isNotEmpty()
            && (draft.release.isEmpty() || (draft.release.length == 4 && draft.release.toIntOrNull() in 1900..2100))
    FullScreenDialog(if (edit) if (game.id == 0) "Neues Spiel" else "Spiel bearbeiten" else "Spieldetails", !state.busy, close) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    AsyncImage(draft.cover, "Cover von ${draft.title}", modifier = Modifier.width(112.dp).aspectRatio(.72f).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(draft.title.ifEmpty { "Dein neues Spiel" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(listOf(draft.release, draft.ageRating).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!edit) Text(draft.status + (draft.rating?.let { " · ★ $it/10" } ?: ""))
                    }
                }
            }
            if (edit) {
                item { FormTextField(draft.title, { draft = draft.copy(title = it) }, label = "Spieletitel", singleLine = false,
                    enabled = !state.busy, modifier = Modifier.fillMaxWidth()) }
                item { FormTextField(draft.release, { draft = draft.copy(release = it.filter(Char::isDigit).take(4)) }, label = "Release-Jahr",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !state.busy, modifier = Modifier.fillMaxWidth()) }
                item { ChoiceField("Format", draft.format, listOf("Digital", "Physisch"), enabled = !state.busy) { draft = draft.copy(format = it) } }
                item { ChoiceField("Status", draft.status, listOf("Nicht gespielt", "Gespielt", "Abgebrochen"), enabled = !state.busy) { draft = draft.copy(status = it) } }
                item { ChoiceField("Bewertung", draft.rating?.toString() ?: "Nicht bewertet", listOf("Nicht bewertet") + (0..10).map { it.toString() }, enabled = !state.busy) { draft = draft.copy(rating = it.toIntOrNull()) } }
                item { OutlinedButton(onClick = { platformDialog = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (draft.platforms.isEmpty()) "Besitzplattformen wählen (Pflichtfeld)" else "Besitz: ${draft.platforms.joinToString(", ")}")
                } }
                item { Button(onClick = { save(draft.copy(title = draft.title.trim())) }, enabled = valid && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Speichern") } }
            } else {
                item { Button(onClick = startEditing, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("Bearbeiten") } }
                item { Metadata("Besitz auf Plattform", game.platforms.joinToString(", ")) }
                item { Metadata("Format", game.format) }
            }
            item { Metadata("Entwickler", draft.developer) }
            item { Metadata("Publisher", draft.publisher) }
            item { Metadata("Engine", draft.engine) }
            item { Metadata("Verfügbare Plattformen", draft.available) }
            if (draft.igdbUrl.isNotEmpty()) item { OutlinedButton(onClick = { openBrowser(context, draft.igdbUrl) }, modifier = Modifier.fillMaxWidth()) { Text("Zu IGDB") } }
            if (draft.title.isNotBlank()) item {
                OutlinedButton(onClick = { openBrowser(context, "https://www.dekudeals.com/search?q=${Uri.encode(draft.title)}") }, modifier = Modifier.fillMaxWidth()) { Text("Zu Deku Deals") }
            }
            if (game.id != 0) item { TextButton(onClick = { confirmDelete = true }, enabled = !state.busy) { Text("Spiel löschen", color = MaterialTheme.colorScheme.error) } }
            if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
    }
    if (platformDialog) AlertDialog(onDismissRequest = { platformDialog = false }, title = { Text("Besitz auf Plattform") }, text = {
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(state.platforms) { platform ->
                Row(Modifier.fillMaxWidth().clickable {
                    draft = draft.copy(platforms = if (platform in draft.platforms) draft.platforms - platform else draft.platforms + platform)
                }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(platform in draft.platforms, null)
                    Text(platform, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { platformDialog = false }) { Text("Übernehmen") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Spiel löschen?") },
        text = { Text("„${game.title}“ wird aus deiner Sammlung gelöscht.") }, confirmButton = {
            TextButton(onClick = { confirmDelete = false; delete() }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
        }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } })
}

@Composable private fun Metadata(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifEmpty { "–" }, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun openBrowser(context: android.content.Context, url: String) {
    val uri = url.toUri()
    if (uri.scheme != "https" && uri.scheme != "http") return
    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)) }
    catch (_: ActivityNotFoundException) { Toast.makeText(context, "Kein Browser gefunden.", Toast.LENGTH_LONG).show() }
}
