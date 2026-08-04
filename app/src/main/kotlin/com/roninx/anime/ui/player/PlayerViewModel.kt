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
import com.roninx.anime.data.repository.GitHubMinerRepository
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
    private val gitHubMinerRepository: GitHubMinerRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val animeId: Int = checkNotNull(savedStateHandle["animeId"])
    val episode: Int = checkNotNull(savedStateHandle["episode"])

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState

    private var streamList: List<StreamLink> = emptyList()
    private var currentStreamIndex: Int = 0
    private var isMining: Boolean = false

    private val trackSelector = DefaultTrackSelector(context)

    // Core ExoPlayer instance with globalized network configuration & headers
    val player: ExoPlayer = run {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Referer" to "https://ronin-api-proxy.vercel.app/"
        )
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(headers["User-Agent"]!!)
            .setDefaultRequestProperties(headers)
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

    private val _isBuffering = MutableStateFlow(true)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _availableQualities = MutableStateFlow<List<VideoQuality>>(emptyList())
    val availableQualities: StateFlow<List<VideoQuality>> = _availableQualities

    private var cachedAnime: JikanAnime? = null
    private var bufferingTimeoutJob: kotlinx.coroutines.Job? = null

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
                when (state) {
                    Player.STATE_BUFFERING -> {
                        _isBuffering.value = true
                        startBufferingTimeoutGuard()
                    }
                    Player.STATE_READY -> {
                        _isBuffering.value = false
                        _duration.value = player.duration
                        cancelBufferingTimeoutGuard()
                    }
                    Player.STATE_ENDED, Player.STATE_IDLE -> {
                        _isBuffering.value = false
                        cancelBufferingTimeoutGuard()
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateAvailableQualities(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                cancelBufferingTimeoutGuard()
                // Prevent re-entrant error handling while mining is in progress
                if (isMining) return

                // Automatic mirror fallback logic
                val nextIndex = currentStreamIndex + 1
                if (nextIndex < streamList.size) {
                    currentStreamIndex = nextIndex
                    playStream(streamList[nextIndex].url)
                } else {
                    // All cached streams failed, trigger miner and start polling
                    triggerMinerAndPoll("Playback error on mirror streams: ${error.localizedMessage}")
                }
            }
        })
    }

    private fun startBufferingTimeoutGuard() {
        bufferingTimeoutJob?.cancel()
        bufferingTimeoutJob = viewModelScope.launch {
            delay(12000)
            if (_isBuffering.value && !isMining) {
                val nextIndex = currentStreamIndex + 1
                if (nextIndex < streamList.size) {
                    currentStreamIndex = nextIndex
                    playStream(streamList[nextIndex].url)
                } else {
                    triggerMinerAndPoll("Stream buffering timed out. Mining fresh stream mirrors...")
                }
            }
        }
    }

    private fun cancelBufferingTimeoutGuard() {
        bufferingTimeoutJob?.cancel()
        bufferingTimeoutJob = null
    }

    private fun saveProgress() {
        val anime = cachedAnime ?: return
        viewModelScope.launch {
            repository.upsertWatchHistory(
                WatchHistoryEntity(
                    malId = anime.mal_id,
                    title = anime.title_english ?: anime.title,
                    imageUrl = anime.images.jpg.large_image_url,
                    lastEpisodeWatched = episode,
                    progressMs = player.currentPosition,
                    durationMs = player.duration
                )
            )
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

    fun retry() {
        fetchStreamAndPlay()
    }

    private fun fetchStreamAndPlay() {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading

            val animeRes = repository.getAnimeFull(animeId)
            if (animeRes is Resource.Success) {
                val anime = animeRes.data
                cachedAnime = anime
                val animeTitle = anime.title_english ?: anime.title

                // Check if the title is actually a Manga series with no anime video adaptation
                if (anime.episodes == 0) {
                    _uiState.value = PlayerUiState.Error("\"$animeTitle\" is a Manga series without an anime video adaptation yet! You can read it in the Manga tab.")
                    return@launch
                }

                // ⚡ Stage 1: Instant Native Extraction on device (< 1.5s AniBay speed)
                val nativeStream = repository.extractDirectStream(animeTitle, episode) 
                    ?: repository.extractDirectStream(anime.title, episode)

                if (nativeStream != null && isValidStreamUrl(nativeStream.url)) {
                    streamList = listOf(nativeStream)
                    currentStreamIndex = 0
                    playStream(nativeStream.url)
                    _uiState.value = PlayerUiState.Success(anime, episode)
                    saveProgress()
                    return@launch
                }

                // ⚡ Stage 2: Check cached stream links
                val streamsRes = repository.getStreamLinks(
                    title = anime.title,
                    originalTitle = animeTitle,
                    synonyms = emptyList(),
                    episode = episode
                )

                if (streamsRes is Resource.Success && streamsRes.data.isNotEmpty()) {
                    val validStreams = streamsRes.data.filter { isValidStreamUrl(it.url) }
                    if (validStreams.isNotEmpty()) {
                        streamList = validStreams
                        currentStreamIndex = 0
                        playStream(streamList[0].url)
                        _uiState.value = PlayerUiState.Success(anime, episode)
                        saveProgress()
                        return@launch
                    }
                }

                // ⚡ Stage 3: Fallback to GitHub Cloud Runner
                triggerMinerAndPoll("Mining streams...")
            } else {
                _uiState.value = PlayerUiState.Error((animeRes as? Resource.Error)?.message ?: "Failed to load anime metadata")
            }
        }
    }

    private fun triggerMinerAndPoll(reason: String) {
        if (isMining) return
        val anime = cachedAnime ?: return
        isMining = true
        val animeTitle = anime.title_english ?: anime.title

        viewModelScope.launch {
            _uiState.value = PlayerUiState.Mining(0, 45, "Launching GitHub Cloud Mining Runner...")

            val startTimeMs = System.currentTimeMillis()

            // 1. Also trigger Ronin API backend miner as secondary backup
            repository.triggerMiner(anime.title, episode)

            // 2. Dispatch GitHub Action Workflow
            val dispatched = gitHubMinerRepository.dispatchMiningJob(animeTitle, episode)

            val statusHeader = if (dispatched) {
                "🚀 GitHub Cloud Runner Dispatched!"
            } else {
                "⚡ Searching Cloud Stream Sources..."
            }

            // 3. Poll raw GitHub content
            val minedResult = gitHubMinerRepository.pollMinedStream(
                animeTitle = animeTitle,
                episodeNumber = episode,
                startTimeMs = startTimeMs,
                maxWaitSeconds = 45
            ) { elapsedSec ->
                _uiState.value = PlayerUiState.Mining(
                    attempt = elapsedSec,
                    maxAttempts = 45,
                    message = "$statusHeader Please keep screen open (${elapsedSec}s / 45s)"
                )
            }

            if (minedResult != null && !minedResult.url.isNullOrBlank() && isValidStreamUrl(minedResult.url)) {
                val minedUrl = minedResult.url
                streamList = listOf(StreamLink(title = animeTitle, url = minedUrl, type = "mined"))
                currentStreamIndex = 0
                playStream(minedUrl)
                isMining = false
                _uiState.value = PlayerUiState.Success(anime, episode)
                saveProgress()
                return@launch
            }

            // Secondary check on backend repository if GitHub polling reached 45s without commit
            val pollRes = repository.getStreamLinks(
                title = animeTitle,
                originalTitle = anime.title,
                synonyms = emptyList(),
                episode = episode
            )

            if (pollRes is Resource.Success && pollRes.data.isNotEmpty()) {
                val valid = pollRes.data.filter { isValidStreamUrl(it.url) }
                if (valid.isNotEmpty()) {
                    streamList = valid
                    currentStreamIndex = 0
                    playStream(streamList[0].url)
                    isMining = false
                    _uiState.value = PlayerUiState.Success(anime, episode)
                    saveProgress()
                    return@launch
                }
            }

            isMining = false
            _uiState.value = PlayerUiState.Error("Streams currently unavailable for Episode $episode. Cloud runner is processing request, please tap Retry.")
        }
    }

    @OptIn(UnstableApi::class)
    private fun playStream(url: String) {
        try {
            val isHls = url.contains("m3u8", ignoreCase = true) || 
                       url.contains("hls", ignoreCase = true) || 
                       url.contains(".m3u", ignoreCase = true)
            
            val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(url))
            if (isHls) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            val mediaItem = mediaItemBuilder.build()

            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = PlayerUiState.Error("Failed to play stream: ${e.localizedMessage}")
        }
    }

    private fun isValidStreamUrl(url: String): Boolean {
        if (!url.startsWith("http")) return false

        val rejectPatterns = listOf(
            ".html", ".php", "/embed", "embedplus", "streaming.php", 
            "javascript:", "<html", "<script", "search.html", "/category/", "/anime/"
        )
        if (rejectPatterns.any { url.contains(it, ignoreCase = true) }) {
            return false
        }

        val validPatterns = listOf(
            ".m3u8", ".mp4", ".m3u", "hls", "master", "index", 
            "googlevideo", "cdn", "stream", "video", "media"
        )
        return validPatterns.any { url.contains(it, ignoreCase = true) }
    }

    override fun onCleared() {
        saveProgress()
        super.onCleared()
        player.release()
    }
}

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Mining(val attempt: Int, val maxAttempts: Int, val message: String) : PlayerUiState()
    data class Success(val anime: JikanAnime, val episode: Int) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

data class VideoQuality(
    val label: String,
    val group: Tracks.Group,
    val index: Int
)
