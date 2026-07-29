package com.roninx.anime.ui.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.local.entities.WatchHistoryEntity
import com.roninx.anime.data.local.entities.WatchlistEntity
import com.roninx.anime.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.getWatchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlist: StateFlow<List<WatchlistEntity>> = repository.getWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFromWatchlist(malId: Int) {
        viewModelScope.launch {
            repository.removeFromWatchlist(malId)
        }
    }

    fun deleteHistory(malId: Int) {
        viewModelScope.launch {
            repository.deleteWatchHistory(malId)
        }
    }
}
