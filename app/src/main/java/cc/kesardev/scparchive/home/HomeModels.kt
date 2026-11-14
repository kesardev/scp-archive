package cc.kesardev.scparchive.home

data class HomeArticle(
    val slug: String,
    val title: String,
    val subtitle: String = "",
    val tagline: String = "",
    val progress: Float? = null,
    val rating: Int? = null,
    val rank: Int = 0,
    val timestamp: Long = 0L
)

data class HomeUiState(
    val daily: HomeArticle? = null,
    val recommended: List<HomeArticle> = emptyList(),
    val random: HomeArticle? = null,
    val continueReading: List<HomeArticle> = emptyList(),
    val topRated: List<HomeArticle> = emptyList(),
    val topRatedSource: String = "",
    val recent: List<HomeArticle> = emptyList(),
    val recentSource: String = "",
    val catalogCount: Int = 0,
    val favoriteCount: Int = 0,
    val historyCount: Int = 0,
    val loading: Boolean = true,
    val remoteLoading: Boolean = false
)

fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    if (timestamp <= 0L) {
        return ""
    }
    val delta = (now - timestamp).coerceAtLeast(0L)
    val minute = 60_000L
    val hour = 60L * minute
    val day = 24L * hour
    return when {
        delta < minute -> "刚刚"
        delta < hour -> (delta / minute).toString() + " 分钟前"
        delta < day -> (delta / hour).toString() + " 小时前"
        delta < 7L * day -> (delta / day).toString() + " 天前"
        else -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
            .format(java.util.Date(timestamp))
    }
}