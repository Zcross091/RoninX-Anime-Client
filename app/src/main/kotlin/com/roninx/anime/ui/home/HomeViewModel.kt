package com.roninx.anime.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.data.repository.AniListRepository
import com.roninx.anime.data.repository.AnimeRepository
import com.roninx.anime.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AnimeRepository,
    private val aniListRepository: AniListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        fetchHomeData()
    }

    fun fetchHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            val topAiringDeferred = async { repository.getTopAiring() }
            val actionDeferred = async { repository.getActionAnime() }
            val romanceDeferred = async { repository.getRomanceAnime() }

            val topAiringRes = topAiringDeferred.await()
            val actionRes = actionDeferred.await()
            val romanceRes = romanceDeferred.await()

            if (topAiringRes is Resource.Success && actionRes is Resource.Success && romanceRes is Resource.Success) {
                val heroList = topAiringRes.data.take(5)
                val bannersRes = aniListRepository.getBanners(heroList.map { it.mal_id })
                val banners = if (bannersRes is Resource.Success) bannersRes.data else emptyMap()

                _uiState.value = HomeUiState.Success(
                    heroAnime = heroList,
                    heroBanners = banners,
                    topAiring = topAiringRes.data.drop(5),
                    actionAnime = actionRes.data,
                    romanceAnime = romanceRes.data
                )
            } else {
                val errorMsg = (topAiringRes as? Resource.Error)?.message 
                    ?: (actionRes as? Resource.Error)?.message 
                    ?: (romanceRes as? Resource.Error)?.message 
                    ?: "Failed to load Home data"
                _uiState.value = HomeUiState.Error(errorMsg)
            }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val heroAnime: List<JikanAnime>,
        val heroBanners: Map<Int, String?> = emptyMap(),
        val topAiring: List<JikanAnime>,
        val actionAnime: List<JikanAnime>,
        val romanceAnime: List<JikanAnime>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
