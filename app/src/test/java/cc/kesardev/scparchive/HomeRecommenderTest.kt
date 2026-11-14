package cc.kesardev.scparchive

import cc.kesardev.scparchive.home.HomeArticle
import cc.kesardev.scparchive.home.HomeHeat
import cc.kesardev.scparchive.home.HomeRecommender
import cc.kesardev.scparchive.home.RecommendMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRecommenderTest {

    @Test
    fun combinesRatingAndLocalHeat() {
        val now = System.currentTimeMillis()
        val rating = listOf(
            HomeArticle("scp-100", "High rating", rating = 1000, rank = 1),
            HomeArticle("scp-200", "Middle rating", rating = 500, rank = 2),
            HomeArticle("scp-300", "Low rating", rating = 100, rank = 3)
        )
        val heat = listOf(
            HomeHeat(
                slug = "scp-300",
                title = "Low rating",
                readCount = 20,
                lastReadAt = now,
                favorite = true
            )
        )

        val result = HomeRecommender.recommend(
            ratingPool = rating,
            heat = heat,
            dailySlug = null,
            excludeSlugs = emptySet(),
            limit = 2,
            now = now
        )

        assertEquals(2, result.size)
        assertTrue(result.any { it.slug == "scp-100" })
        assertTrue(result.any { it.slug == "scp-300" })
        assertFalse(result.any { it.slug == "scp-200" })
        assertTrue(result.first { it.slug == "scp-300" }.subtitle.contains("阅读 20 次"))
    }

    @Test
    fun excludesSectionsAlreadyOnHome() {
        val result = HomeRecommender.recommend(
            ratingPool = listOf(
                HomeArticle("scp-100", "A", rating = 1000),
                HomeArticle("scp-200", "B", rating = 900),
                HomeArticle("scp-300", "C", rating = 800)
            ),
            heat = emptyList(),
            dailySlug = "scp-100",
            excludeSlugs = setOf("scp-200"),
            limit = 5
        )

        assertEquals(listOf("scp-300"), result.map { it.slug })
    }

    @Test
    fun fillsFromCuratedFallbackWhenPoolIsEmpty() {
        val fallback = listOf(
            HomeArticle("scp-173", "SCP-173"),
            HomeArticle("scp-682", "SCP-682")
        )

        val result = HomeRecommender.recommend(
            ratingPool = emptyList(),
            heat = emptyList(),
            dailySlug = null,
            excludeSlugs = emptySet(),
            fallback = fallback,
            limit = 2
        )

        assertEquals(listOf("scp-173", "scp-682"), result.map { it.slug })
    }

    @Test
    fun ratingModeOrdersByWikiScoreOnly() {
        val now = System.currentTimeMillis()
        val rating = listOf(
            HomeArticle("scp-100", "High rating", rating = 1000),
            HomeArticle("scp-200", "Low rating", rating = 100)
        )
        val heat = listOf(
            HomeHeat("scp-200", "Low rating", readCount = 30, lastReadAt = now, favorite = true)
        )

        val result = HomeRecommender.recommend(
            ratingPool = rating,
            heat = heat,
            dailySlug = null,
            excludeSlugs = emptySet(),
            limit = 2,
            mode = RecommendMode.RATING,
            now = now
        )

        assertEquals(listOf("scp-100", "scp-200"), result.map { it.slug })
    }

    @Test
    fun heatModeOrdersByLocalActivityOnly() {
        val now = System.currentTimeMillis()
        val rating = listOf(
            HomeArticle("scp-100", "High rating", rating = 1000),
            HomeArticle("scp-200", "Low rating", rating = 100)
        )
        val heat = listOf(
            HomeHeat("scp-200", "Low rating", readCount = 30, lastReadAt = now, favorite = true)
        )

        val result = HomeRecommender.recommend(
            ratingPool = rating,
            heat = heat,
            dailySlug = null,
            excludeSlugs = emptySet(),
            limit = 2,
            mode = RecommendMode.HEAT,
            now = now
        )

        assertEquals(listOf("scp-200", "scp-100"), result.map { it.slug })
        assertTrue(result.first().subtitle.startsWith("阅读 30 次"))
    }

    @Test
    fun batchesRotateThroughTheRankedPool() {
        val pool = (1..8).map { index ->
            HomeArticle("scp-" + index, "Entry " + index, rating = 1000 - index * 10)
        }

        val first = HomeRecommender.recommend(
            ratingPool = pool,
            heat = emptyList(),
            dailySlug = null,
            excludeSlugs = emptySet(),
            limit = 6,
            batch = 0
        )
        val second = HomeRecommender.recommend(
            ratingPool = pool,
            heat = emptyList(),
            dailySlug = null,
            excludeSlugs = emptySet(),
            limit = 6,
            batch = 1
        )

        assertEquals((1..6).map { "scp-" + it }, first.map { it.slug })
        assertEquals(listOf("scp-7", "scp-8", "scp-1", "scp-2", "scp-3", "scp-4"), second.map { it.slug })
    }
    @Test
    fun alternatesCnOriginalsAndMainSeries() {
        val cnPool = (1..6).map { index ->
            HomeArticle("scp-cn-" + index, "CN " + index, rating = 6000 - index * 10)
        }
        val scpPool = (1..6).map { index ->
            HomeArticle("scp-" + index, "SCP " + index, rating = 12000 - index * 10)
        }

        val result = HomeRecommender.recommend(
            ratingPool = cnPool,
            heat = emptyList(),
            dailySlug = null,
            excludeSlugs = emptySet(),
            scpPool = scpPool,
            limit = 6
        )

        val cnCount = result.count { it.slug.startsWith("scp-cn-") }
        assertEquals(3, cnCount)
        assertEquals(3, result.size - cnCount)
        assertEquals("scp-cn-1", result[0].slug)
        assertEquals("scp-1", result[1].slug)
    }

    @Test
    fun keepsOrderWhenOnlyOneSeriesIsAvailable() {
        val cnPool = (1..4).map { index ->
            HomeArticle("scp-cn-" + index, "CN " + index, rating = 900 - index * 10)
        }

        val result = HomeRecommender.recommend(
            ratingPool = cnPool,
            heat = emptyList(),
            dailySlug = null,
            excludeSlugs = emptySet(),
            scpPool = emptyList(),
            limit = 6
        )

        assertEquals(listOf("scp-cn-1", "scp-cn-2", "scp-cn-3", "scp-cn-4"), result.map { it.slug })
    }}