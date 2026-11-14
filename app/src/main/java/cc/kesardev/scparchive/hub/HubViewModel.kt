package cc.kesardev.scparchive.hub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface HubState {
    data object Loading : HubState

    data class Ready(
        val entries: List<HubEntry>,
        val source: String
    ) : HubState

    data class Error(val message: String) : HubState
}

class HubViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HubRepository(application)

    private val _state = MutableLiveData<HubState>(HubState.Loading)
    val state: LiveData<HubState> = _state

    fun load(path: String, mode: HubMode, force: Boolean = false) {
        viewModelScope.launch {
            _state.value = HubState.Loading
            try {
                val page = repository.load(path, mode, force)
                _state.postValue(HubState.Ready(page.entries, page.source))
            } catch (error: Exception) {
                _state.postValue(HubState.Error(error.message ?: "中心页加载失败"))
            }
        }
    }
}