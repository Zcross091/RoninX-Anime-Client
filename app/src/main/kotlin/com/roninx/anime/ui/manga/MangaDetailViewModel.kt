package com.roninx.anime.ui.manga

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.api.AniListTitle
import com.roninx.anime.data.api.AniListCoverImage
import com.roninx.anime.data.api.KitsuApi
import com.roninx.anime.data.local.dao.AnimeDao
import com.roninx.anime.data.local.entities.MangaHistoryEntity
import com.roninx.anime.data.repository.AniListRepository
import com.roninx.anime.data.util.Resource
import com.roninx.anime.data.util.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaDetailViewModel @Inject constructor(
    private val aniListRepository: AniListRepository,
    private val kitsuApi: KitsuApi,
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

            // 1. AniList
            when (val res = aniListRepository.getMangaDetails(mangaId)) {
                is Resource.Success -> {
                    _uiState.value = MangaDetailUiState.Success(res.data)
                    return@launch
                }
                else -> {}
            }

            // 2. Kitsu fallback
            val kitsuRes = safeApiCall { kitsuApi.getMangaById(mangaId.toString()).data }
            if (kitsuRes is Resource.Success) {
                val attr = kitsuRes.data.attributes
                val media = AniListMedia(
                    id = kitsuRes.data.id.toIntOrNull() ?: mangaId,
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
                _uiState.value = MangaDetailUiState.Success(media)
                return@launch
            }

            _uiState.value = MangaDetailUiState.Error("Failed to load manga details from all sources")
        }
    }
}

sealed class MangaDetailUiState {
    object Loading : MangaDetailUiState()
    data class Success(val manga: AniListMedia) : MangaDetailUiState()
    data class Error(val message: String) : MangaDetailUiState()
}
