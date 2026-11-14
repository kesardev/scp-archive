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

    private val _entries = MutableLiveData<List<LibraryEntry>>(emptyList())
    val entries: LiveData<List<LibraryEntry>> = _entries

    private var currentTab = LibraryTab.FAVORITES

    fun load(tab: LibraryTab) {
        currentTab = tab
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                if (tab == LibraryTab.FAVORITES) database.favorites() else database.history()
            }
            _entries.postValue(data)
        }
    }

    fun refresh() {
        load(currentTab)
    }

    fun remove(slug: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (currentTab == LibraryTab.FAVORITES) {
                    database.removeFavorite(slug)
                } else {
                    database.removeHistory(slug)
                }
            }
            load(currentTab)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { database.clearHistory() }
            load(LibraryTab.HISTORY)
        }
    }
}
