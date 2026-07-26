package com.roninx.anime.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.data.repository.AnimeRepository
import com.roninx.anime.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: AnimeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val animeId: Int = checkNotNull(savedStateHandle["animeId"])

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState

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
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val anime: JikanAnime) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
