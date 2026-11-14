package cc.kesardev.scparchive.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeRepository(application)

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
            val previous = _state.value ?: HomeUiState()
            _state.value = local.copy(
                random = previous.random,
                topRated = previous.topRated,
                topRatedSource = previous.topRatedSource,
                recent = previous.recent,
                recentSource = previous.recentSource,
                remoteLoading = true
            )
            loadRandom()
            val remote = try {
                repository.remoteSnapshot(force)
            } catch (_: Exception) {
                null
            }
            val current = _state.value ?: local
            _state.value = if (remote == null) {
                current.copy(remoteLoading = false)
            } else {
                current.copy(
                    topRated = remote.topRated,
                    topRatedSource = remote.topRatedSource,
                    recent = remote.recent,
                    recentSource = remote.recentSource,
                    remoteLoading = false
                )
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
            val current = _state.value ?: local
            _state.value = local.copy(
                random = current.random,
                topRated = current.topRated,
                topRatedSource = current.topRatedSource,
                recent = current.recent,
                recentSource = current.recentSource,
                remoteLoading = current.remoteLoading
            )
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