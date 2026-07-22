import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.roninx.app.ui.theme.RoninTheme
import com.roninx.app.ui.theme.RoninDark
import com.roninx.app.ui.theme.RoninRed
import com.roninx.app.ui.home.HomeViewModel
import com.roninx.app.ui.search.SearchViewModel
import com.roninx.app.ui.manga.MangaViewModel
import com.roninx.app.ui.auth.AuthViewModel
import com.roninx.app.ui.mylist.MyListViewModel
import com.roninx.app.ui.components.AnimeRow
import com.roninx.app.ui.components.AnimeCard
import com.roninx.app.ui.components.HeroCarousel
import com.roninx.app.ui.components.VideoPlayer
import com.roninx.app.ui.navigation.Screen
import com.roninx.app.ui.player.PlayerViewModel
import com.roninx.shared.models.Anime

@Composable
fun App() {
    RoninTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
        val authViewModel: AuthViewModel = viewModel { AuthViewModel() }
        val user by authViewModel.user.collectAsState()
        var showAuthModal by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                if (currentScreen !is Screen.Player && currentScreen !is Screen.MangaReader) {
                    TopAppBar(
                        title = { Text("RONIN", color = RoninRed, fontWeight = FontWeight.Bold) },
                        backgroundColor = Color.Black,
                        actions = {
                            IconButton(onClick = { if (user == null) showAuthModal = true else authViewModel.signOut() }) {
                                Icon(
                                    if (user == null) Icons.Default.Person else Icons.Default.ExitToApp,
                                    contentDescription = "Profile",
                                    color = Color.White
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (currentScreen !is Screen.Player && currentScreen !is Screen.MangaReader) {
                    BottomNavigation(
                        backgroundColor = Color.Black.copy(alpha = 0.9f),
                        contentColor = RoninRed
                    ) {
                        BottomNavigationItem(
                            icon = { Icon(Icons.Default.Home, "Home") },
                            label = { Text("Home") },
                            selected = currentScreen is Screen.Home,
                            onClick = { currentScreen = Screen.Home }
                        )
                        BottomNavigationItem(
                            icon = { Icon(Icons.Default.Search, "Search") },
                            label = { Text("Search") },
                            selected = currentScreen is Screen.Search,
                            onClick = { currentScreen = Screen.Search }
                        )
                        BottomNavigationItem(
                            icon = { Icon(Icons.Default.List, "My List") },
                            label = { Text("My List") },
                            selected = currentScreen is Screen.MyList,
                            onClick = { currentScreen = Screen.MyList }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RoninDark)
                    .padding(if (currentScreen is Screen.Player || currentScreen is Screen.MangaReader) PaddingValues(0.dp) else padding)
            ) {
                when (val screen = currentScreen) {
                    is Screen.Home -> HomeScreen(onAnimeClick = { currentScreen = Screen.Details(it) })
                    is Screen.Search -> SearchScreen(onAnimeClick = { currentScreen = Screen.Details(it) })
                    is Screen.MyList -> MyListScreen(onAnimeClick = { currentScreen = Screen.Details(it) })
                    is Screen.Details -> DetailScreen(screen.anime, 
                        onBack = { currentScreen = Screen.Home },
                        onEpisodeClick = { ep -> 
                            if (screen.anime.isManga) {
                                currentScreen = Screen.MangaReader(screen.anime)
                            } else {
                                currentScreen = Screen.Player(screen.anime, ep)
                            }
                        }
                    )
                    is Screen.Player -> PlayerScreen(screen.anime, screen.episode, 
                        onBack = { currentScreen = Screen.Details(screen.anime) }
                    )
                    is Screen.MangaReader -> MangaReaderScreen(screen.anime,
                        onBack = { currentScreen = Screen.Details(screen.anime) }
                    )
                }

                if (showAuthModal) {
                    AuthDialog(
                        onDismiss = { showAuthModal = false },
                        viewModel = authViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AuthDialog(onDismiss: () -> Unit, viewModel: AuthViewModel) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            backgroundColor = RoninDark,
            elevation = 8.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(if (isSignUp) "Sign Up" else "Sign In", style = MaterialTheme.typography.h5, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.DarkGray, textColor = Color.White)
                )
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.DarkGray, textColor = Color.White)
                )
                
                if (error != null) {
                    Text(error!!, color = RoninRed, style = MaterialTheme.typography.caption, modifier = Modifier.padding(top = 8.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        if (isSignUp) viewModel.signUp(email, password) 
                        else viewModel.signIn(email, password)
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = RoninRed),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text(if (isSignUp) "GO" else "LOGIN", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                TextButton(onClick = { isSignUp = !isSignUp }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (isSignUp) "Already have an account? Sign In" else "Need an account? Sign Up", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun MyListScreen(onAnimeClick: (Anime) -> Unit) {
    val viewModel: MyListViewModel = viewModel { MyListViewModel() }
    val watchlist by viewModel.watchlist.collectAsState()

    if (watchlist.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your List is Empty", color = Color.Gray)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(watchlist) { anime ->
                AnimeCard(anime = anime, onClick = { onAnimeClick(anime) })
            }
        }
    }
}

@Composable
fun MangaReaderScreen(anime: Anime, onBack: () -> Unit) {
    val viewModel: MangaViewModel = viewModel { MangaViewModel(anime) }
    val pages by viewModel.pages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeChapter by viewModel.activeChapter.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.DarkGray).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", color = Color.White)
            }
            Text(
                activeChapter?.title ?: "Reading...",
                color = Color.White,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                maxLines = 1
            )
        }

        if (isLoading && pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RoninRed)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                pages.forEach { page ->
                    AsyncImage(
                        model = page.img,
                        contentDescription = "Page ${page.page}",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreen(onAnimeClick: (Anime) -> Unit) {
    val viewModel: SearchViewModel = viewModel { SearchViewModel() }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    var term by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = term,
            onValueChange = { term = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search Anime...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, color = Color.Gray) },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.DarkGray,
                textColor = Color.White,
                cursorColor = RoninRed,
                focusedIndicatorColor = RoninRed,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search(term) })
        )

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RoninRed)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResults) { anime ->
                    AnimeCard(anime = anime, onClick = { onAnimeClick(anime) })
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(anime: Anime, episode: Int, onBack: () -> Unit) {
    val viewModel: PlayerViewModel = viewModel { PlayerViewModel(anime, episode) }
    val streamUrl by viewModel.streamUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = RoninRed)
        } else if (streamUrl != null) {
            VideoPlayer(url = streamUrl!!, modifier = Modifier.fillMaxSize())
        } else {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error ?: "Failed to load stream", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(backgroundColor = RoninRed)) {
                    Text("GO BACK", color = Color.White)
                }
            }
        }
        
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).align(Alignment.TopStart)) {
            Icon(Icons.Default.ArrowBack, "Back", color = Color.White)
        }
    }
}

@Composable
fun HomeScreen(onAnimeClick: (Anime) -> Unit) {
    val viewModel: HomeViewModel = viewModel { HomeViewModel() }
    val trendingAnime by viewModel.trendingAnime.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (trendingAnime.isNotEmpty()) {
            HeroCarousel(
                anime = trendingAnime.first(),
                onWatchClick = { onAnimeClick(trendingAnime.first()) }
            )
        }
        
        if (continueWatching.isNotEmpty()) {
            AnimeRow(
                title = "Continue Watching",
                animeList = continueWatching,
                onAnimeClick = onAnimeClick
            )
        }
        
        AnimeRow(
            title = "Trending Now",
            animeList = trendingAnime,
            onAnimeClick = onAnimeClick
        )
    }
}

@Composable
fun DetailScreen(anime: Anime, onBack: () -> Unit, onEpisodeClick: (Int) -> Unit) {
    val episodes = (1..(anime.epCount ?: 12)).toList()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 60.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    AsyncImage(
                        model = anime.banner ?: anime.image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.ArrowBack, "Back", color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(anime.title, style = MaterialTheme.typography.h4, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(anime.synopsis, style = MaterialTheme.typography.body1, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(if (anime.isManga) "Read Manga" else "Episodes", style = MaterialTheme.typography.h6, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        if (anime.isManga) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = { onEpisodeClick(1) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = RoninRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("START READING", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            items(episodes) { ep ->
                Button(
                    onClick = { onEpisodeClick(ep) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
                    modifier = Modifier.aspectRatio(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(ep.toString(), color = Color.White)
                }
            }
        }
    }
}
