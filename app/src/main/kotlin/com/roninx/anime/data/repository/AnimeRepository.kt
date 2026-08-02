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
import com.roninx.anime.data.api.KitsuAnime
import com.roninx.anime.data.api.KitsuApi
import com.roninx.anime.data.api.ShikimoriAnime
import com.roninx.anime.data.api.ShikimoriApi

@Singleton
class AnimeRepository @Inject constructor(
    private val jikanApi: JikanApi,
    private val aniListRepository: AniListRepository,
    private val kitsuApi: KitsuApi,
    private val shikimoriApi: ShikimoriApi,
    private val roninProxyApi: RoninProxyApi,
    private val animeDao: AnimeDao
) {
    // 4-Tier Fallback Chain: Jikan -> AniList -> Kitsu -> Shikimori

    suspend fun getTopAiring(): Resource<List<JikanAnime>> {
        // 1. Jikan
        val jikanRes = safeApiCall { jikanApi.getSeasonNow().data }
        if (jikanRes is Resource.Success && jikanRes.data.isNotEmpty()) return jikanRes

        // 2. AniList
        val aniRes = aniListRepository.getTrendingAnime()
        if (aniRes is Resource.Success && aniRes.data.isNotEmpty()) {
            return Resource.Success(aniRes.data.map { it.toJikan() })
        }

        // 3. Kitsu
        val kitsuRes = safeApiCall { kitsuApi.getTrending().data }
        if (kitsuRes is Resource.Success && kitsuRes.data.isNotEmpty()) {
            return Resource.Success(kitsuRes.data.map { it.toJikan() })
        }

        // 4. Shikimori
        val shikiRes = safeApiCall { shikimoriApi.getTrending() }
        if (shikiRes is Resource.Success && shikiRes.data.isNotEmpty()) {
            return Resource.Success(shikiRes.data.map { it.toJikan() })
        }

        return jikanRes
    }

    suspend fun getActionAnime(): Resource<List<JikanAnime>> {
        return getAnimeByGenre("1")
    }

    suspend fun getRomanceAnime(): Resource<List<JikanAnime>> {
        return getAnimeByGenre("22")
    }

    suspend fun getAnimeByGenre(genreId: String): Resource<List<JikanAnime>> {
        // 1. Jikan
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

        // 2. AniList
        val aniRes = aniListRepository.getAnimeByGenre(genreName)
        if (aniRes is Resource.Success && aniRes.data.isNotEmpty()) {
            return Resource.Success(aniRes.data.map { it.toJikan() })
        }

        // 3. Kitsu
        val kitsuRes = safeApiCall { kitsuApi.getByCategory(genreName.lowercase()).data }
        if (kitsuRes is Resource.Success && kitsuRes.data.isNotEmpty()) {
            return Resource.Success(kitsuRes.data.map { it.toJikan() })
        }

        // 4. Shikimori
        val shikiRes = safeApiCall { shikimoriApi.getByGenre(genreId) }
        if (shikiRes is Resource.Success && shikiRes.data.isNotEmpty()) {
            return Resource.Success(shikiRes.data.map { it.toJikan() })
        }

        return jikanRes
    }

    suspend fun searchAnime(query: String): Resource<List<JikanAnime>> {
        // 1. Jikan
        val jikanRes = safeApiCall { jikanApi.searchAnime(query).data }
        if (jikanRes is Resource.Success && jikanRes.data.isNotEmpty()) return jikanRes

        // 2. AniList
        val aniRes = aniListRepository.searchAnime(query)
        if (aniRes is Resource.Success && aniRes.data.isNotEmpty()) {
            return Resource.Success(aniRes.data.map { it.toJikan() })
        }

        // 3. Kitsu
        val kitsuRes = safeApiCall { kitsuApi.searchAnime(query).data }
        if (kitsuRes is Resource.Success && kitsuRes.data.isNotEmpty()) {
            return Resource.Success(kitsuRes.data.map { it.toJikan() })
        }

        // 4. Shikimori
        val shikiRes = safeApiCall { shikimoriApi.searchAnime(query) }
        if (shikiRes is Resource.Success && shikiRes.data.isNotEmpty()) {
            return Resource.Success(shikiRes.data.map { it.toJikan() })
        }

        return jikanRes
    }

    suspend fun getAnimeFull(id: Int): Resource<JikanAnime> {
        // 1. Jikan
        val jikanRes = safeApiCall { jikanApi.getAnimeFull(id).data }
        if (jikanRes is Resource.Success) return jikanRes

        // 2. AniList
        val aniRes = aniListRepository.getAnimeDetails(id)
        if (aniRes is Resource.Success) return Resource.Success(aniRes.data.toJikan())

        // 3. Kitsu
        val kitsuRes = safeApiCall { kitsuApi.getAnimeById(id.toString()).data }
        if (kitsuRes is Resource.Success) return Resource.Success(kitsuRes.data.toJikan())

        // 4. Shikimori
        val shikiRes = safeApiCall { shikimoriApi.getAnimeById(id) }
        if (shikiRes is Resource.Success) return Resource.Success(shikiRes.data.toJikan())

        return jikanRes
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

    private fun KitsuAnime.toJikan(): JikanAnime {
        val attr = attributes
        val kitsuTitle = attr?.canonicalTitle 
            ?: attr?.titles?.en 
            ?: attr?.titles?.en_jp 
            ?: "Unknown"
        val imageUrl = attr?.posterImage?.large 
            ?: attr?.posterImage?.medium 
            ?: attr?.posterImage?.original 
            ?: ""
        val rating = attr?.averageRating?.toDoubleOrNull()?.let { it / 10.0 } ?: 0.0

        return JikanAnime(
            mal_id = id.toIntOrNull() ?: id.hashCode(),
            title = kitsuTitle,
            title_english = attr?.titles?.en,
            images = JikanImages(JikanJpg(imageUrl)),
            episodes = attr?.episodeCount,
            score = rating,
            synopsis = attr?.synopsis
        )
    }

    private fun ShikimoriAnime.toJikan(): JikanAnime {
        val imgPath = image?.original ?: image?.preview ?: ""
        val fullImageUrl = if (imgPath.startsWith("http")) imgPath else "https://shikimori.one$imgPath"

        return JikanAnime(
            mal_id = id,
            title = name,
            title_english = name,
            images = JikanImages(JikanJpg(fullImageUrl)),
            episodes = episodes,
            score = score?.toDoubleOrNull() ?: 0.0,
            synopsis = description ?: russian
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
