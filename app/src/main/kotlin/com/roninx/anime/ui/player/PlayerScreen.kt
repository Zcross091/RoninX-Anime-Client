package com.roninx.anime.ui.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.roninx.anime.ui.theme.RoninRed
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onNextEpisodeClick: (Int, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val qualities by viewModel.availableQualities.collectAsState()
    val hasNextEpisode by viewModel.hasNextEpisode.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showQualityMenu by remember { mutableStateOf(false) }

    LaunchedEffect(showControls) {
        if (showControls && !showQualityMenu) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        if (showQualityMenu) {
                            showQualityMenu = false
                        } else {
                            showControls = !showControls 
                        }
                    },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) {
                            viewModel.seekBackward()
                        } else {
                            viewModel.seekForward()
                        }
                    }
                )
            }
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = RoninRed)
            }
            is PlayerUiState.Mining -> {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = RoninRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = state.message, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            is PlayerUiState.Success -> {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = viewModel.player
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isBuffering && !showControls) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = RoninRed)
                }

                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    PlayerControls(
                        animeTitle = state.anime.title_english ?: state.anime.title,
                        episode = state.episode,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        currentPosition = currentPosition,
                        duration = duration,
                        hasNextEpisode = hasNextEpisode,
                        onPlayPauseToggle = { viewModel.togglePlayPause() },
                        onSeek = { viewModel.seekTo(it) },
                        onSkipIntro = { viewModel.skipIntro() },
                        onBackClick = onBackClick,
                        onQualityClick = { showQualityMenu = true },
                        onNextEpisode = { onNextEpisodeClick(state.anime.mal_id, state.episode + 1) }
                    )
                }

                if (showQualityMenu) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showQualityMenu = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.width(200.dp),
                            color = Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Select Quality",
                                    color = Color.White,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                if (qualities.isEmpty()) {
                                    Text("Detecting qualities...", color = Color.Gray, fontSize = 12.sp)
                                }
                                qualities.forEach { quality ->
                                    Text(
                                        text = quality.label,
                                        color = Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setQuality(quality)
                                                showQualityMenu = false
                                            }
                                            .padding(vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is PlayerUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                            Text("Go Back")
                        }
                        Button(onClick = { viewModel.retry() }, colors = ButtonDefaults.buttonColors(containerColor = RoninRed)) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerControls(
    animeTitle: String,
    episode: Int,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosition: Long,
    duration: Long,
    hasNextEpisode: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipIntro: () -> Unit,
    onBackClick: () -> Unit,
    onQualityClick: () -> Unit,
    onNextEpisode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = animeTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "Episode $episode", color = Color.Gray, fontSize = 14.sp)
            }
            IconButton(onClick = onQualityClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        // Center Controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            IconButton(onClick = { onSeek((currentPosition - 10000L).coerceAtLeast(0L)) }) {
                Icon(Icons.Default.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            if (isBuffering) {
                CircularProgressIndicator(color = RoninRed, modifier = Modifier.size(48.dp))
            } else {
                IconButton(onClick = onPlayPauseToggle, modifier = Modifier.size(64.dp)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            IconButton(onClick = { onSeek((currentPosition + 10000L).coerceAtMost(duration)) }) {
                Icon(Icons.Default.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // Bottom Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSkipIntro,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Skip Intro (85s)", color = Color.White, fontSize = 12.sp)
                }

                if (hasNextEpisode) {
                    Button(
                        onClick = onNextEpisode,
                        colors = ButtonDefaults.buttonColors(containerColor = RoninRed),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Next Episode", color = Color.White, fontSize = 12.sp)
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..(duration.coerceAtLeast(1L).toFloat()),
                colors = SliderDefaults.colors(
                    thumbColor = RoninRed,
                    activeTrackColor = RoninRed,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(currentPosition), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(text = formatTime(duration), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
