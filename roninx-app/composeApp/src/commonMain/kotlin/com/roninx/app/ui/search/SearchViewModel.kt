package com.roninx.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.shared.models.Anime
import com.roninx.shared.network.AniListApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val aniListApi = AniListApi()
    
    private val _searchResults = MutableStateFlow<List<Anime>>(emptyList())
    val searchResults: StateFlow<List<Anime>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    fun search(term: String) {
        if (term.isBlank()) return
        
        viewModelScope.launch {
            _isSearching.value = true
            try {
                _searchResults.value = aniListApi.searchAnime(term)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isSearching.value = false
            }
        }
    }
}
