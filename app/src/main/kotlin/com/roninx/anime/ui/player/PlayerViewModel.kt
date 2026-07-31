package com.roninx.anime.ui.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.data.api.StreamLink
import com.roninx.anime.data.local.entities.WatchHistoryEntity
import com.roninx.anime.data.repository.AnimeRepository
import com.roninx.anime.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: AnimeRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val animeId: Int = checkNotNull(savedStateHandle["animeId"])
    private val episode: Int = checkNotNull(savedStateHandle["episode"])

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState

    private var streamList: List<StreamLink> = emptyList()
    private var currentStreamIndex: Int = 0

    private val trackSelector = DefaultTrackSelector(context)
    
    // Core ExoPlayer instance with globalized network configuration
    val player: ExoPlayer = run {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _availableQualities = MutableStateFlow<List<VideoQuality>>(emptyList())
    val availableQualities: StateFlow<List<VideoQuality>> = _availableQualities

    init {
        setupPlayerListeners()
        fetchStreamAndPlay()
        startPositionUpdateTracker()
    }

    private fun setupPlayerListeners() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                saveProgress()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateAvailableQualities(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                // Automatic mirror fallback
                val nextIndex = currentStreamIndex + 1
                if (nextIndex < streamList.size) {
                    currentStreamIndex = nextIndex
                    playStream(streamList[nextIndex].url)
                } else {
                    _uiState.value = PlayerUiState.Error("Playback Error: ${error.localizedMessage}")
                }
            }
        })
    }

    private fun saveProgress() {
        val state = uiState.value
        if (state is PlayerUiState.Success) {
            viewModelScope.launch {
                repository.upsertWatchHistory(
                    WatchHistoryEntity(
                        malId = state.anime.mal_id,
                        title = state.anime.title_english ?: state.anime.title,
                        imageUrl = state.anime.images.jpg.large_image_url,
                        lastEpisodeWatched = state.episode,
                        progressMs = player.currentPosition,
                        durationMs = player.duration
                    )
                )
            }
        }
    }

    private fun updateAvailableQualities(tracks: Tracks) {
        val qualities = mutableListOf<VideoQuality>()
        tracks.groups.forEach { group ->
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.height != androidx.media3.common.Format.NO_VALUE) {
                        qualities.add(VideoQuality("${format.height}p", group, i))
                    }
                }
            }
        }
        _availableQualities.value = qualities.sortedByDescending { 
            it.label.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 
        }
    }

    private fun startPositionUpdateTracker() {
        viewModelScope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _currentPosition.value = player.currentPosition
                }
                delay(1000)
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
        _currentPosition.value = position
        saveProgress()
    }

    fun skipIntro() {
        val newPos = player.currentPosition + 85000L
        player.seekTo(newPos.coerceAtMost(player.duration))
        saveProgress()
    }

    fun seekForward() {
        player.seekTo(player.currentPosition + 10000L)
        saveProgress()
    }

    fun seekBackward() {
        player.seekTo((player.currentPosition - 10000L).coerceAtLeast(0L))
        saveProgress()
    }

    @OptIn(UnstableApi::class)
    fun setQuality(quality: VideoQuality) {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setOverrideForType(TrackSelectionOverride(quality.group.mediaTrackGroup, quality.index))
            .build()
    }

    private fun fetchStreamAndPlay() {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            
            val animeRes = repository.getAnimeFull(animeId)
            if (animeRes is Resource.Success) {
                val anime = animeRes.data
                val streamsRes = repository.getStreamLinks(
                    title = anime.title,
                    originalTitle = anime.title_english ?: anime.title,
                    synonyms = emptyList(),
                    episode = episode
                )

                if (streamsRes is Resource.Success && streamsRes.data.isNotEmpty()) {
                    val validStreams = streamsRes.data.filter { it.url.startsWith("http") }
                    if (validStreams.isNotEmpty()) {
                        streamList = validStreams
                        currentStreamIndex = 0
                        playStream(streamList[0].url)
                        _uiState.value = PlayerUiState.Success(anime, episode)
                        saveProgress()
                    } else {
                        _uiState.value = PlayerUiState.Error("No playable stream found. Miner triggered.")
                        repository.triggerMiner(anime.title, episode)
                    }
                } else {
                    _uiState.value = PlayerUiState.Error("No cached stream found. Miner triggered.")
                    repository.triggerMiner(anime.title, episode)
                }
            } else {
                _uiState.value = PlayerUiState.Error((animeRes as? Resource.Error)?.message ?: "Failed to load anime metadata")
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun playStream(url: String) {
        // Broad HLS detection signatures
        val isHls = url.contains("m3u8", ignoreCase = true) || 
                   url.contains("hls", ignoreCase = true) || 
                   url.contains(".m3u", ignoreCase = true)
        
        val mimeType = if (isHls) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMimeType(mimeType)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare() // Ensure player transitions from IDLE to BUFFERING/READY
        player.playWhenReady = true
    }

    override fun onCleared() {
        saveProgress()
        super.onCleared()
        player.release()
    }
}

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Success(val anime: JikanAnime, val episode: Int) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

data class VideoQuality(
    val label: String,
    val group: Tracks.Group,
    val index: Int
)
