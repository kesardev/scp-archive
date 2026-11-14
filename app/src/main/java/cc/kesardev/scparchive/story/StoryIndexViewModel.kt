package cc.kesardev.scparchive.story

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface StoryIndexState {
    data object Loading : StoryIndexState

    data class Ready(
        val groups: List<StoryGroup>,
        val source: String
    ) : StoryIndexState

    data class Error(val message: String) : StoryIndexState
}

class StoryIndexViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StoryIndexRepository(application)
    private val _state = MutableLiveData<StoryIndexState>(StoryIndexState.Loading)
    val state: LiveData<StoryIndexState> = _state

    fun load(path: String, mode: StoryIndexMode, force: Boolean = false) {
        viewModelScope.launch {
            _state.value = StoryIndexState.Loading
            try {
                val page = repository.load(path, mode, force)
                _state.postValue(StoryIndexState.Ready(page.groups, page.source))
            } catch (error: Exception) {
                _state.postValue(StoryIndexState.Error(error.message ?: "索引加载失败"))
            }
        }
    }
}