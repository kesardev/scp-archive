package cc.kesardev.scparchive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cc.kesardev.scparchive.data.ArticleRepository
import cc.kesardev.scparchive.reader.ReaderAssets
import cc.kesardev.scparchive.reader.ReaderShellBuilder
import cc.kesardev.scparchive.reader.ReaderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ReaderState {
    data object Loading : ReaderState

    data class Ready(
        val html: String,
        val title: String,
        val stats: ReaderShellBuilder.Stats,
        val source: String,
        val theme: ReaderTheme
    ) : ReaderState

    data class Error(val message: String) : ReaderState
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArticleRepository(application)
    private val assets = ReaderAssets.from(application)

    private val _state = MutableLiveData<ReaderState>(ReaderState.Loading)
    val state: LiveData<ReaderState> = _state

    fun load(
        slug: String,
        theme: ReaderTheme = ReaderTheme.LIGHT,
        forceNetwork: Boolean = false
    ) {
        viewModelScope.launch {
            _state.value = ReaderState.Loading
            val page = try {
                repository.load(slug, forceNetwork)
            } catch (error: Exception) {
                _state.postValue(ReaderState.Error(error.message ?: "加载失败"))
                return@launch
            }
            val built = try {
                withContext(Dispatchers.Default) {
                    ReaderShellBuilder.build(
                        page.html,
                        ReaderShellBuilder.BASE_URL,
                        assets,
                        theme
                    )
                }
            } catch (error: Exception) {
                _state.postValue(ReaderState.Error("解析失败：" + (error.message ?: "unknown")))
                return@launch
            }
            _state.postValue(
                ReaderState.Ready(
                    built.html,
                    built.title,
                    built.stats,
                    page.source,
                    theme
                )
            )
        }
    }
}
