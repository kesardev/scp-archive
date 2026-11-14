package cc.kesardev.scparchive.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeRepository(application)

    private var ratingPool: List<HomeArticle> = emptyList()

    private var scpPool: List<HomeArticle> = emptyList()

    private var latestHeat: List<HomeHeat> = emptyList()

    private var mode: RecommendMode = RecommendMode.MIXED

    private var batch: Int = 0

    private var dailyRefresh: Int = 0

    private val summaries = HashMap<String, String>()

    private val _state = MutableLiveData(HomeUiState())
    val state: LiveData<HomeUiState> = _state

    fun load(force: Boolean = false) {
        viewModelScope.launch {
            val local = try {
                repository.localSnapshot()
            } catch (_: Exception) {
                val pick = DailyPicks.forDay(DailyPicks.currentEpochDay())
                HomeUiState(
                    daily = HomeArticle(pick.slug, pick.name, "今日推荐", pick.tagline),
                    loading = false
                )
            }
            latestHeat = local.heat
            val previous = _state.value ?: HomeUiState()
            val localState = local.copy(
                random = previous.random,
                featured = previous.featured,
                featuredSource = previous.featuredSource,
                recent = previous.recent,
                recentSource = previous.recentSource,
                remoteLoading = true,
                recommendMode = mode
            )
            _state.value = localState.copy(recommended = recommend(localState))
            loadRandom()
            val remote = try {
                repository.remoteSnapshot(force)
            } catch (_: Exception) {
                null
            }
            val current = _state.value ?: localState
            _state.value = if (remote == null) {
                current.copy(remoteLoading = false, recommendMode = mode)
            } else {
                ratingPool = remote.topRated
                scpPool = remote.scpTopRated
                val remoteState = current.copy(
                    featured = remote.featured,
                    featuredSource = remote.featuredSource,
                    recent = remote.recent,
                    recentSource = remote.recentSource,
                    remoteLoading = false,
                    recommendMode = mode
                )
                applyDaily(remoteState.copy(recommended = recommend(remoteState)))
            }
        }
    }

    fun refreshLocal() {
        viewModelScope.launch {
            val local = try {
                repository.localSnapshot()
            } catch (_: Exception) {
                return@launch
            }
            latestHeat = local.heat
            val current = _state.value ?: local
            val merged = local.copy(
                random = current.random,
                featured = current.featured,
                featuredSource = current.featuredSource,
                recent = current.recent,
                recentSource = current.recentSource,
                remoteLoading = current.remoteLoading,
                recommendMode = mode
            )
            _state.value = merged.copy(recommended = recommend(merged))
        }
    }

    /** Switches the dynamic picks between rating, heat and the blended score. */
    fun setRecommendMode(next: RecommendMode) {
        if (mode == next) {
            return
        }
        mode = next
        batch = 0
        val current = _state.value ?: return
        val nextState = current.copy(recommendMode = mode)
        _state.value = nextState.copy(recommended = recommend(nextState))
    }

    /** Shows the next batch of the current ranking. */
    fun shuffleRecommended() {
        val current = _state.value ?: return
        batch += 1
        _state.value = current.copy(recommended = recommend(current))
    }

    /** Moves the daily pick to the other series, keeping CN and main series balanced. */
    fun shuffleDaily() {
        val current = _state.value ?: return
        dailyRefresh += 1
        _state.value = applyDaily(current)
    }

    private fun recommend(state: HomeUiState): List<HomeArticle> {
        val ranked = HomeRecommender.rank(
            ratingPool = ratingPool,
            heat = latestHeat,
            dailySlug = state.daily?.slug,
            excludeSlugs = excludedSlugs(state),
            fallback = fallbackPicks(),
            scpPool = scpPool,
            mode = mode
        )
        return HomeRecommender.window(ranked, batch * HomeRecommender.DEFAULT_LIMIT)
    }

    private fun applyDaily(state: HomeUiState): HomeUiState {
        val pick = pickDaily() ?: return state
        val summary = summaries[pick.slug]
        if (summary == null) {
            loadSummary(pick.slug, pick.title)
        }
        return state.copy(daily = pick.copy(tagline = summary.orEmpty()))
    }

    private fun pickDaily(): HomeArticle? {
        return DailySelector.pick(
            cnPool = ratingPool,
            mainPool = scpPool,
            refresh = dailyRefresh,
            epochDay = DailyPicks.currentEpochDay()
        )
    }
    private fun loadSummary(slug: String, title: String) {
        viewModelScope.launch {
            val summary = try {
                repository.dailySummary(slug, title)
            } catch (_: Exception) {
                ""
            }
            if (summary.isEmpty()) {
                return@launch
            }
            summaries[slug] = summary
            val current = _state.value ?: return@launch
            if (current.daily?.slug == slug) {
                _state.value = current.copy(daily = current.daily.copy(tagline = summary))
            }
        }
    }

    private fun excludedSlugs(state: HomeUiState): Set<String> {
        val slugs = HashSet<String>()
        state.daily?.let { slugs.add(it.slug) }
        state.continueReading.forEach { slugs.add(it.slug) }
        state.recent.forEach { slugs.add(it.slug) }
        return slugs
    }

    private fun fallbackPicks(): List<HomeArticle> {
        return DailyPicks.all.map {
            HomeArticle(it.slug, it.name, subtitle = "精选", tagline = it.tagline)
        }
    }

    fun random(onResult: (HomeArticle?) -> Unit) {
        viewModelScope.launch {
            val article = repository.randomArticle()
            if (article != null) {
                _state.value = _state.value?.copy(random = article)
            }
            onResult(article)
        }
    }

    private fun loadRandom() {
        viewModelScope.launch {
            val article = repository.randomArticle() ?: return@launch
            _state.value = _state.value?.copy(random = article)
        }
    }
}
