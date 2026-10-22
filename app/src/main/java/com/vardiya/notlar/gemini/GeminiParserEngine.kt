package com.vardiya.notlar.gemini

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Why a page could not be read; the screen turns each into a Turkish message. */
enum class ParseFailure { InvalidLink, Network, NotFound, Blocked, Empty, Crashed, Timeout }

sealed interface ParseResult {
    data class Success(val turns: List<ChatTurn>) : ParseResult
    data class Failure(val reason: ParseFailure) : ParseResult
}

/**
 * Loads a Gemini share page in a WebView that is never shown and reads the turns out of the rendered
 * page with a script. One call to [parse] owns one WebView, and the WebView is destroyed when the call
 * ends, fails or is cancelled. Only https Gemini share pages are loaded; the page gets no bridge to the app.
 */
class GeminiParserEngine(context: Context) {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    /**
     * Must be called on the main thread. [onAttempt] gets the number of the script run (1..[MAX_ATTEMPTS])
     * each time the page is read, so the screen can show progress.
     */
    suspend fun parse(url: String, onAttempt: (Int) -> Unit): ParseResult {
        val link = GeminiChat.validLink(url) ?: return ParseResult.Failure(ParseFailure.InvalidLink)
        return suspendCancellableCoroutine { continuation ->
            val session = Session(link, onAttempt) { result -> if (continuation.isActive) continuation.resume(result) }
            continuation.invokeOnCancellation { main.post { session.destroy() } }
            session.start()
        }
    }

    private inner class Session(
        private val link: String,
        private val onAttempt: (Int) -> Unit,
        private val finish: (ParseResult) -> Unit,
    ) {
        private var webView: WebView? = null
        private var attempt = 0
        private var done = false
        private val timeout = Runnable { complete(ParseResult.Failure(ParseFailure.Timeout)) }
        private val retry = Runnable { read() }

        @SuppressLint("SetJavaScriptEnabled") // The page is a single-page app; nothing renders without scripts.
        fun start() {
            val view = WebView(appContext)
            webView = view
            // No cookies or storage may carry a Google session into the scrape, or out of it.
            CookieManager.getInstance().apply {
                removeAllCookies(null)
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(view, false)
            }
            view.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = USER_AGENT
                allowFileAccess = false
                allowContentAccess = false
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                safeBrowsingEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                setGeolocationEnabled(false)
            }
            view.webViewClient = client()
            // A real size lets the page lay itself out, so the text it reports is the text a reader sees.
            view.measure(WIDTH_SPEC, HEIGHT_SPEC)
            view.layout(0, 0, VIEW_WIDTH, VIEW_HEIGHT)
            main.postDelayed(timeout, TIMEOUT_MS)
            view.loadUrl(link)
        }

        private fun client() = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Redirects are followed only inside the allowed hosts; anything else is not opened.
                if (GeminiChat.isAllowedNavigation(request.url.toString())) return false
                complete(ParseResult.Failure(ParseFailure.Blocked))
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (!GeminiChat.isAllowedNavigation(url)) complete(ParseResult.Failure(ParseFailure.Blocked))
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!done && attempt == 0) read()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) complete(ParseResult.Failure(ParseFailure.Network))
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (request.isForMainFrame) {
                    val notFound = errorResponse.statusCode == HTTP_NOT_FOUND || errorResponse.statusCode == HTTP_GONE
                    complete(ParseResult.Failure(if (notFound) ParseFailure.NotFound else ParseFailure.Network))
                }
            }

            // A broken certificate is never accepted.
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                complete(ParseResult.Failure(ParseFailure.Network))
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                complete(ParseResult.Failure(ParseFailure.Crashed))
                return true
            }
        }

        /** Runs the script; an empty answer schedules the next run, up to [MAX_ATTEMPTS] runs in all. */
        private fun read() {
            val view = webView?.takeIf { !done } ?: return
            attempt++
            onAttempt(attempt)
            view.evaluateJavascript(SCRIPT) { raw ->
                if (done) return@evaluateJavascript
                val turns = GeminiChat.parseTurns(raw).orEmpty()
                when {
                    turns.isNotEmpty() -> complete(ParseResult.Success(turns))
                    attempt >= MAX_ATTEMPTS -> complete(ParseResult.Failure(ParseFailure.Empty))
                    else -> main.postDelayed(retry, RETRY_DELAY_MS)
                }
            }
        }

        private fun complete(result: ParseResult) {
            if (done) return
            destroy()
            finish(result)
        }

        fun destroy() {
            done = true
            main.removeCallbacks(timeout)
            main.removeCallbacks(retry)
            webView?.apply {
                stopLoading()
                webViewClient = WebViewClient()
                clearCache(true)
                clearHistory()
                destroy()
            }
            webView = null
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 4
        private const val RETRY_DELAY_MS = 1_500L
        private const val TIMEOUT_MS = 45_000L
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_GONE = 410
        private const val VIEW_WIDTH = 1080
        private const val VIEW_HEIGHT = 1920
        private val WIDTH_SPEC = android.view.View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH, android.view.View.MeasureSpec.EXACTLY)
        private val HEIGHT_SPEC = android.view.View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT, android.view.View.MeasureSpec.EXACTLY)

        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/138.0.0.0 Mobile Safari/537.36"

        /** Reads every question and answer in page order; the page's own text is only ever read, never run. */
        private const val SCRIPT = """
            (function() {
              let data = [];
              let turns = document.querySelectorAll('user-query, model-response');
              turns.forEach(el => {
                let isUser = el.tagName.toLowerCase() === 'user-query';
                data.push({
                  role: isUser ? 'User' : 'Model',
                  text: el.innerText.trim()
                });
              });
              return JSON.stringify(data);
            })();
        """
    }
}
