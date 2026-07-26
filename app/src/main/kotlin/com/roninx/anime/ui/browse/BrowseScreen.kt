package com.roninx.anime.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.ui.components.AnimeCard
import com.roninx.anime.ui.components.MangaCard
import com.roninx.anime.ui.theme.RoninBase
import com.roninx.anime.ui.theme.RoninRed
import com.roninx.anime.ui.theme.RoninSurface

val GENRES = listOf(
    GenreItem(1, "Action", Color(0xFFC4202C)),
    GenreItem(2, "Adventure", Color(0xFFE67E22)),
    GenreItem(4, "Comedy", Color(0xFFF1C40F)),
    GenreItem(8, "Drama", Color(0xFF9B59B6)),
    GenreItem(10, "Fantasy", Color(0xFF2ECC71)),
    GenreItem(14, "Horror", Color(0xFF2C3E50)),
    GenreItem(7, "Mystery", Color(0xFF2980B9)),
    GenreItem(22, "Romance", Color(0xFFE91E63)),
    GenreItem(24, "Sci-Fi", Color(0xFF3498DB)),
    GenreItem(36, "Slice of Life", Color(0xFF1ABC9C)),
    GenreItem(37, "Supernatural", Color(0xFF8E44AD)),
    GenreItem(30, "Sports", Color(0xFFE67E22))
)

@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    onAnimeClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedTab by viewModel.selectedScheduleTab.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RoninBase)
            .padding(top = 16.dp)
    ) {
        // Header
        Text(
            text = if (selectedGenre != null) "Genre: ${selectedGenre?.name}" else "Browse & Schedule",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Schedule Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(ScheduleTab.values()) { tab ->
                val isSelected = selectedTab == tab && selectedGenre == null
                Surface(
                    onClick = { viewModel.onScheduleTabSelected(tab) },
                    color = if (isSelected) RoninRed else RoninSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = tab.name.lowercase().capitalize(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Genre List (Horizontal)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(GENRES) { genre ->
                val isSelected = selectedGenre == genre
                Surface(
                    onClick = { viewModel.onGenreSelected(if (isSelected) null else genre) },
                    color = if (isSelected) genre.color else RoninSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = genre.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is BrowseUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = RoninRed
                    )
                }
                is BrowseUiState.GenreSuccess -> {
                    AnimeGrid(anime = state.anime, onAnimeClick = onAnimeClick)
                }
                is BrowseUiState.ScheduleSuccess -> {
                    AniListGrid(media = state.anime, onAnimeClick = onAnimeClick)
                }
                is BrowseUiState.Error -> {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun AnimeGrid(anime: List<JikanAnime>, onAnimeClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(anime) { item ->
            AnimeCard(
                anime = item,
                onClick = { onAnimeClick(item.mal_id) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AniListGrid(media: List<AniListMedia>, onAnimeClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(media) { item ->
            // Map AniListMedia to AnimeCard or create a GenericCard
            // Reusing AnimeCard with a helper mapping or simple mock for now
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onAnimeClick(item.idMal ?: 0) }
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.aspectRatio(2f / 3f).fillMaxWidth()
                ) {
                    androidx.compose.ui.layout.ContentScale
                    coil.compose.AsyncImage(
                        model = item.coverImage?.large,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Cover,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.title?.english ?: item.title?.romaji ?: "",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
