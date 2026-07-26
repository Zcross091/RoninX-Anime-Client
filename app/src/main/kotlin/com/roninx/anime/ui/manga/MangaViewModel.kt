package com.roninx.anime.ui.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.repository.AniListRepository
import com.roninx.anime.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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

    fun fetchMangaData() {
        viewModelScope.launch {
            _uiState.value = MangaUiState.Loading
            
            val trendingDeferred = async { aniListRepository.getTrendingManga() }
            val popularDeferred = async { aniListRepository.getPopularManga() }

            val trendingRes = trendingDeferred.await()
            val popularRes = popularDeferred.await()

            if (trendingRes is Resource.Success && popularRes is Resource.Success) {
                _uiState.value = MangaUiState.Success(
                    trendingManga = trendingRes.data,
                    popularManga = popularRes.data
                )
            } else {
                val errorMsg = (trendingRes as? Resource.Error)?.message 
                    ?: (popularRes as? Resource.Error)?.message 
                    ?: "Failed to load Manga data"
                _uiState.value = MangaUiState.Error(errorMsg)
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
