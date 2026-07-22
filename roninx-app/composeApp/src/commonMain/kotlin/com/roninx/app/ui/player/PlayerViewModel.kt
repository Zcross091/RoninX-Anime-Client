package com.roninx.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.shared.models.Anime
import com.roninx.shared.network.RoninApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(private val anime: Anime, private val episode: Int) : ViewModel() {
    private val roninApi = RoninApi()
    
    private val _streamUrl = MutableStateFlow<String?>(null)
    val streamUrl: StateFlow<String?> = _streamUrl

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchStream()
    }

    private fun fetchStream() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val streams = roninApi.fetchStreams(anime.title, episode)
                if (streams.isNotEmpty()) {
                    // Just picking the first one for now. 
                    // In a real scenario, we'd filter for direct video links or handle the Blogger JSON.
                    val url = streams.first().url
                    
                    // If it's a magnet or something we can't play, we might need a different UI
                    if (url.startsWith("magnet:")) {
                        _error.value = "Magnet links are not supported in the player yet."
                    } else {
                        _streamUrl.value = url
                    }
                } else {
                    _error.value = "No streams found. Miner triggered!"
                }
            } catch (e: Exception) {
                _error.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
