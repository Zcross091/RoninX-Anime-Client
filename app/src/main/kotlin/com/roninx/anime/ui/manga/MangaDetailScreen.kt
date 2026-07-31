package com.roninx.anime.ui.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.ui.theme.RoninBase
import com.roninx.anime.ui.theme.RoninRed
import com.roninx.anime.ui.theme.RoninSurface

@Composable
fun MangaDetailScreen(
    viewModel: MangaDetailViewModel,
    onBackClick: () -> Unit,
    onChapterClick: (mangaId: Int, chapter: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val readingHistory by viewModel.readingHistory.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(RoninBase)) {
        when (val state = uiState) {
            is MangaDetailUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = RoninRed)
            }
            is MangaDetailUiState.Success -> {
                MangaDetailContent(
                    manga = state.manga,
                    lastReadChapter = readingHistory?.lastChapterRead,
                    onBackClick = onBackClick,
                    onChapterClick = { ep -> onChapterClick(state.manga.id, ep) }
                )
            }
            is MangaDetailUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchMangaDetail() },
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
fun MangaDetailContent(
    manga: AniListMedia,
    lastReadChapter: Int?,
    onBackClick: () -> Unit,
    onChapterClick: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val totalChapters = manga.chapters ?: 100
    val titleStr = manga.title?.english ?: manga.title?.romaji ?: "Unknown Manga"

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        // Banner Header
        Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
            AsyncImage(
                model = manga.bannerImage ?: manga.coverImage?.large,
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
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
            ) {
                Text(
                    text = titleStr,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = RoninRed,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "RATING: ${manga.averageScore ?: "N/A"}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${manga.chapters ?: "?"} Chapters",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val startChapter = lastReadChapter ?: 1
                Button(
                    onClick = { onChapterClick(startChapter) },
                    colors = ButtonDefaults.buttonColors(containerColor = RoninRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        if (lastReadChapter != null) Icons.Default.PlayArrow else Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lastReadChapter != null) "Continue Ch. $lastReadChapter" else "Start Reading",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Synopsis & Genres
        Column(modifier = Modifier.padding(24.dp)) {
            if (!manga.genres.isNullOrEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    manga.genres.take(4).forEach { genre ->
                        Surface(
                            color = RoninSurface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = genre,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Synopsis",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = manga.description?.replace(Regex("<[^>]*>"), "") ?: "No description available.",
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Chapters List
            Text(
                text = "Chapters ($totalChapters total)",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            val chunked = (1..totalEpisodes(manga)).chunked(4)
            chunked.forEach { rowChs ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    rowChs.forEach { ch ->
                        val isRead = lastReadChapter == ch
                        Surface(
                            onClick = { onChapterClick(ch) },
                            color = if (isRead) RoninRed.copy(alpha = 0.3f) else RoninSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).padding(4.dp).height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Ch. $ch",
                                    color = if (isRead) RoninRed else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    repeat(4 - rowChs.size) {
                        Spacer(modifier = Modifier.weight(1f).padding(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

private fun totalEpisodes(manga: AniListMedia): Int {
    val ch = manga.chapters
    return if (ch != null && ch > 0) ch else 50
}
