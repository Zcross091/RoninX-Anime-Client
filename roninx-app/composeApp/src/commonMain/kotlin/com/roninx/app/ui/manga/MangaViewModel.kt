package com.roninx.app.ui.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.shared.models.Anime
import com.roninx.shared.network.MangaApi
import com.roninx.shared.network.MangaChapter
import com.roninx.shared.network.MangaPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MangaViewModel(private val manga: Anime) : ViewModel() {
    private val mangaApi = MangaApi()
    
    private val _chapters = MutableStateFlow<List<MangaChapter>>(emptyList())
    val chapters: StateFlow<List<MangaChapter>> = _chapters

    private val _activeChapter = MutableStateFlow<MangaChapter?>(null)
    val activeChapter: StateFlow<MangaChapter?> = _activeChapter

    private val _pages = MutableStateFlow<List<MangaPage>>(emptyList())
    val pages: StateFlow<List<MangaPage>> = _pages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadManga()
    }

    private fun loadManga() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val mangaId = mangaApi.searchManga(manga.title)
                if (mangaId != null) {
                    val info = mangaApi.getMangaInfo(mangaId)
                    _chapters.value = info.chapters
                    if (info.chapters.isNotEmpty()) {
                        selectChapter(info.chapters.last()) // Start with oldest or first
                    }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectChapter(chapter: MangaChapter) {
        _activeChapter.value = chapter
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _pages.value = mangaApi.getChapterPages(chapter.id)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
