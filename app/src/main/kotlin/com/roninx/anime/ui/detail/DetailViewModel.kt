package com.roninx.anime.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.data.local.entities.WatchlistEntity
import com.roninx.anime.data.repository.AniListRepository
import com.roninx.anime.data.repository.AnimeRepository
import com.roninx.anime.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: AnimeRepository,
    private val aniListRepository: AniListRepository,
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
            
            val jikanDeferred = async { repository.getAnimeFull(animeId) }
            val aniListDeferred = async { aniListRepository.getAnimeDetails(animeId) }

            val jikanRes = jikanDeferred.await()
            val aniListRes = aniListDeferred.await()

            if (jikanRes is Resource.Success) {
                _uiState.value = DetailUiState.Success(
                    anime = jikanRes.data,
                    aniListDetails = if (aniListRes is Resource.Success) aniListRes.data else null
                )
            } else if (aniListRes is Resource.Success) {
                // Fallback: If Jikan fails but AniList works, use AniList data mapped to Jikan
                _uiState.value = DetailUiState.Success(
                    anime = aniListRes.data.toJikan(),
                    aniListDetails = aniListRes.data
                )
            } else {
                val errorMsg = (jikanRes as? Resource.Error)?.message ?: "Failed to load details"
                _uiState.value = DetailUiState.Error(errorMsg)
            }
        }
    }

    private fun AniListMedia.toJikan(): JikanAnime {
        return JikanAnime(
            mal_id = idMal ?: id,
            title = title?.romaji ?: title?.english ?: "Unknown",
            title_english = title?.english,
            images = com.roninx.anime.data.api.JikanImages(com.roninx.anime.data.api.JikanJpg(coverImage?.large ?: "")),
            episodes = episodes ?: chapters,
            score = (averageScore?.toDouble() ?: 0.0) / 10.0,
            synopsis = description
        )
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
    data class Success(
        val anime: JikanAnime,
        val aniListDetails: AniListMedia? = null
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
