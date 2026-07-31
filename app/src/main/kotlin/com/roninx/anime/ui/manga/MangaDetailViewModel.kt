package com.roninx.anime.ui.manga

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.local.dao.AnimeDao
import com.roninx.anime.data.local.entities.MangaHistoryEntity
import com.roninx.anime.data.repository.AniListRepository
import com.roninx.anime.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaDetailViewModel @Inject constructor(
    private val aniListRepository: AniListRepository,
    private val animeDao: AnimeDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val mangaId: Int = checkNotNull(savedStateHandle["mangaId"])

    private val _uiState = MutableStateFlow<MangaDetailUiState>(MangaDetailUiState.Loading)
    val uiState: StateFlow<MangaDetailUiState> = _uiState

    val readingHistory: StateFlow<MangaHistoryEntity?> = animeDao.getMangaHistoryItem(mangaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        fetchMangaDetail()
    }

    fun fetchMangaDetail() {
        viewModelScope.launch {
            _uiState.value = MangaDetailUiState.Loading
            when (val res = aniListRepository.getMangaDetails(mangaId)) {
                is Resource.Success -> {
                    _uiState.value = MangaDetailUiState.Success(res.data)
                }
                is Resource.Error -> {
                    _uiState.value = MangaDetailUiState.Error(res.message)
                }
                else -> {}
            }
        }
    }
}

sealed class MangaDetailUiState {
    object Loading : MangaDetailUiState()
    data class Success(val manga: AniListMedia) : MangaDetailUiState()
    data class Error(val message: String) : MangaDetailUiState()
}
