package com.agtarena.mod

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var loginWebView: WebView
    private lateinit var gameWebView: WebView
    private lateinit var gameStatus: TextView
    private lateinit var radarOverlay: RadarOverlayView

    private val slitherHosts = setOf("slither.io", "www.slither.io")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(9, 5, 18)
        window.navigationBarColor = Color.rgb(9, 5, 18)
        buildInterface()
    }

    private fun buildInterface() {
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(9, 5, 18))
        }

        gameWebView = WebView(this).apply {
            visibility = View.GONE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val uri = request.url
                    return uri.scheme != "https" || uri.host !in slitherHosts
                }

                override fun onPageFinished(view: WebView, url: String) {
                    gameStatus.text = "JOGO CARREGADO • RADAR AGUARDANDO INTEGRAÇÃO"
                }
            }
        }
        root.addView(gameWebView, FrameLayout.LayoutParams(-1, -1))

        val gameHud = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.argb(220, 18, 9, 31), dp(14))
        }
        gameStatus = TextView(this).apply {
            text = "CONECTANDO AO JOGO..."
            setTextColor(Color.rgb(224, 195, 255))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        val exitButton = TextView(this).apply {
            text = "SAIR DA PARTIDA"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(91, 34, 135), dp(10))
            setOnClickListener { showLogin() }
        }
        gameHud.addView(gameStatus)
        gameHud.addView(exitButton)
        val hudParams = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END)
        hudParams.setMargins(dp(10), dp(10), dp(10), 0)
        root.addView(gameHud, hudParams)

        radarOverlay = RadarOverlayView(this).apply {
            visibility = View.GONE
            alpha = 0.82f
        }
        val radarParams = FrameLayout.LayoutParams(dp(156), dp(156), Gravity.BOTTOM or Gravity.END)
        radarParams.setMargins(0, 0, dp(16), dp(24))
        root.addView(radarOverlay, radarParams)

        loginWebView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            webChromeClient = WebChromeClient()
            addJavascriptInterface(AuthBridge(), "AndroidAuth")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = true
            }
            loadUrl("file:///android_asset/login.html")
        }
        root.addView(loginWebView, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    private inner class AuthBridge {
        @JavascriptInterface
        fun onAuthenticated(nick: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Bem-vindo, $nick!", Toast.LENGTH_SHORT).show()
                startGame()
            }
        }

        @JavascriptInterface
        fun onSignedOut() {
            runOnUiThread { showLogin() }
        }
    }

    private fun startGame() {
        loginWebView.visibility = View.GONE
        gameWebView.visibility = View.VISIBLE
        radarOverlay.visibility = View.VISIBLE
        gameStatus.text = "ABRINDO SLITHER.IO..."
        if (gameWebView.url.isNullOrBlank()) {
            gameWebView.loadUrl("https://slither.io/")
        } else {
            gameWebView.reload()
        }
    }

    private fun showLogin() {
        gameWebView.stopLoading()
        gameWebView.visibility = View.GONE
        radarOverlay.visibility = View.GONE
        loginWebView.visibility = View.VISIBLE
        loginWebView.evaluateJavascript(
            "window.agtReturnToLogin && window.agtReturnToLogin();",
            null
        )
    }

    override fun onBackPressed() {
        if (::gameWebView.isInitialized && gameWebView.visibility == View.VISIBLE) {
            showLogin()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::loginWebView.isInitialized) loginWebView.destroy()
        if (::gameWebView.isInitialized) gameWebView.destroy()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), Color.argb(130, 190, 112, 255))
        }
}

/**
 * Camada visual inicial do radar.
 * Ainda não recebe posições de cobras/jogadores: o Slither.io não oferece,
 * nesta arquitetura, um evento/API pública confirmada para esses dados.
 */
private class RadarOverlayView(context: android.content.Context) : View(context) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 202, 139, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
    }
    private val faint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(65, 205, 151, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f - 8f
        val radius = minOf(width, height) * 0.39f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 17, 8, 30)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius + 12f, fill)
        canvas.drawCircle(cx, cy, radius, line)
        canvas.drawCircle(cx, cy, radius * .66f, faint)
        canvas.drawCircle(cx, cy, radius * .33f, faint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, faint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, faint)
        canvas.drawCircle(cx, cy, 3.5f, dot)
        canvas.drawText("RADAR", cx, height - 10f, label)
    }
}
