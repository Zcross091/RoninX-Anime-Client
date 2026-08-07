package com.roninx.anime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.roninx.anime.ui.detail.DetailScreen
import com.roninx.anime.ui.detail.DetailViewModel
import com.roninx.anime.ui.home.HomeScreen
import com.roninx.anime.ui.home.HomeViewModel
import com.roninx.anime.ui.browse.BrowseScreen
import com.roninx.anime.ui.browse.BrowseViewModel
import com.roninx.anime.ui.manga.MangaScreen
import com.roninx.anime.ui.manga.MangaViewModel
import com.roninx.anime.ui.manga.MangaDetailScreen
import com.roninx.anime.ui.manga.MangaDetailViewModel
import com.roninx.anime.ui.manga.MangaReaderScreen
import com.roninx.anime.ui.manga.MangaReaderViewModel
import com.roninx.anime.ui.player.PlayerScreen
import com.roninx.anime.ui.player.PlayerViewModel
import com.roninx.anime.ui.search.SearchScreen
import com.roninx.anime.ui.search.SearchViewModel
import com.roninx.anime.ui.mylist.MyListScreen
import com.roninx.anime.ui.mylist.MyListViewModel
import com.roninx.anime.ui.navigation.Screen
import com.roninx.anime.ui.theme.RoninBase
import com.roninx.anime.ui.theme.RoninRed
import com.roninx.anime.ui.theme.RoninXAnimeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val updateInfo by mainViewModel.updateInfo.collectAsState()
            val downloadProgress by mainViewModel.downloadProgress.collectAsState()
            val updateError by mainViewModel.updateError.collectAsState()

            RoninXAnimeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = RoninBase) {
                    MainScreen()

                    updateInfo?.let { info ->
                        AlertDialog(
                            onDismissRequest = { 
                                if (downloadProgress == null) mainViewModel.dismissUpdate() 
                            },
                            title = { Text(if (downloadProgress != null) "Downloading Update..." else "New Update Available (${info.versionName})") },
                            text = { 
                                Column {
                                    if (downloadProgress != null) {
                                        LinearProgressIndicator(
                                            progress = downloadProgress ?: 0f,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                            color = RoninRed
                                        )
                                        Text("${((downloadProgress ?: 0f) * 100).toInt()}%", modifier = Modifier.align(Alignment.CenterHorizontally))
                                    } else {
                                        Text(info.releaseNotes.take(300) + if (info.releaseNotes.length > 300) "..." else "")
                                    }
                                }
                            },
                            confirmButton = {
                                if (downloadProgress == null) {
                                    Button(onClick = {
                                        mainViewModel.startUpdate(info)
                                    }, colors = ButtonDefaults.buttonColors(containerColor = RoninRed)) {
                                        Text("Download & Install")
                                    }
                                }
                            },
                            dismissButton = {
                                if (downloadProgress == null) {
                                    TextButton(onClick = { mainViewModel.dismissUpdate() }) {
                                        Text("Later", color = Color.Gray)
                                    }
                                }
                            },
                            containerColor = Color(0xFF1A1A1A),
                            titleContentColor = Color.White,
                            textContentColor = Color.LightGray
                        )
                    }

                    updateError?.let { error ->
                        AlertDialog(
                            onDismissRequest = { mainViewModel.dismissError() },
                            title = { Text("Update Failed") },
                            text = { Text(error) },
                            confirmButton = {
                                Button(onClick = { mainViewModel.dismissError() }) {
                                    Text("OK")
                                }
                            },
                            containerColor = Color(0xFF1A1A1A),
                            titleContentColor = Color.White,
                            textContentColor = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
        BottomNavItem("Manga", Screen.Manga.route, Icons.Default.List),
        BottomNavItem("Browse", Screen.Browse.route, Icons.Default.Menu),
        BottomNavItem("Search", Screen.Search.route, Icons.Default.Search),
        BottomNavItem("My List", Screen.MyList.route, Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = RoninBase,
                contentColor = Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RoninRed,
                            selectedTextColor = RoninRed,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = RoninBase
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController, 
            startDestination = Screen.Home.route, 
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = viewModel, 
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.Detail.createRoute(anime.mal_id))
                    },
                    onHistoryClick = { history ->
                        navController.navigate(Screen.Detail.createRoute(history.malId))
                    }
                )
            }
            composable(
                Screen.Detail.route,
                arguments = listOf(navArgument("animeId") { type = NavType.IntType })
            ) {
                val viewModel: DetailViewModel = hiltViewModel()
                DetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onEpisodeClick = { anime, episode ->
                        navController.navigate(Screen.Player.createRoute(anime.mal_id, episode))
                    },
                    onAnimeClick = { animeId ->
                        navController.navigate(Screen.Detail.createRoute(animeId))
                    }
                )
            }
            composable(
                Screen.Player.route,
                arguments = listOf(
                    navArgument("animeId") { type = NavType.IntType },
                    navArgument("episode") { type = NavType.IntType }
                )
            ) {
                val viewModel: PlayerViewModel = hiltViewModel()
                PlayerScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onNextEpisodeClick = { animeId, nextEp ->
                        navController.navigate(Screen.Player.createRoute(animeId, nextEp)) {
                            popUpTo(Screen.Player.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Manga.route) {
                val viewModel: MangaViewModel = hiltViewModel()
                MangaScreen(
                    viewModel = viewModel,
                    onMangaClick = { manga ->
                        navController.navigate(Screen.MangaDetail.createRoute(manga.id))
                    }
                )
            }
            composable(
                Screen.MangaDetail.route,
                arguments = listOf(navArgument("mangaId") { type = NavType.IntType })
            ) {
                val viewModel: MangaDetailViewModel = hiltViewModel()
                MangaDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onChapterClick = { mangaId, chapter ->
                        navController.navigate(Screen.MangaReader.createRoute(mangaId, chapter))
                    }
                )
            }
            composable(
                Screen.MangaReader.route,
                arguments = listOf(
                    navArgument("mangaId") { type = NavType.IntType },
                    navArgument("chapter") { type = NavType.IntType }
                )
            ) {
                val viewModel: MangaReaderViewModel = hiltViewModel()
                MangaReaderScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Browse.route) {
                val viewModel: BrowseViewModel = hiltViewModel()
                BrowseScreen(
                    viewModel = viewModel,
                    onAnimeClick = { animeId ->
                        navController.navigate(Screen.Detail.createRoute(animeId))
                    }
                )
            }
            composable(Screen.Search.route) {
                val viewModel: SearchViewModel = hiltViewModel()
                SearchScreen(
                    viewModel = viewModel,
                    onAnimeClick = { anime ->
                        navController.navigate(Screen.Detail.createRoute(anime.mal_id))
                    }
                )
            }
            composable(Screen.MyList.route) { 
                val viewModel: MyListViewModel = hiltViewModel()
                MyListScreen(
                    viewModel = viewModel,
                    onAnimeClick = { animeId ->
                        navController.navigate(Screen.Detail.createRoute(animeId))
                    }
                )
            }
        }
    }
}

data class BottomNavItem(val title: String, val route: String, val icon: ImageVector)
