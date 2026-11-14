package cc.kesardev.scparchive.home

/**
 * Picks the daily hero entry from the rating pools. The day decides which
 * position is used, while every refresh flips between CN originals and main
 * series entries so both keep showing up.
 */
object DailySelector {

    fun pick(
        cnPool: List<HomeArticle>,
        mainPool: List<HomeArticle>,
        refresh: Int,
        epochDay: Long
    ): HomeArticle? {
        val cn = cnPool.filter { it.slug.startsWith("scp-cn-") }
        if (cn.isEmpty() && mainPool.isEmpty()) {
            return null
        }
        val preferCn = refresh % 2 == 0
        var pool = if (preferCn) cn else mainPool
        var isCn = preferCn
        if (pool.isEmpty()) {
            pool = if (preferCn) mainPool else cn
            isCn = !preferCn
        }
        if (pool.isEmpty()) {
            return null
        }
        val index = ((epochDay + refresh / 2) % pool.size).toInt()
        val article = pool[index]
        val series = if (isCn) "中文分部原创" else "基金会本部"
        return article.copy(
            subtitle = "评分 +" + (article.rating ?: 0) + " · " + series,
            tagline = ""
        )
    }
}
