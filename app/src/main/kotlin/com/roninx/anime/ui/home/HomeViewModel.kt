package com.roninx.anime.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.*
import com.roninx.anime.data.local.entities.WatchHistoryEntity
import com.roninx.anime.data.repository.AniListRepository
import com.roninx.anime.data.repository.AnimeRepository
import com.roninx.anime.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AnimeRepository,
    private val aniListRepository: AniListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.getWatchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchHomeData()
    }

    fun fetchHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            // Primary: Try Jikan API
            val topAiringRes = repository.getTopAiring()
            val actionRes = repository.getActionAnime()
            val romanceRes = repository.getRomanceAnime()

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
                // Fallback: Try AniList API if Jikan fails (e.g. 504 error)
                fetchFallbackHomeData()
            }
        }
    }

    private suspend fun fetchFallbackHomeData() {
        val trendingRes = aniListRepository.getTrendingAnime()
        val actionRes = aniListRepository.getAnimeByGenre("Action")
        val romanceRes = aniListRepository.getAnimeByGenre("Romance")

        if (trendingRes is Resource.Success && actionRes is Resource.Success && romanceRes is Resource.Success) {
            val heroList = trendingRes.data.take(5).map { it.toJikan() }
            val heroBanners = trendingRes.data.take(5).associate { (it.idMal ?: 0) to it.bannerImage }

            _uiState.value = HomeUiState.Success(
                heroAnime = heroList,
                heroBanners = heroBanners,
                topAiring = trendingRes.data.drop(5).map { it.toJikan() },
                actionAnime = actionRes.data.map { it.toJikan() },
                romanceAnime = romanceRes.data.map { it.toJikan() }
            )
        } else {
            val errorMsg = (trendingRes as? Resource.Error)?.message ?: "Critical: Both Jikan and AniList failed."
            _uiState.value = HomeUiState.Error(errorMsg)
        }
    }

    private fun AniListMedia.toJikan(): JikanAnime {
        return JikanAnime(
            mal_id = idMal ?: id,
            title = title?.romaji ?: "Unknown",
            title_english = title?.english,
            images = JikanImages(JikanJpg(coverImage?.large ?: "")),
            episodes = episodes ?: chapters,
            score = (averageScore?.toDouble() ?: 0.0) / 10.0,
            synopsis = description
        )
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
