package de.gamescollection.viewer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

// Bound the encoded image because one account-bound editor draft is saved in SavedStateHandle.
internal suspend fun prepareCustomCover(context: Context, uri: Uri): String {
    val request = ImageRequest.Builder(context).data(uri).size(480).scale(Scale.FIT)
        .allowHardware(false).memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: error("Ungültiges Bild.")
    val drawable = result.drawable
    val scale = minOf(1.0, 480.0 / maxOf(drawable.intrinsicWidth, drawable.intrinsicHeight))
    val bitmap = drawable.toBitmap(
        (drawable.intrinsicWidth * scale).toInt().coerceAtLeast(1),
        (drawable.intrinsicHeight * scale).toInt().coerceAtLeast(1)
    )
    val output = ByteArrayOutputStream()
    for (quality in listOf(85, 70, 55, 40, 25)) {
        output.reset()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output))
        if (output.size() <= 48 * 1024) return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
    error("Das Bild konnte nicht ausreichend verkleinert werden.")
}

private data class CoverAccess(val server: String, val loader: ImageLoader)
private val LocalCoverAccess = compositionLocalOf<CoverAccess?> { null }
private val coverPath = Regex("/api/games/[1-9][0-9]*/cover/[0-9a-f]{64}")

@Composable
internal fun CollectionTheme(client: ApiClient?, colors: ColorScheme, content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val access = remember(client, context) { client?.let { CoverAccess(it.server, it.coverLoader(context)) } }
    DisposableEffect(access) { onDispose { access?.loader?.shutdown() } }
    CompositionLocalProvider(LocalCoverAccess provides access) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

@Composable
internal fun GameCoverImage(game: Game, description: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val access = LocalCoverAccess.current
    val editedCover = game.customCoverEdit
    if (!editedCover.isNullOrEmpty()) {
        val bytes = remember(editedCover) { ByteBuffer.wrap(Base64.decode(editedCover, Base64.DEFAULT)) }
        val request = remember(context, bytes) {
            ImageRequest.Builder(context).data(bytes).memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED).build()
        }
        AsyncImage(request, description, modifier, contentScale = ContentScale.Crop)
    } else if (editedCover == null && access != null && coverPath.matches(game.customCoverUrl)) {
        val url = access.server.trimEnd('/') + game.customCoverUrl
        val request = remember(context, url) {
            ImageRequest.Builder(context).data(url).memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED).build()
        }
        AsyncImage(request, description, access.loader, modifier, contentScale = ContentScale.Crop)
    } else if (game.cover.isNotEmpty()) {
        AsyncImage(game.cover, description, modifier, contentScale = ContentScale.Crop)
    }
}
