package cc.kesardev.scparchive.reader

import android.content.Context

/**
 * Reader CSS/JS live in assets/reader. They are inlined into the WebView shell
 * so the document has no local file dependencies.
 */
class ReaderAssets private constructor(
    val css: String,
    val js: String
) {
    companion object {
        fun from(context: Context): ReaderAssets {
            val appContext = context.applicationContext
            return ReaderAssets(
                readAsset(appContext, "reader/reader.css"),
                readAsset(appContext, "reader/reader.js")
            )
        }

        fun forTest(css: String = "", js: String = ""): ReaderAssets = ReaderAssets(css, js)

        private fun readAsset(context: Context, path: String): String {
            return context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }
}
