package com.roninx.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.shared.models.Anime
import com.roninx.shared.network.JikanApi
import com.roninx.shared.network.Supabase
import io.github.jan_tennert.supabase.gotrue.auth
import io.github.jan_tennert.supabase.postgrest.postgrest
import io.github.jan_tennert.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class HistoryEntry(
    val title: String,
    val image: String,
    val ep_count: Int,
    val last_ep: Int,
    val updated_at: String
)

class HomeViewModel : ViewModel() {
    private val jikanApi = JikanApi()
    
    private val _trendingAnime = MutableStateFlow<List<Anime>>(emptyList())
    val trendingAnime: StateFlow<List<Anime>> = _trendingAnime

    private val _continueWatching = MutableStateFlow<List<Anime>>(emptyList())
    val continueWatching: StateFlow<List<Anime>> = _continueWatching

    init {
        fetchHomeData()
        fetchWatchHistory()
    }

    private fun fetchHomeData() {
        viewModelScope.launch {
            try {
                _trendingAnime.value = jikanApi.getTrending()
            } catch (e: Exception) { }
        }
    }

    fun fetchWatchHistory() {
        val user = Supabase.client.auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            try {
                val results = Supabase.client.postgrest.from("user_watch_history")
                    .select()
                    .eq("user_id", user.id)
                    .order("updated_at", Order.DESCENDING)
                    .decodeList<HistoryEntry>()

                _continueWatching.value = results.map { entry ->
                    Anime(
                        title = entry.title,
                        image = entry.image,
                        epCount = entry.ep_count,
                        synopsis = "Last watched: Episode ${entry.last_ep}"
                    )
                }
            } catch (e: Exception) { }
        }
    }
}
