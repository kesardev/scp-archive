package cc.kesardev.scparchive.reader

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Keeps wiki links inside the app and sends everything else to the browser.
 */
class ReaderWebViewClient(
    private val onInternal: (String) -> Unit,
    private val onExternal: (String) -> Unit,
    private val onInteractive: (String) -> Unit = {},
    private val pageFinished: (WebView, String) -> Unit = { _, _ -> }
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return handle(request.url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return handle(Uri.parse(url))
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        pageFinished(view, url)
    }

    private fun handle(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        if (scheme == "scparchive") {
            if (uri.host == "interactive") {
                val url = uri.getQueryParameter("url")
                if (!url.isNullOrBlank()) {
                    onInteractive(url)
                }
            }
            return true
        }
        if (scheme != "http" && scheme != "https") {
            return true
        }
        val host = uri.host?.lowercase()
        if (host == "scp-wiki-cn.wikidot.com" || host == "scp-wiki.wikidot.com") {
            val path = uri.path?.trim('/') ?: return true
            if (path.isNotEmpty()) {
                if (path.contains("/html/")) {
                    onInteractive(uri.toString())
                    return true
                }
                onInternal(path)
            }
            return true
        }
        onExternal(uri.toString())
        return true
    }
}
