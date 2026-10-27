package com.anymex.desktop.network

import eu.kanade.tachiyomi.network.NetworkHelper
import java.net.HttpCookie
import java.net.URI

/** Host-provided browser state shared by the desktop extension clients. */
object DesktopNetworkState {
    fun setCookies(url: String, rawCookieHeader: String): Int {
        val uri = URI(url)
        require(uri.host?.isNotBlank() == true) { "A valid cookie URL is required" }

        val cookieHeader = rawCookieHeader.trim().let { value ->
            if (value.startsWith("cookie:", ignoreCase = true)) {
                value.substringAfter(':').trim()
            } else {
                value
            }
        }
        require(cookieHeader.isNotBlank()) { "Cookie header is required" }

        val cookies = cookieHeader.split(';').mapNotNull { part ->
            val pair = part.trim()
            if (pair.isEmpty()) return@mapNotNull null
            val separator = pair.indexOf('=')
            require(separator > 0) { "Invalid cookie pair" }
            val name = pair.substring(0, separator).trim()
            val value = pair.substring(separator + 1).trim()
            require(name.isNotEmpty()) { "Invalid cookie name" }
            HttpCookie(name, value).apply {
                path = "/"
                version = 0
                secure = uri.scheme.equals("https", ignoreCase = true)
            }
        }
        require(cookies.isNotEmpty()) { "Cookie header contains no cookies" }

        val store = NetworkHelper.sharedCookieManager.cookieStore
        cookies.forEach { cookie ->
            store.remove(uri, cookie)
            store.add(uri, cookie)
        }
        return cookies.size
    }
}
