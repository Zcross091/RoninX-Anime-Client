package com.roninx.anime.ui.manga

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.roninx.anime.ui.theme.RoninRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaReaderScreen(
    viewModel: MangaReaderViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val readerMode by viewModel.readerMode.collectAsState()
    val context = LocalContext.current

    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(showControls) {
        if (showControls) {
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
                    onTap = { showControls = !showControls }
                )
            }
    ) {
        when (val state = uiState) {
            is MangaReaderUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = RoninRed)
            }
            is MangaReaderUiState.Success -> {
                if (readerMode == ReaderMode.PAGED) {
                    val pagerState = rememberPagerState(pageCount = { state.totalPages })
                    
                    LaunchedEffect(pagerState.currentPage) {
                        viewModel.setPage(pagerState.currentPage + 1)
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val pageUrl = state.pageUrls.getOrElse(page) { state.manga.coverImage?.large ?: "" }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(pageUrl)
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                    .addHeader("Referer", "https://mangapill.com/")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Page ${page + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                            ) {
                                Text(
                                    text = "Page ${page + 1} / ${state.totalPages}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.totalPages) { pageIndex ->
                            val pageUrl = state.pageUrls.getOrElse(pageIndex) { state.manga.coverImage?.large ?: "" }
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(pageUrl)
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                    .addHeader("Referer", "https://mangapill.com/")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Page ${pageIndex + 1}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // Controls Overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top Bar
                        Surface(
                            color = Color.Black.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onBackClick) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.manga.title?.english ?: state.manga.title?.romaji ?: "Manga",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Chapter $currentChapter",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleReaderMode() }) {
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = "Toggle Mode",
                                        tint = if (readerMode == ReaderMode.VERTICAL) RoninRed else Color.White
                                    )
                                }
                            }
                        }

                        // Bottom Navigation Bar
                        Surface(
                            color = Color.Black.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.prevChapter() },
                                    enabled = currentChapter > 1,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                                    Text("Prev Ch")
                                }

                                Text(
                                    text = "Ch. $currentChapter",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Button(
                                    onClick = { viewModel.nextChapter() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoninRed)
                                ) {
                                    Text("Next Ch")
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
            is MangaReaderUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(containerColor = RoninRed)) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}
