package com.reza.khodro45monitor

import android.webkit.CookieManager

/** Reads the first-party dealer token only at request time. It is never persisted by this app. */
object Khodro45Session {
    private const val TOKEN_COOKIE = "k45-superapp-dealer-access-token"
    private const val DEALER_ORIGIN = "https://dealer.khodro45.com"

    fun token(): String? = runCatching {
        CookieManager.getInstance().getCookie(DEALER_ORIGIN)
            ?.split(';')
            ?.map { it.trim() }
            ?.firstOrNull { it.substringBefore('=').trim() == TOKEN_COOKIE }
            ?.substringAfter('=', "")
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun isLoggedIn(): Boolean = token() != null
}
