package cc.kesardev.scparchive.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryTab {
    FAVORITES,
    HISTORY
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LibraryDatabase(application)

    private val _favorites = MutableLiveData<List<LibraryEntry>>(emptyList())
    val favorites: LiveData<List<LibraryEntry>> = _favorites

    private val _history = MutableLiveData<List<LibraryEntry>>(emptyList())
    val history: LiveData<List<LibraryEntry>> = _history

    fun refresh() {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                database.favorites() to database.history()
            }
            _favorites.postValue(data.first)
            _history.postValue(data.second)
        }
    }

    fun remove(tab: LibraryTab, slug: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (tab == LibraryTab.FAVORITES) {
                    database.removeFavorite(slug)
                } else {
                    database.removeHistory(slug)
                }
            }
            refresh()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.clearHistory() }
            refresh()
        }
    }
}