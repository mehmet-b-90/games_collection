package de.gamescollection.viewer

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CookiePersistence {
    fun read(): List<String>
    fun write(cookies: List<String>)
}

/** Only persistent cookies are stored; passwords and CSRF tokens are never persisted. */
class SessionStore(context: Context) : CookiePersistence {
    private val preferences = context.getSharedPreferences("native_session", Context.MODE_PRIVATE)
    private val alias = "games_collection_session_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    override fun read(): List<String> = try {
        val stored = preferences.getString("cookies", null)
        if (stored == null) emptyList() else {
            val parts = stored.split(':', limit = 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8).lines().filter { it.isNotBlank() }
        }
    } catch (_: Exception) {
        preferences.edit { remove("cookies") }
        emptyList()
    }

    override fun write(cookies: List<String>) {
        if (cookies.isEmpty()) { preferences.edit { remove("cookies") }; return }
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(cipher.doFinal(cookies.joinToString("\n").toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            preferences.edit { putString("cookies", encoded) }
        } catch (_: Exception) {
            // A device without a usable keystore can still use an in-memory session.
            preferences.edit { remove("cookies") }
        }
    }
}
