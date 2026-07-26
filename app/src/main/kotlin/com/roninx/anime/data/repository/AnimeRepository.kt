package com.roninx.anime.data.repository

import com.roninx.anime.data.api.JikanApi
import com.roninx.anime.data.api.JikanAnime
import com.roninx.anime.data.api.RoninProxyApi
import com.roninx.anime.data.api.StreamLink
import com.roninx.anime.data.util.Resource
import com.roninx.anime.data.util.TitleUtils
import com.roninx.anime.data.util.safeApiCall
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepository @Inject constructor(
    private val jikanApi: JikanApi,
    private val roninProxyApi: RoninProxyApi
) {
    suspend fun getTopAiring(): Resource<List<JikanAnime>> {
        return safeApiCall { jikanApi.getSeasonNow().data }
    }

    suspend fun getActionAnime(): Resource<List<JikanAnime>> {
        return safeApiCall { jikanApi.getAnimeByGenre("1").data }
    }

    suspend fun getRomanceAnime(): Resource<List<JikanAnime>> {
        return safeApiCall { jikanApi.getAnimeByGenre("22").data }
    }

    suspend fun getAnimeByGenre(genreId: String): Resource<List<JikanAnime>> {
        return safeApiCall { jikanApi.getAnimeByGenre(genreId).data }
    }

    suspend fun searchAnime(query: String): Resource<List<JikanAnime>> {
        return safeApiCall { jikanApi.searchAnime(query).data }
    }

    suspend fun getAnimeFull(id: Int): Resource<JikanAnime> {
        return safeApiCall { jikanApi.getAnimeFull(id).data }
    }

    suspend fun getStreamLinks(title: String, originalTitle: String?, synonyms: List<String>, episode: Int): Resource<List<StreamLink>> {
        val variants = TitleUtils.buildVariants(listOf(title, originalTitle) + synonyms)
        val variantsJson = Gson().toJson(variants)
        return safeApiCall { roninProxyApi.getStreamLinks(episode, originalTitle ?: title, variantsJson) }
    }

    suspend fun triggerMiner(title: String, episode: Int) {
        // Background task, we don't necessarily need to return Resource here but let's be safe
        safeApiCall { roninProxyApi.triggerMiner(title, episode) }
    }
}
