package com.roninx.anime.ui.manga

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.local.dao.AnimeDao
import com.roninx.anime.data.local.entities.MangaHistoryEntity
import com.roninx.anime.data.repository.AniListRepository
import com.roninx.anime.data.repository.MangaRepository
import com.roninx.anime.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaReaderViewModel @Inject constructor(
    private val aniListRepository: AniListRepository,
    private val mangaRepository: MangaRepository,
    private val animeDao: AnimeDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val mangaId: Int = checkNotNull(savedStateHandle["mangaId"])
    val initialChapter: Int = checkNotNull(savedStateHandle["chapter"])

    private val _uiState = MutableStateFlow<MangaReaderUiState>(MangaReaderUiState.Loading)
    val uiState: StateFlow<MangaReaderUiState> = _uiState

    private val _currentChapter = MutableStateFlow(initialChapter)
    val currentChapter: StateFlow<Int> = _currentChapter

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage

    private val _readerMode = MutableStateFlow(ReaderMode.PAGED)
    val readerMode: StateFlow<ReaderMode> = _readerMode

    init {
        loadChapter(initialChapter)
    }

    fun loadChapter(chapter: Int) {
        _currentChapter.value = chapter
        viewModelScope.launch {
            _uiState.value = MangaReaderUiState.Loading
            when (val res = aniListRepository.getMangaDetails(mangaId)) {
                is Resource.Success -> {
                    val manga = res.data
                    val title = manga.title?.english ?: manga.title?.romaji ?: "Manga"
                    val pagesRes = mangaRepository.getChapterPages(title, chapter)
                    val pagesList = if (pagesRes is Resource.Success && pagesRes.data.isNotEmpty()) {
                        pagesRes.data
                    } else {
                        listOfNotNull(manga.coverImage?.large)
                    }

                    _uiState.value = MangaReaderUiState.Success(
                        manga = manga,
                        chapter = chapter,
                        totalPages = pagesList.size,
                        pageUrls = pagesList
                    )
                    saveProgress(chapter, 1)
                }
                is Resource.Error -> {
                    _uiState.value = MangaReaderUiState.Error(res.message)
                }
                else -> {}
            }
        }
    }

    fun setPage(page: Int) {
        _currentPage.value = page
        saveProgress(_currentChapter.value, page)
    }

    fun toggleReaderMode() {
        _readerMode.value = if (_readerMode.value == ReaderMode.PAGED) ReaderMode.VERTICAL else ReaderMode.PAGED
    }

    fun nextChapter() {
        val state = _uiState.value
        if (state is MangaReaderUiState.Success) {
            val maxChapter = state.manga.chapters ?: 100
            if (_currentChapter.value < maxChapter) {
                loadChapter(_currentChapter.value + 1)
            }
        }
    }

    fun prevChapter() {
        if (_currentChapter.value > 1) {
            loadChapter(_currentChapter.value - 1)
        }
    }

    private fun saveProgress(chapter: Int, page: Int) {
        val state = _uiState.value
        if (state is MangaReaderUiState.Success) {
            viewModelScope.launch {
                animeDao.upsertMangaHistory(
                    MangaHistoryEntity(
                        mangaId = mangaId,
                        title = state.manga.title?.english ?: state.manga.title?.romaji ?: "Manga",
                        imageUrl = state.manga.coverImage?.large,
                        lastChapterRead = chapter,
                        lastPageRead = page
                    )
                )
            }
        }
    }
}

sealed class MangaReaderUiState {
    object Loading : MangaReaderUiState()
    data class Success(
        val manga: AniListMedia,
        val chapter: Int,
        val totalPages: Int,
        val pageUrls: List<String>
    ) : MangaReaderUiState()
    data class Error(val message: String) : MangaReaderUiState()
}

enum class ReaderMode {
    PAGED, VERTICAL
}
