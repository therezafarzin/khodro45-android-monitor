package com.reza.khodro45monitor

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView

/** A single native WebView lives for this Activity's complete lifetime; no login traffic is instrumented. */
class LoginActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var status: TextView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                    !trusted(request?.url?.host)

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    updateLoginState()
                }
            }
        }
        status = TextView(this).apply {
            setTextColor(Color.WHITE); setBackgroundColor(0xCC071B33.toInt()); setPadding(28, 20, 28, 20)
            text = "ورود به خودرو45 — شماره و رمز یک‌بارمصرف را فقط در صفحهٔ خودرو45 وارد کنید."
        }
        val done = Button(this).apply {
            text = "بازگشت به مانیتور"
            setOnClickListener { finish() }
        }
        root.addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(status, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))
        root.addView(done, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = 24 })
        root.addView(ProgressBar(this), FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        setContentView(root)
        if (savedInstanceState == null) webView.loadUrl("https://dealer.khodro45.com/cars?ordering=suggested")
    }

    override fun onResume() { super.onResume(); updateLoginState() }
    override fun onPause() { webView.onPause(); super.onPause() }
    override fun onDestroy() { webView.destroy(); super.onDestroy() }

    private fun trusted(host: String?) = host == "khodro45.com" || host?.endsWith(".khodro45.com") == true
    private fun updateLoginState() {
        status.text = if (Khodro45Session.isLoggedIn()) "ورود خودرو45 برقرار است. می‌توانید به مانیتور بازگردید." else "ورود به خودرو45 — شماره و رمز یک‌بارمصرف را فقط در صفحهٔ خودرو45 وارد کنید."
    }
}
