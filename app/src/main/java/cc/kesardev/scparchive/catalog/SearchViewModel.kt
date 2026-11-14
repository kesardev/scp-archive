package cc.kesardev.scparchive.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CatalogDatabase(application)

    private val _results = MutableLiveData<List<CatalogEntry>>(emptyList())
    val results: LiveData<List<CatalogEntry>> = _results

    private val _indexCount = MutableLiveData(0)
    val indexCount: LiveData<Int> = _indexCount

    fun search(query: String) {
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) { database.search(query) }
            _results.postValue(found)
            _indexCount.postValue(withContext(Dispatchers.IO) { database.count() })
        }
    }

    fun refreshCount() {
        viewModelScope.launch {
            _indexCount.postValue(withContext(Dispatchers.IO) { database.count() })
        }
    }
}
