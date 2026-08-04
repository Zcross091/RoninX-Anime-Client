package com.roninx.anime.ui.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.api.AniListTitle
import com.roninx.anime.data.api.AniListCoverImage
import com.roninx.anime.data.api.KitsuAnime
import com.roninx.anime.data.api.KitsuApi
import com.roninx.anime.data.repository.AniListRepository
import com.roninx.anime.data.util.Resource
import com.roninx.anime.data.util.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaViewModel @Inject constructor(
    private val aniListRepository: AniListRepository,
    private val kitsuApi: KitsuApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<MangaUiState>(MangaUiState.Loading)
    val uiState: StateFlow<MangaUiState> = _uiState

    init {
        fetchMangaData()
    }

    fun fetchMangaData() {
        viewModelScope.launch {
            _uiState.value = MangaUiState.Loading
            
            val trendingDeferred = async { fetchTrendingManga() }
            val popularDeferred = async { fetchPopularManga() }

            val trending = trendingDeferred.await()
            val popular = popularDeferred.await()

            if (trending.isNotEmpty() || popular.isNotEmpty()) {
                _uiState.value = MangaUiState.Success(
                    trendingManga = trending,
                    popularManga = popular
                )
            } else {
                _uiState.value = MangaUiState.Error("Failed to load Manga data from all sources")
            }
        }
    }

    private suspend fun fetchTrendingManga(): List<AniListMedia> {
        // 1. AniList
        val aniRes = aniListRepository.getTrendingManga()
        if (aniRes is Resource.Success && aniRes.data.isNotEmpty()) return aniRes.data

        // 2. Kitsu fallback
        val kitsuRes = safeApiCall { kitsuApi.getTrendingManga().data }
        if (kitsuRes is Resource.Success && kitsuRes.data.isNotEmpty()) {
            return kitsuRes.data.map { it.toAniListMedia() }
        }

        return emptyList()
    }

    private suspend fun fetchPopularManga(): List<AniListMedia> {
        // 1. AniList
        val aniRes = aniListRepository.getPopularManga()
        if (aniRes is Resource.Success && aniRes.data.isNotEmpty()) return aniRes.data

        // 2. Kitsu fallback
        val kitsuRes = safeApiCall { kitsuApi.getPopularManga().data }
        if (kitsuRes is Resource.Success && kitsuRes.data.isNotEmpty()) {
            return kitsuRes.data.map { it.toAniListMedia() }
        }

        return emptyList()
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
            episodes = null,
            chapters = attr?.chapterCount,
            volumes = attr?.volumeCount,
            averageScore = attr?.averageRating?.toDoubleOrNull()?.toInt(),
            description = attr?.synopsis,
            genres = null,
            status = attr?.status
        )
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
