package com.roninx.anime.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.ui.components.AnimeCard
import com.roninx.anime.ui.theme.RoninBase
import com.roninx.anime.ui.theme.RoninRed
import com.roninx.anime.ui.theme.RoninSurface

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onEpisodeClick: (JikanAnime, Int) -> Unit,
    onAnimeClick: (Int) -> Unit
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
                    aniListDetails = state.aniListDetails,
                    isInWatchlist = isInWatchlist,
                    onWatchlistToggle = { viewModel.toggleWatchlist() },
                    onEpisodeClick = onEpisodeClick,
                    onBackClick = onBackClick,
                    onAnimeClick = onAnimeClick
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
    aniListDetails: AniListMedia?,
    isInWatchlist: Boolean,
    onWatchlistToggle: () -> Unit,
    onEpisodeClick: (JikanAnime, Int) -> Unit,
    onBackClick: () -> Unit,
    onAnimeClick: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val bannerUrl = aniListDetails?.bannerImage ?: anime.images.jpg.large_image_url

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        // Banner/Header
        Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
            AsyncImage(
                model = bannerUrl,
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
                    lineHeight = 34.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
                    Text(if (isInWatchlist) "In Watchlist" else "Add to List", fontWeight = FontWeight.Bold)
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
                text = anime.synopsis?.replace(Regex("<[^>]*>"), "") ?: "No synopsis available.",
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
            
            // Metadata Grid (Studio, Status, Season)
            if (aniListDetails != null) {
                Spacer(modifier = Modifier.height(24.dp))
                MetadataGrid(aniListDetails)
            }

            // Characters Row
            val charEdges = aniListDetails?.characters?.edges
            if (!charEdges.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Characters",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(charEdges) { edge ->
                        CharacterItem(edge)
                    }
                }
            }

            // Episodes
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Episodes",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val episodesCount = anime.episodes ?: 12
            val chunkedEpisodes = (1..episodesCount).toList().chunked(4)
            
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

            // Recommendations
            val recs = aniListDetails?.recommendations?.nodes?.mapNotNull { it.mediaRecommendation }
            if (!recs.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "Recommended for You",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(recs) { rec ->
                        RecommendationItem(rec, onAnimeClick)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun MetadataGrid(media: AniListMedia) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MetadataItem("Studio", media.studios?.nodes?.firstOrNull()?.name ?: "N/A")
        MetadataItem("Status", media.status ?: "N/A")
        MetadataItem("Season", "${media.season ?: ""} ${media.seasonYear ?: ""}")
    }
}

@Composable
fun MetadataItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CharacterItem(edge: com.roninx.anime.data.api.AniListCharacterEdge) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        AsyncImage(
            model = edge.node?.image?.medium,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(70.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = edge.node?.name?.full ?: "Unknown",
            color = Color.White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
        Text(
            text = edge.role ?: "",
            color = Color.Gray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecommendationItem(media: AniListMedia, onClick: (Int) -> Unit) {
    Column(modifier = Modifier.width(140.dp).clickable { onClick(media.idMal ?: media.id) }) {
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.aspectRatio(2f / 3f).fillMaxWidth()
        ) {
            AsyncImage(
                model = media.coverImage?.large,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = media.title?.english ?: media.title?.romaji ?: "Unknown",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
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
