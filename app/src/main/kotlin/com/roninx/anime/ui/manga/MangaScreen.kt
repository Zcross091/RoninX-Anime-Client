package com.roninx.anime.ui.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.ui.components.MangaRow
import com.roninx.anime.ui.theme.RoninBase
import com.roninx.anime.ui.theme.RoninRed

@Composable
fun MangaScreen(
    viewModel: MangaViewModel,
    onMangaClick: (AniListMedia) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(RoninBase)) {
        when (val state = uiState) {
            is MangaUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = RoninRed
                )
            }
            is MangaUiState.Success -> {
                MangaContent(
                    state = state,
                    onMangaClick = onMangaClick
                )
            }
            is MangaUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchMangaData() },
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
fun MangaContent(
    state: MangaUiState.Success,
    onMangaClick: (AniListMedia) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Manga Discovery",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
        )

        MangaRow(
            title = "Trending Manga",
            mangaList = state.trendingManga,
            onMangaClick = onMangaClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        MangaRow(
            title = "Popular Manga",
            mangaList = state.popularManga,
            onMangaClick = onMangaClick
        )
        
        Spacer(modifier = Modifier.height(100.dp)) // Bottom nav space
    }
}
