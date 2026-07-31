package com.roninx.anime.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.ui.theme.RoninBase
import com.roninx.anime.ui.theme.RoninRed
import com.roninx.anime.ui.theme.RoninSurface

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onEpisodeClick: (JikanAnime, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isInWatchlist by viewModel.isInWatchlist.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(RoninBase)) {
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = RoninRed)
            }
            is DetailUiState.Success -> {
                DetailContent(
                    anime = state.anime,
                    isInWatchlist = isInWatchlist,
                    onWatchlistToggle = { viewModel.toggleWatchlist() },
                    onEpisodeClick = onEpisodeClick,
                    onBackClick = onBackClick
                )
            }
            is DetailUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchAnimeDetail() },
                        colors = ButtonDefaults.buttonColors(containerColor = RoninRed)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailContent(
    anime: JikanAnime,
    isInWatchlist: Boolean,
    onWatchlistToggle: () -> Unit,
    onEpisodeClick: (JikanAnime, Int) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        // Banner/Header
        Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
            AsyncImage(
                model = anime.images.jpg.large_image_url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, RoninBase.copy(alpha = 0.8f), RoninBase)
                        )
                    )
            )
            
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(16.dp).align(Alignment.TopStart).background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
            ) {
                Text(
                    text = anime.title_english ?: anime.title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 34.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = RoninRed,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "SCORE: ${anime.score ?: "N/A"}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${anime.episodes ?: "?"} Episodes",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onWatchlistToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInWatchlist) Color.White.copy(alpha = 0.1f) else RoninRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isInWatchlist) "Added to List" else "Add to List", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Synopsis
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Synopsis",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = anime.synopsis ?: "No synopsis available.",
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val titleName = anime.title_english ?: anime.title
            val totalEpisodes = when {
                anime.episodes != null && anime.episodes > 0 -> anime.episodes
                titleName.contains("One Piece", ignoreCase = true) -> 1100
                titleName.contains("Conan", ignoreCase = true) -> 1100
                titleName.contains("Pokemon", ignoreCase = true) || titleName.contains("Pokémon", ignoreCase = true) -> 1200
                titleName.contains("Naruto", ignoreCase = true) || titleName.contains("Boruto", ignoreCase = true) -> 720
                titleName.contains("Bleach", ignoreCase = true) -> 366
                else -> 100
            }

            var selectedRangeIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
            val chunkSize = 50
            val episodeRanges = (1..totalEpisodes).chunked(chunkSize)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Episodes",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "($totalEpisodes total)",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (episodeRanges.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedRangeIndex,
                    containerColor = RoninSurface,
                    contentColor = RoninRed,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    episodeRanges.forEachIndexed { index, range ->
                        Tab(
                            selected = selectedRangeIndex == index,
                            onClick = { selectedRangeIndex = index },
                            text = {
                                Text(
                                    text = "${range.first} - ${range.last}",
                                    color = if (selectedRangeIndex == index) RoninRed else Color.Gray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val currentEpisodes = episodeRanges.getOrNull(selectedRangeIndex) ?: (1..totalEpisodes).toList()
            val chunkedEpisodes = currentEpisodes.chunked(4)
            
            chunkedEpisodes.forEach { rowEps ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    rowEps.forEach { ep ->
                        EpisodeButton(
                            episode = ep,
                            onClick = { onEpisodeClick(anime, ep) },
                            modifier = Modifier.weight(1f).padding(4.dp)
                        )
                    }
                    repeat(4 - rowEps.size) {
                        Spacer(modifier = Modifier.weight(1f).padding(4.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun EpisodeButton(
    episode: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = RoninSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(50.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = episode.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
