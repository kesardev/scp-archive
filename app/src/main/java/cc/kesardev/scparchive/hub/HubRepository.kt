package cc.kesardev.scparchive.hub

import android.content.Context
import cc.kesardev.scparchive.data.NetworkClient
import cc.kesardev.scparchive.data.RequestRateLimiter
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

data class HubPage(
    val entries: List<HubEntry>,
    val source: String
)

class HubRepository(private val context: Context) {

    private val client get() = NetworkClient.get(context)

    private val cache = HubCache(context)


    suspend fun load(
        path: String,
        mode: HubMode,
        force: Boolean = false
    ): HubPage = withContext(Dispatchers.IO) {
        val cached = cache.read(path, mode)
        if (!force && cached != null && cache.isFresh(cached)) {
            return@withContext HubPage(cached.entries, "缓存")
        }

        try {
            val entries = HubParser.parse(fetch(path), mode)
            if (entries.isEmpty()) {
                throw IOException("这个中心页没有可列出的条目")
            }
            cache.write(path, mode, entries)
            HubPage(entries, "在线")
        } catch (error: Exception) {
            if (cached != null && cached.entries.isNotEmpty()) {
                HubPage(cached.entries, "离线缓存")
            } else {
                throw error
            }
        }
    }

    private suspend fun fetch(path: String): String {
        var last: IOException? = null
        repeat(2) { attempt ->
            try {
                return fetchOnce(path)
            } catch (error: IOException) {
                last = error
                if (attempt == 0) {
                    delay(1_500L)
                }
            }
        }
        throw last ?: IOException("empty response")
    }

    private suspend fun fetchOnce(path: String): String {
        RequestRateLimiter.await()
        val request = Request.Builder()
            .url(ReaderShellBuilder.BASE_URL + path)
            .header("User-Agent", "SCPArchive/2.5 (Android reader)")
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP " + response.code)
            }
            return response.body?.string() ?: throw IOException("empty response")
        }
    }
}