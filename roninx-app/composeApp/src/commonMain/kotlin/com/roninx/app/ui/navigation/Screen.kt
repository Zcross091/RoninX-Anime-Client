package com.roninx.app.ui.navigation

import com.roninx.shared.models.Anime

sealed class Screen {
    object Home : Screen()
    object Search : Screen()
    object MyList : Screen()
    data class Details(val anime: Anime) : Screen()
    data class Player(val anime: Anime, val episode: Int) : Screen()
    data class MangaReader(val anime: Anime) : Screen()
}
