package com.roninx.anime.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface GitHubApi {
    @Headers("User-Agent: RoninX-Android-App")
    @GET("repos/Zcross091/RoninX-Anime-Client/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease

    @Headers("Accept: application/vnd.github+json", "User-Agent: RoninX-Android-App")
    @POST("repos/Zcross091/RoninX-Anime-Client/actions/workflows/mine-episode.yml/dispatches")
    suspend fun dispatchMineWorkflow(
        @Header("Authorization") authHeader: String? = null,
        @Body request: GitHubDispatchPayload
    ): Response<Unit>

    @GET
    suspend fun getRawMinedStream(@Url url: String): MinedStreamPayload
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

data class GitHubDispatchPayload(
    val ref: String = "main",
    val inputs: Map<String, String>
)

data class MinedStreamPayload(
    val title: String?,
    val episode: Int?,
    val url: String?,
    val timestamp: Long?,
    val status: String?
)

