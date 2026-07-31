package com.roninx.anime.data.repository

import com.roninx.anime.data.api.JikanApi
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.data.api.RoninProxyApi
import com.roninx.anime.data.api.StreamLink
import com.roninx.anime.data.local.dao.AnimeDao
import com.roninx.anime.data.local.entities.WatchHistoryEntity
import com.roninx.anime.data.local.entities.WatchlistEntity
import com.roninx.anime.data.util.Resource
import com.roninx.anime.data.util.TitleUtils
import com.roninx.anime.data.util.safeApiCall
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import com.roninx.anime.data.api.AniListMedia
import com.roninx.anime.data.api.JikanImages
import com.roninx.anime.data.api.JikanJpg

@Singleton
class AnimeRepository @Inject constructor(
    private val jikanApi: JikanApi,
    private val aniListRepository: AniListRepository,
    private val roninProxyApi: RoninProxyApi,
    private val animeDao: AnimeDao
) {
    // API Calls with Automatic AniList Fallback
    suspend fun getTopAiring(): Resource<List<JikanAnime>> {
        val jikanRes = safeApiCall { jikanApi.getSeasonNow().data }
        if (jikanRes is Resource.Success && jikanRes.data.isNotEmpty()) return jikanRes
        
        return when (val aniRes = aniListRepository.getTrendingAnime()) {
            is Resource.Success -> Resource.Success(aniRes.data.map { it.toJikan() })
            is Resource.Error -> jikanRes
            else -> jikanRes
        }
    }

    suspend fun getActionAnime(): Resource<List<JikanAnime>> {
        val jikanRes = safeApiCall { jikanApi.getAnimeByGenre("1").data }
        if (jikanRes is Resource.Success && jikanRes.data.isNotEmpty()) return jikanRes

        return when (val aniRes = aniListRepository.getAnimeByGenre("Action")) {
            is Resource.Success -> Resource.Success(aniRes.data.map { it.toJikan() })
            is Resource.Error -> jikanRes
            else -> jikanRes
        }
    }

    suspend fun getRomanceAnime(): Resource<List<JikanAnime>> {
        val jikanRes = safeApiCall { jikanApi.getAnimeByGenre("22").data }
        if (jikanRes is Resource.Success && jikanRes.data.isNotEmpty()) return jikanRes

        return when (val aniRes = aniListRepository.getAnimeByGenre("Romance")) {
            is Resource.Success -> Resource.Success(aniRes.data.map { it.toJikan() })
            is Resource.Error -> jikanRes
            else -> jikanRes
        }
    }

    suspend fun getAnimeByGenre(genreId: String): Resource<List<JikanAnime>> {
        val jikanRes = safeApiCall { jikanApi.getAnimeByGenre(genreId).data }
        if (jikanRes is Resource.Success && jikanRes.data.isNotEmpty()) return jikanRes

        val genreName = when (genreId) {
            "1" -> "Action"
            "22" -> "Romance"
            "4" -> "Comedy"
            "10" -> "Fantasy"
            "8" -> "Drama"
            "24" -> "Sci-Fi"
            else -> "Action"
        }

        return when (val aniRes = aniListRepository.getAnimeByGenre(genreName)) {
            is Resource.Success -> Resource.Success(aniRes.data.map { it.toJikan() })
            is Resource.Error -> jikanRes
            else -> jikanRes
        }
    }

    suspend fun searchAnime(query: String): Resource<List<JikanAnime>> {
        val jikanRes = safeApiCall { jikanApi.searchAnime(query).data }
        if (jikanRes is Resource.Success && jikanRes.data.isNotEmpty()) return jikanRes

        return when (val aniRes = aniListRepository.searchAnime(query)) {
            is Resource.Success -> Resource.Success(aniRes.data.map { it.toJikan() })
            is Resource.Error -> jikanRes
            else -> jikanRes
        }
    }

    suspend fun getAnimeFull(id: Int): Resource<JikanAnime> {
        val jikanRes = safeApiCall { jikanApi.getAnimeFull(id).data }
        if (jikanRes is Resource.Success) return jikanRes

        return when (val aniRes = aniListRepository.getAnimeDetails(id)) {
            is Resource.Success -> Resource.Success(aniRes.data.toJikan())
            is Resource.Error -> jikanRes
            else -> jikanRes
        }
    }

    private fun AniListMedia.toJikan(): JikanAnime {
        return JikanAnime(
            mal_id = idMal ?: id,
            title = title?.romaji ?: title?.english ?: "Unknown",
            title_english = title?.english,
            images = JikanImages(JikanJpg(coverImage?.large ?: "")),
            episodes = episodes ?: chapters,
            score = (averageScore?.toDouble() ?: 0.0) / 10.0,
            synopsis = description
        )
    }

    suspend fun getStreamLinks(title: String, originalTitle: String?, synonyms: List<String>, episode: Int): Resource<List<StreamLink>> {
        val variants = TitleUtils.buildVariants(listOf(title, originalTitle) + synonyms)
        val variantsJson = Gson().toJson(variants)
        return safeApiCall { roninProxyApi.getStreamLinks(episode, originalTitle ?: title, variantsJson) }
    }

    suspend fun triggerMiner(title: String, episode: Int) {
        safeApiCall { roninProxyApi.triggerMiner(title, episode) }
    }

    // Local DB - Watch History
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>> = animeDao.getAllHistory()

    suspend fun upsertWatchHistory(history: WatchHistoryEntity) {
        animeDao.upsertHistory(history)
    }

    suspend fun deleteWatchHistory(malId: Int) {
        animeDao.deleteHistory(malId)
    }

    // Local DB - Watchlist
    fun getWatchlist(): Flow<List<WatchlistEntity>> = animeDao.getWatchlist()

    suspend fun addToWatchlist(item: WatchlistEntity) {
        animeDao.addToWatchlist(item)
    }

    suspend fun removeFromWatchlist(malId: Int) {
        animeDao.removeFromWatchlist(malId)
    }

    fun isInWatchlist(malId: Int): Flow<Boolean> = animeDao.isInWatchlist(malId)
}
