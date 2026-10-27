package com.anymex.desktop.network

import com.google.gson.Gson
import eu.kanade.tachiyomi.network.RequestTag
import okhttp3.Interceptor
import okhttp3.Response
import java.io.PrintStream

/** A challenge observed on the wire, before an extension can consume its response. */
data class CloudflareChallenge(
    val url: String,
    val statusCode: Int,
    val userAgent: String?,
)

/**
 * Desktop-only notification seam. Hosts own recovery and retry; observing a
 * challenge never consumes the response or waits for a host to answer.
 */
object CloudflareNotifications {
    @Volatile
    var onChallenge: ((requestId: String?, challenge: CloudflareChallenge) -> Unit)? = null

    /** Use the sidecar's existing output lock so events and replies stay atomic. */
    fun install(output: PrintStream, outputLock: Any) {
        val gson = Gson()
        onChallenge = { requestId, challenge ->
            // No top-level "id": older clients must not treat this as an RPC reply.
            val event = mapOf(
                "event" to "cloudflare",
                "requestId" to requestId,
                "data" to challenge,
            )
            synchronized(outputLock) {
                output.println(gson.toJson(event))
            }
        }
    }
}

/** Install as a network interceptor to see challenges inside extension retries. */
class CloudflareNotificationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val callback = CloudflareNotifications.onChallenge ?: return response

        val mitigated = response.header("cf-mitigated")
        val server = response.header("Server")
        val isChallenge = isCloudflareChallenge(response.code, mitigated, server)
        if (!isChallenge) return response

        val request = response.request
        try {
            callback(
                request.tag(String::class.java) ?: RequestTag.get(),
                CloudflareChallenge(
                    url = request.url.toString(),
                    statusCode = response.code,
                    userAgent = request.header("User-Agent"),
                ),
            )
        } catch (e: Exception) {
            // A notification failure must not alter the extension's HTTP result.
            System.err.println("[Cloudflare] Failed to notify host: ${e.message}")
        }
        return response
    }
}

internal fun isCloudflareChallenge(statusCode: Int, mitigated: String?, server: String?): Boolean {
    if (statusCode == 429) return true
    if (mitigated.equals("challenge", ignoreCase = true)) return true
    val servedByCloudflare = server.equals("cloudflare", ignoreCase = true) ||
        server.equals("cloudflare-nginx", ignoreCase = true)
    return statusCode in listOf(403, 503) && servedByCloudflare
}
