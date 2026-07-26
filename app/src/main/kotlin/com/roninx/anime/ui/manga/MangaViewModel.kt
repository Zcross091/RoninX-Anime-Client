package com.roninx.anime.ui.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.repository.AniListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaViewModel @Inject constructor(
    private val aniListRepository: AniListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MangaUiState>(MangaUiState.Loading)
    val uiState: StateFlow<MangaUiState> = _uiState

    init {
        fetchMangaData()
    }

    private fun fetchMangaData() {
        viewModelScope.launch {
            try {
                val trending = aniListRepository.getTrendingManga()
                val popular = aniListRepository.getPopularManga()
                
                _uiState.value = MangaUiState.Success(
                    trendingManga = trending,
                    popularManga = popular
                )
            } catch (e: Exception) {
                _uiState.value = MangaUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class MangaUiState {
    object Loading : MangaUiState()
    data class Success(
        val trendingManga: List<AniListMedia>,
        val popularManga: List<AniListMedia>
    ) : MangaUiState()
    data class Error(val message: String) : MangaUiState()
}
