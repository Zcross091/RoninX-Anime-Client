package com.roninx.app.ui.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.shared.models.Anime
import com.roninx.shared.network.Supabase
import io.github.jan_tennert.supabase.gotrue.auth
import io.github.jan_tennert.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class WatchlistEntry(
    val title: String,
    val image: String,
    val ep_count: Int,
    val score: String,
    val synopsis: String
)

class MyListViewModel : ViewModel() {
    private val _watchlist = MutableStateFlow<List<Anime>>(emptyList())
    val watchlist: StateFlow<List<Anime>> = _watchlist

    init {
        fetchWatchlist()
    }

    private fun fetchWatchlist() {
        val user = Supabase.client.auth.currentUserOrNull() ?: return
        
        viewModelScope.launch {
            try {
                val results = Supabase.client.postgrest.from("user_watchlist")
                    .select()
                    .eq("user_id", user.id)
                    .decodeList<WatchlistEntry>()
                
                _watchlist.value = results.map { entry ->
                    Anime(
                        title = entry.title,
                        image = entry.image,
                        epCount = entry.ep_count,
                        score = entry.score,
                        synopsis = entry.synopsis
                    )
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
