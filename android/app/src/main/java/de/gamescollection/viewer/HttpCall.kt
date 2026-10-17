package de.gamescollection.viewer

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Keep cancellation wired to the call until the entire response body has been consumed. */
internal suspend fun Call.readBody(): HttpResult = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            try {
                response.use {
                    if (!continuation.isActive) return
                    val result = HttpResult(response.code, response.header("Content-Type"),
                        response.header("X-CSRF-Error") == "1", response.body?.string().orEmpty())
                    continuation.resume(result)
                }
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    })
}

internal data class HttpResult(val code: Int, val contentType: String?, val csrfError: Boolean, val body: String)
