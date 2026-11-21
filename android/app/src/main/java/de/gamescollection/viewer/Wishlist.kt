package de.gamescollection.viewer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

data class WishRelease(val platform: Int, val region: String, val description: String) {
    companion object {
        fun parse(json: JSONObject) = WishRelease(json.getInt("platform"), json.getString("region"),
            listOf(json.text("name"), json.text("region"), json.text("label")).filter(String::isNotBlank).joinToString(" · "))
    }
}
data class WishEntry(val game: Game, val release: WishRelease, val notifyRelease: Boolean, val stale: Boolean, val released: Boolean)

internal class Wishlist(private val client: ApiClient?, private val scope: CoroutineScope) {
    var entries by mutableStateOf<List<WishEntry>>(emptyList())
    var choices by mutableStateOf<Pair<Game, List<WishRelease>>?>(null)
    var error by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    private val mutex = Mutex()

    private fun run(work: suspend (ApiClient) -> Unit) {
        scope.launch {
            mutex.withLock {
                busy = true
                try { work(requireNotNull(client) { "Bitte verbinde dich mit dem Server." }); error = null }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.localizedMessage ?: "Wunschliste nicht erreichbar." }
                finally { busy = false }
            }
        }
    }
    fun refresh() = run { entries = it.wishes() }
    fun choose(game: Game) = run { choices = game to it.wishOptions(requireNotNull(game.igdbId)) }
    fun save(game: Game, option: WishRelease) = run {
        it.saveWish(requireNotNull(game.igdbId), option)
        choices = null
        entries = it.wishes()
    }
    fun remove(game: Game) = run { it.removeWish(requireNotNull(game.igdbId)); entries = it.wishes() }
    fun seen(items: List<WishEntry>) = run {
        for (entry in items) it.seenWish(requireNotNull(entry.game.igdbId))
        entries = entries.map { entry -> if (entry in items) entry.copy(notifyRelease = false) else entry }
    }
}

@Composable
internal fun WishButton(game: Game, wishlist: Wishlist) {
    val saved = wishlist.entries.any { it.game.igdbId == game.igdbId }
    TextButton(onClick = { wishlist.choose(game) }, enabled = !saved && !wishlist.busy) {
        Text(if (saved) "✓ Vorgemerkt" else "Auf die Wunschliste")
    }
}

@Composable
internal fun WishlistScreen(wishlist: Wishlist, owned: List<Game>, add: (Game) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Wunschliste", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = wishlist::refresh, enabled = !wishlist.busy) { Text("Aktualisieren") }
            if (wishlist.entries.isEmpty() && !wishlist.busy && wishlist.error == null) Text("Deine Wunschliste ist leer.")
        }
        items(wishlist.entries, key = { requireNotNull(it.game.igdbId) }) { entry ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GameCoverImage(entry.game, null, Modifier.size(54.dp, 76.dp))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(entry.game.title, style = MaterialTheme.typography.titleMedium)
                        Text(entry.release.description)
                        if (entry.released) Text("✓ Erschienen")
                        if (entry.stale) Text("Gespeicherter Termin · IGDB derzeit nicht aktuell")
                        if (owned.any { it.igdbId == entry.game.igdbId }) Text("✓ In Sammlung")
                        else TextButton(onClick = { add(entry.game) }) { Text("Zur Sammlung hinzufügen") }
                        TextButton(onClick = { wishlist.remove(entry.game) }, enabled = !wishlist.busy) { Text("Entfernen") }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WishlistEvents(wishlist: Wishlist, connected: Boolean, account: String) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(wishlist, connected, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (connected && event == Lifecycle.Event.ON_RESUME) wishlist.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val due = wishlist.entries.filter { it.notifyRelease }
    val notices = due.joinToString("\n") { it.game.title + " ist erschienen · " + it.release.description }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    fun post() {
        manager.createNotificationChannel(NotificationChannel("wish_releases", "Wunschlisten-Veröffentlichungen",
            NotificationManager.IMPORTANCE_DEFAULT))
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        if (notices.isNotEmpty()) manager.notify(account, 1, Notification.Builder(context, "wish_releases")
            .setSmallIcon(R.drawable.ic_launcher_monochrome).setContentTitle("Games Collection")
            .setContentText(notices).setStyle(Notification.BigTextStyle().bigText(notices))
            .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE))
            .setOnlyAlertOnce(true).setAutoCancel(true).build())
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) post() }
    val hasWishes = wishlist.entries.isNotEmpty()
    LaunchedEffect(hasWishes) {
        if (hasWishes && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            val prefs = context.getSharedPreferences("wish_notifications", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("asked", false)) {
                prefs.edit { putBoolean("asked", true) }
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    LaunchedEffect(notices) { if (notices.isNotEmpty()) post() }
    wishlist.choices?.let { (game, options) ->
        AlertDialog(onDismissRequest = { wishlist.choices = null }, title = { Text(game.title) },
            text = { LazyColumn { items(options) { option ->
                TextButton(onClick = { wishlist.save(game, option) }, enabled = !wishlist.busy) { Text(option.description) }
            } } }, confirmButton = {}, dismissButton = {
                TextButton(onClick = { wishlist.choices = null }) { Text("Abbrechen") }
            })
    }
    if (notices.isNotEmpty()) AlertDialog(onDismissRequest = { wishlist.seen(due) },
        title = { Text("Neu erschienen") }, text = { Text(notices) }, confirmButton = {
            TextButton(onClick = { wishlist.seen(due) }) { Text("Gelesen") }
        }, dismissButton = {
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) TextButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text("Systemmeldungen erlauben")
            }
        })
    wishlist.error?.let { message ->
        AlertDialog(onDismissRequest = { wishlist.error = null }, title = { Text("Wunschliste") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { wishlist.error = null }) { Text("Schließen") } })
    }
}
