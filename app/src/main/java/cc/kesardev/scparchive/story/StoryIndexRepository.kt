package cc.kesardev.scparchive.story

import android.content.Context
import cc.kesardev.scparchive.data.NetworkClient
import cc.kesardev.scparchive.data.RequestRateLimiter
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

data class StoryIndexPage(
    val groups: List<StoryGroup>,
    val source: String
)

class StoryIndexRepository(private val context: Context) {

    private val cache = StoryIndexCache(context)
    private val client get() = NetworkClient.get(context)

    suspend fun load(
        path: String,
        mode: StoryIndexMode,
        force: Boolean = false
    ): StoryIndexPage =
        withContext(Dispatchers.IO) {
            val cached = cache.read(path, mode)
            if (!force && cached != null && cache.isFresh(cached)) {
                return@withContext StoryIndexPage(cached.groups, "缓存")
            }
            try {
                val groups = StoryIndexParser.parse(fetch(path), mode)
                if (groups.isEmpty()) {
                    throw IOException("这个索引页没有条目")
                }
                cache.write(path, mode, groups)
                StoryIndexPage(groups, "在线")
            } catch (error: Exception) {
                if (cached != null && cached.groups.isNotEmpty()) {
                    StoryIndexPage(cached.groups, "离线缓存")
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
            .header("User-Agent", "SCPArchive/3.0 (Android reader)")
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