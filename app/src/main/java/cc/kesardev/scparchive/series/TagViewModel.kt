package cc.kesardev.scparchive.series

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cc.kesardev.scparchive.catalog.CatalogEntry
import kotlinx.coroutines.launch

sealed interface TagState {
    data object Loading : TagState

    data class Ready(
        val entries: List<CatalogEntry>,
        val source: String
    ) : TagState

    data class Error(val message: String) : TagState
}

class TagViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TagRepository(application)
    private val _state = MutableLiveData<TagState>(TagState.Loading)
    val state: LiveData<TagState> = _state

    fun load(tag: String, force: Boolean = false) {
        viewModelScope.launch {
            _state.value = TagState.Loading
            try {
                val page = repository.load(tag, force)
                _state.postValue(TagState.Ready(page.entries, page.source))
            } catch (error: Exception) {
                _state.postValue(TagState.Error(error.message ?: "标签页加载失败"))
            }
        }
    }
}