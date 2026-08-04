package com.roninx.anime.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.api.AniListTitle
import com.roninx.anime.data.api.AniListCoverImage
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.data.api.KitsuAnime
import com.roninx.anime.data.api.KitsuApi
import com.roninx.anime.data.repository.AniListRepository
import com.roninx.anime.data.repository.AnimeRepository
import com.roninx.anime.data.util.Resource
import com.roninx.anime.data.util.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val aniListRepository: AniListRepository,
    private val kitsuApi: KitsuApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Initial)
    val uiState: StateFlow<BrowseUiState> = _uiState

    private val _selectedGenre = MutableStateFlow<GenreItem?>(null)
    val selectedGenre: StateFlow<GenreItem?> = _selectedGenre

    private val _selectedScheduleTab = MutableStateFlow(ScheduleTab.AIRING)
    val selectedScheduleTab: StateFlow<ScheduleTab> = _selectedScheduleTab

    init {
        // Default view
        fetchScheduleData(ScheduleTab.AIRING)
    }

    fun retry() {
        val genre = _selectedGenre.value
        if (genre != null) {
            fetchGenreData(genre)
        } else {
            fetchScheduleData(_selectedScheduleTab.value)
        }
    }

    fun onGenreSelected(genre: GenreItem?) {
        _selectedGenre.value = genre
        if (genre != null) {
            fetchGenreData(genre)
        } else {
            fetchScheduleData(_selectedScheduleTab.value)
        }
    }

    fun onScheduleTabSelected(tab: ScheduleTab) {
        _selectedScheduleTab.value = tab
        _selectedGenre.value = null
        fetchScheduleData(tab)
    }

    private fun fetchGenreData(genre: GenreItem) {
        viewModelScope.launch {
            _uiState.value = BrowseUiState.Loading
            when (val resource = animeRepository.getAnimeByGenre(genre.id.toString())) {
                is Resource.Success -> {
                    _uiState.value = BrowseUiState.GenreSuccess(resource.data)
                }
                is Resource.Error -> {
                    _uiState.value = BrowseUiState.Error(resource.message)
                }
                else -> {}
            }
        }
    }

    private fun fetchScheduleData(tab: ScheduleTab) {
        viewModelScope.launch {
            _uiState.value = BrowseUiState.Loading

            // 1. AniList
            val resource = when (tab) {
                ScheduleTab.AIRING -> aniListRepository.getAnimeBySchedule(status = "RELEASING")
                ScheduleTab.UPCOMING -> aniListRepository.getAnimeBySchedule(status = "NOT_YET_RELEASED")
                ScheduleTab.TV -> aniListRepository.getAnimeBySchedule(format = "TV")
                ScheduleTab.MOVIE -> aniListRepository.getAnimeBySchedule(format = "MOVIE")
            }
            
            when (resource) {
                is Resource.Success -> {
                    _uiState.value = BrowseUiState.ScheduleSuccess(resource.data)
                    return@launch
                }
                else -> {}
            }

            // 2. Kitsu fallback — use trending anime as approximate schedule data
            val kitsuStatus = when (tab) {
                ScheduleTab.AIRING -> "current"
                ScheduleTab.UPCOMING -> "upcoming"
                ScheduleTab.TV -> "current"
                ScheduleTab.MOVIE -> "current"
            }
            val kitsuRes = safeApiCall { kitsuApi.getTrending(status = kitsuStatus).data }
            if (kitsuRes is Resource.Success && kitsuRes.data.isNotEmpty()) {
                _uiState.value = BrowseUiState.ScheduleSuccess(kitsuRes.data.map { it.toAniListMedia() })
                return@launch
            }

            _uiState.value = BrowseUiState.Error(
                (resource as? Resource.Error)?.message ?: "Failed to load browse data from all sources"
            )
        }
    }

    private fun KitsuAnime.toAniListMedia(): AniListMedia {
        val attr = attributes
        return AniListMedia(
            id = id.toIntOrNull() ?: id.hashCode(),
            idMal = null,
            title = AniListTitle(
                english = attr?.titles?.en,
                romaji = attr?.canonicalTitle ?: attr?.titles?.en_jp
            ),
            coverImage = AniListCoverImage(
                large = attr?.posterImage?.large ?: attr?.posterImage?.medium
            ),
            bannerImage = attr?.coverImage?.large,
            episodes = attr?.episodeCount,
            chapters = null,
            volumes = null,
            averageScore = attr?.averageRating?.toDoubleOrNull()?.toInt(),
            description = attr?.synopsis,
            genres = null,
            status = attr?.status
        )
    }
}

sealed class BrowseUiState {
    object Initial : BrowseUiState()
    object Loading : BrowseUiState()
    data class GenreSuccess(val anime: List<JikanAnime>) : BrowseUiState()
    data class ScheduleSuccess(val anime: List<AniListMedia>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}

data class GenreItem(val id: Int, val name: String, val color: androidx.compose.ui.graphics.Color)

enum class ScheduleTab {
    AIRING, UPCOMING, TV, MOVIE
}
