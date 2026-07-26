package com.roninx.anime.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.roninx.anime.ui.components.AnimeRow
import com.roninx.anime.ui.theme.RoninBase
import com.roninx.anime.ui.theme.RoninRed
import kotlinx.coroutines.delay
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAnimeClick: (JikanAnime) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(RoninBase)) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = RoninRed
                )
            }
            is HomeUiState.Success -> {
                HomeContent(
                    state = state,
                    onAnimeClick = onAnimeClick
                )
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchHomeData() },
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
fun HomeContent(
    state: HomeUiState.Success,
    onAnimeClick: (JikanAnime) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Hero Carousel Section
        if (state.heroAnime.isNotEmpty()) {
            HeroCarousel(
                heroAnime = state.heroAnime,
                heroBanners = state.heroBanners,
                onAnimeClick = onAnimeClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimeRow(
            title = "Top Airing",
            animeList = state.topAiring,
            onAnimeClick = onAnimeClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnimeRow(
            title = "Action",
            animeList = state.actionAnime,
            onAnimeClick = onAnimeClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnimeRow(
            title = "Romance",
            animeList = state.romanceAnime,
            onAnimeClick = onAnimeClick
        )
        
        Spacer(modifier = Modifier.height(100.dp)) // Bottom nav space
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    heroAnime: List<JikanAnime>,
    heroBanners: Map<Int, String?>,
    onAnimeClick: (JikanAnime) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { heroAnime.size })

    // Auto-scroll logic
    LaunchedEffect(Unit) {
        while (true) {
            delay(7000) // 7 seconds
            val nextPage = (pagerState.currentPage + 1) % heroAnime.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val anime = heroAnime[page]
            val bannerUrl = heroBanners[anime.mal_id] ?: anime.images.jpg.large_image_url
            
            HeroSlide(
                anime = anime,
                bannerUrl = bannerUrl,
                onClick = { onAnimeClick(anime) }
            )
        }
        
        // Pager Indicators
        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(heroAnime.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) RoninRed else Color.White.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                        .background(color, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}

@Composable
fun HeroSlide(
    anime: JikanAnime,
    bannerUrl: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }
    ) {
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
                        colors = listOf(
                            Color.Transparent,
                            RoninBase.copy(alpha = 0.5f),
                            RoninBase
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .padding(bottom = 40.dp) // Space for indicators
        ) {
            Text(
                text = "TRENDING NOW",
                color = RoninRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = anime.title_english ?: anime.title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 38.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = anime.synopsis?.take(150) + "...",
                color = Color.LightGray,
                fontSize = 14.sp,
                maxLines = 3,
                lineHeight = 20.sp
            )
        }
    }
}
