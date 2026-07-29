package com.roninx.anime.ui.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.roninx.anime.data.local.entities.WatchHistoryEntity
import com.roninx.anime.data.local.entities.WatchlistEntity
import com.roninx.anime.ui.theme.RoninBase
import com.roninx.anime.ui.theme.RoninRed
import com.roninx.anime.ui.theme.RoninSurface

@Composable
fun MyListScreen(
    viewModel: MyListViewModel,
    onAnimeClick: (Int) -> Unit
) {
    val history by viewModel.watchHistory.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RoninBase)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "My List",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = RoninBase,
            contentColor = RoninRed,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = RoninRed
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Watchlist", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("History", fontWeight = FontWeight.Bold) }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedTab == 0) {
                WatchlistGrid(
                    items = watchlist,
                    onItemClick = onAnimeClick,
                    onDelete = { viewModel.removeFromWatchlist(it) }
                )
            } else {
                HistoryGrid(
                    items = history,
                    onItemClick = onAnimeClick,
                    onDelete = { viewModel.deleteHistory(it) }
                )
            }
        }
    }
}

@Composable
fun WatchlistGrid(
    items: List<WatchlistEntity>,
    onItemClick: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState("Your watchlist is empty")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(items) { item ->
                Box {
                    Column(
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { onItemClick(item.malId) }
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.aspectRatio(2f / 3f).fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onDelete(item.malId) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryGrid(
    items: List<WatchHistoryEntity>,
    onItemClick: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState("No watch history yet")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(items) { item ->
                Box {
                    Column(
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { onItemClick(item.malId) }
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.aspectRatio(2f / 3f).fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Ep ${item.lastEpisodeWatched}",
                            color = RoninRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { onDelete(item.malId) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = Color.Gray, fontSize = 16.sp)
    }
}
