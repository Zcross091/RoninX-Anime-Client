package com.roninx.anime.data.api

import retrofit2.http.GET
import retrofit2.http.Headers

interface GitHubApi {
    @Headers("User-Agent: RoninX-Android-App")
    @GET("repos/Zcross091/RoninX-Anime-Client/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}

data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long
)
