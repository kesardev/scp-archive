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

    private var latestHeat: List<HomeHeat> = emptyList()

    private var mode: RecommendMode = RecommendMode.MIXED

    private var batch: Int = 0

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
                topRated = previous.topRated,
                topRatedSource = previous.topRatedSource,
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
                val remoteState = current.copy(
                    topRated = remote.topRated.take(10),
                    topRatedSource = remote.topRatedSource,
                    recent = remote.recent,
                    recentSource = remote.recentSource,
                    remoteLoading = false,
                    recommendMode = mode
                )
                remoteState.copy(recommended = recommend(remoteState))
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
                topRated = current.topRated,
                topRatedSource = current.topRatedSource,
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

    private fun recommend(state: HomeUiState): List<HomeArticle> {
        val ranked = HomeRecommender.rank(
            ratingPool = ratingPool,
            heat = latestHeat,
            dailySlug = state.daily?.slug,
            excludeSlugs = excludedSlugs(state, state.topRated, state.recent),
            fallback = fallbackPicks(),
            mode = mode
        )
        return HomeRecommender.window(ranked, batch * HomeRecommender.DEFAULT_LIMIT)
    }

    private fun excludedSlugs(
        state: HomeUiState,
        topRated: List<HomeArticle>,
        recent: List<HomeArticle>
    ): Set<String> {
        val slugs = HashSet<String>()
        state.daily?.let { slugs.add(it.slug) }
        state.continueReading.forEach { slugs.add(it.slug) }
        topRated.forEach { slugs.add(it.slug) }
        recent.forEach { slugs.add(it.slug) }
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