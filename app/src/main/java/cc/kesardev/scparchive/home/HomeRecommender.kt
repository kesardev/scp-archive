package cc.kesardev.scparchive.home

import kotlin.math.ln

/**
 * Combines the wiki rating pool with local reading heat. Rating is normalized
 * against the strongest candidate; heat uses read count, recency and favorites.
 * Entries already shown in the daily, continue-reading, top-rated or recent
 * sections are excluded to keep the home page varied.
 */
object HomeRecommender {

    const val DEFAULT_LIMIT = 6

    /**
     * Ranks every candidate for the selected mode. The result is the full pool,
     * so the home page can page through it batch by batch.
     */
    fun rank(
        ratingPool: List<HomeArticle>,
        heat: List<HomeHeat>,
        dailySlug: String?,
        excludeSlugs: Set<String>,
        fallback: List<HomeArticle> = emptyList(),
        scpPool: List<HomeArticle> = emptyList(),
        mode: RecommendMode = RecommendMode.MIXED,
        now: Long = System.currentTimeMillis()
    ): List<HomeArticle> {
        val heatBySlug = heat.associateBy { it.slug }
        val allRated = ratingPool + scpPool
        val maxRating = allRated.maxOfOrNull { (it.rating ?: 0).coerceAtLeast(1) }
            ?.coerceAtLeast(1) ?: 1
        val maxReads = heat.maxOfOrNull { it.readCount }?.coerceAtLeast(1) ?: 1

        val candidates = LinkedHashMap<String, HomeArticle>()
        for (article in ratingPool) {
            if (article.slug == dailySlug || article.slug in excludeSlugs) {
                continue
            }
            candidates[article.slug] = article
        }
        for (article in scpPool) {
            if (article.slug == dailySlug || article.slug in excludeSlugs) {
                continue
            }
            candidates[article.slug] = article
        }
        for (item in heat) {
            if (item.slug == dailySlug || item.slug in excludeSlugs) {
                continue
            }
            if (!candidates.containsKey(item.slug)) {
                candidates[item.slug] = HomeArticle(item.slug, item.title)
            }
        }

        val scored = candidates.values.map { article ->
            val ratingScore = ((article.rating ?: 0).coerceAtLeast(0).toDouble() / maxRating)
                .coerceIn(0.0, 1.0)
            val item = heatBySlug[article.slug]
            val readScore = if (item == null || item.readCount <= 0) {
                0.0
            } else {
                ln(1.0 + item.readCount) / ln(1.0 + maxReads)
            }
            val recency = if (item == null || item.lastReadAt <= 0L) {
                0.0
            } else {
                val days = ((now - item.lastReadAt).coerceAtLeast(0L) / 86_400_000.0)
                1.0 / (1.0 + days / 30.0)
            }
            val favorite = if (item?.favorite == true) 1.0 else 0.0
            val heatScore = 0.55 * readScore + 0.30 * recency + 0.15 * favorite
            val score = when (mode) {
                RecommendMode.RATING -> ratingScore
                RecommendMode.HEAT -> heatScore
                RecommendMode.MIXED -> 0.55 * ratingScore + 0.45 * heatScore
            }
            Scored(article, score, heatScore, ratingScore, item)
        }

        val comparator = when (mode) {
            RecommendMode.RATING -> compareByDescending<Scored> { it.score }
                .thenByDescending { it.heatScore }

            RecommendMode.HEAT -> compareByDescending<Scored> { it.score }
                .thenByDescending { it.ratingScore }

            RecommendMode.MIXED -> compareByDescending<Scored> { it.score }
                .thenByDescending { it.article.rating ?: 0 }
        }

        val sorted = ArrayList<HomeArticle>(scored.size)
        val used = HashSet<String>()
        for (item in scored.sortedWith(comparator)) {
            if (used.add(item.article.slug)) {
                sorted.add(withMeta(item.article, item.heat, mode))
            }
        }
        val ranked = ArrayList<HomeArticle>(sorted.size + fallback.size)
        ranked.addAll(interleave(sorted))
        for (item in fallback) {
            if (item.slug == dailySlug || item.slug in excludeSlugs || !used.add(item.slug)) {
                continue
            }
            ranked.add(item.copy(subtitle = "精选"))
        }
        return ranked
    }

    private val MAIN_SERIES = Regex("^scp-\\d+(?:-[a-z0-9]+)*$", RegexOption.IGNORE_CASE)

    /**
     * Alternates CN originals and main series entries so the home page shows
     * both halves instead of a single wall of SCP-CN picks.
     */
    private fun interleave(ranked: List<HomeArticle>): List<HomeArticle> {
        if (ranked.isEmpty()) {
            return ranked
        }
        val cn = ranked.filter { it.slug.startsWith("scp-cn-") }
        val scp = ranked.filter { MAIN_SERIES.matches(it.slug) }
        if (cn.isEmpty() || scp.isEmpty()) {
            return ranked
        }
        val rest = ranked.filter {
            !it.slug.startsWith("scp-cn-") && !MAIN_SERIES.matches(it.slug)
        }
        val out = ArrayList<HomeArticle>(ranked.size)
        var index = 0
        while (index < cn.size || index < scp.size) {
            if (index < cn.size) {
                out.add(cn[index])
            }
            if (index < scp.size) {
                out.add(scp[index])
            }
            index++
        }
        out.addAll(rest)
        return out
    }
    /** Takes one batch from a ranked list, wrapping around at the end. */
    fun window(
        ranked: List<HomeArticle>,
        offset: Int,
        limit: Int = DEFAULT_LIMIT
    ): List<HomeArticle> {
        if (ranked.isEmpty()) {
            return emptyList()
        }
        if (ranked.size <= limit) {
            return ranked
        }
        val start = ((offset % ranked.size) + ranked.size) % ranked.size
        val result = ArrayList<HomeArticle>(limit)
        for (index in 0 until limit) {
            result.add(ranked[(start + index) % ranked.size])
        }
        return result
    }

    fun recommend(
        ratingPool: List<HomeArticle>,
        heat: List<HomeHeat>,
        dailySlug: String?,
        excludeSlugs: Set<String>,
        fallback: List<HomeArticle> = emptyList(),
        limit: Int = DEFAULT_LIMIT,
        mode: RecommendMode = RecommendMode.MIXED,
        batch: Int = 0,
        scpPool: List<HomeArticle> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): List<HomeArticle> {
        val ranked = rank(
            ratingPool = ratingPool,
            heat = heat,
            dailySlug = dailySlug,
            excludeSlugs = excludeSlugs,
            fallback = fallback,
            scpPool = scpPool,
            mode = mode,
            now = now
        )
        return window(ranked, batch * limit, limit)
    }

    private fun withMeta(
        article: HomeArticle,
        heat: HomeHeat?,
        mode: RecommendMode
    ): HomeArticle {
        val parts = ArrayList<String>(3)
        val rating = article.rating ?: 0
        val ratingPart = if (rating > 0) "评分 +" + rating else null
        val readPart = if (heat != null && heat.readCount > 0) {
            "阅读 " + heat.readCount + " 次"
        } else {
            null
        }
        val favoritePart = if (heat?.favorite == true) "已收藏" else null

        if (mode == RecommendMode.HEAT) {
            readPart?.let { parts.add(it) }
            favoritePart?.let { parts.add(it) }
            ratingPart?.let { parts.add(it) }
        } else {
            ratingPart?.let { parts.add(it) }
            readPart?.let { parts.add(it) }
            favoritePart?.let { parts.add(it) }
        }
        if (parts.isEmpty()) {
            parts.add("动态精选")
        }
        return article.copy(subtitle = parts.joinToString(" · "))
    }

    private data class Scored(
        val article: HomeArticle,
        val score: Double,
        val heatScore: Double,
        val ratingScore: Double,
        val heat: HomeHeat?
    )
}