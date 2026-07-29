package com.roninx.anime.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.data.local.entities.WatchlistEntity
import com.roninx.anime.data.repository.AnimeRepository
import com.roninx.anime.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: AnimeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val animeId: Int = checkNotNull(savedStateHandle["animeId"])

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState

    val isInWatchlist: StateFlow<Boolean> = repository.isInWatchlist(animeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        fetchAnimeDetail()
    }

    fun fetchAnimeDetail() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            when (val resource = repository.getAnimeFull(animeId)) {
                is Resource.Success -> {
                    _uiState.value = DetailUiState.Success(resource.data)
                }
                is Resource.Error -> {
                    _uiState.value = DetailUiState.Error(resource.message)
                }
                else -> {}
            }
        }
    }

    fun toggleWatchlist() {
        val currentState = uiState.value
        if (currentState is DetailUiState.Success) {
            val anime = currentState.anime
            viewModelScope.launch {
                if (isInWatchlist.value) {
                    repository.removeFromWatchlist(anime.mal_id)
                } else {
                    repository.addToWatchlist(
                        WatchlistEntity(
                            malId = anime.mal_id,
                            title = anime.title_english ?: anime.title,
                            imageUrl = anime.images.jpg.large_image_url,
                            score = anime.score?.toString() ?: "N/A",
                            episodes = anime.episodes
                        )
                    )
                }
            }
        }
    }
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val anime: JikanAnime) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
