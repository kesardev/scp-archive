package cc.kesardev.scparchive.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface CatalogState {
    data object Loading : CatalogState

    data class Ready(
        val series: SeriesInfo,
        val entries: List<CatalogEntry>,
        val source: String
    ) : CatalogState

    data class Error(val message: String) : CatalogState
}

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CatalogRepository(application)
    private val _state = MutableLiveData<CatalogState>(CatalogState.Loading)
    val state: LiveData<CatalogState> = _state

    private val _syncing = MutableLiveData(false)
    val syncing: LiveData<Boolean> = _syncing

    private val _syncMessage = MutableLiveData("")
    val syncMessage: LiveData<String> = _syncMessage

    fun syncAll() {
        if (_syncing.value == true) {
            return
        }
        _syncing.value = true
        viewModelScope.launch {
            val total = repository.syncAll { position, count, name ->
                _syncMessage.postValue("同步 " + position + "/" + count + "：" + name)
            }
            _syncMessage.postValue("已索引 " + total + " 条 · 同步完成")
            _syncing.value = false
        }
    }

    fun load(series: SeriesInfo, force: Boolean = false) {
        viewModelScope.launch {
            _state.value = CatalogState.Loading
            try {
                val page = repository.load(series, force)
                _state.postValue(CatalogState.Ready(series, page.entries, page.source))
            } catch (error: Exception) {
                _state.postValue(CatalogState.Error(error.message ?: "目录加载失败"))
            }
        }
    }
}
