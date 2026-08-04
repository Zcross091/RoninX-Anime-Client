package com.roninx.anime.data.repository

import com.roninx.anime.BuildConfig
import com.roninx.anime.data.api.GitHubApi
import com.roninx.anime.data.api.GitHubDispatchPayload
import com.roninx.anime.data.api.MinedStreamPayload
import com.roninx.anime.data.api.RoninProxyApi
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubMinerRepository @Inject constructor(
    private val gitHubApi: GitHubApi,
    private val roninProxyApi: RoninProxyApi
) {
    /**
     * Dispatches the GitHub Actions workflow to mine the specified anime episode on cloud runner.
     */
    suspend fun dispatchMiningJob(animeTitle: String, episodeNumber: Int, token: String? = null): Boolean {
        // 1. Try direct GitHub API dispatch if PAT token exists
        try {
            val activeToken = token?.ifBlank { null } ?: BuildConfig.GITHUB_PAT.ifBlank { null }
            if (!activeToken.isNullOrBlank()) {
                val authHeader = if (activeToken.startsWith("Bearer ") || activeToken.startsWith("token ")) activeToken else "Bearer $activeToken"
                val payload = GitHubDispatchPayload(
                    ref = "main",
                    inputs = mapOf(
                        "anime_title" to animeTitle,
                        "episode_number" to episodeNumber.toString()
                    )
                )
                val response = gitHubApi.dispatchMineWorkflow(authHeader, payload)
                if (response.isSuccessful || response.code() == 204) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback: Dispatch via Ronin Proxy Vercel Relay (server-side GitHub secret token)
        return try {
            roninProxyApi.triggerMiner(animeTitle, episodeNumber)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Polls raw.githubusercontent.com for cache/latest_stream.json until the stream link
     * matching animeTitle & episodeNumber is available, or timeout is reached.
     */
    suspend fun pollMinedStream(
        animeTitle: String,
        episodeNumber: Int,
        startTimeMs: Long = System.currentTimeMillis(),
        maxWaitSeconds: Int = 45,
        onProgress: ((elapsedSeconds: Int) -> Unit)? = null
    ): MinedStreamPayload? {
        val pollIntervalMs = 3000L
        val maxAttempts = (maxWaitSeconds * 1000L / pollIntervalMs).toInt()
        val cleanTitle = animeTitle.lowercase().trim()

        val rawUrl = "https://raw.githubusercontent.com/Zcross091/RoninX-Anime-Client/main/cache/latest_stream.json"

        for (attempt in 1..maxAttempts) {
            val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
            onProgress?.invoke(elapsedSec)

            try {
                val cacheBustedUrl = "$rawUrl?t=${System.currentTimeMillis()}"
                val result = gitHubApi.getRawMinedStream(cacheBustedUrl)

                if (result.timestamp != null && result.timestamp > startTimeMs - 5000) {
                    val resultTitle = result.title?.lowercase()?.trim() ?: ""
                    if ((resultTitle == cleanTitle || resultTitle.contains(cleanTitle) || cleanTitle.contains(resultTitle)) &&
                        result.episode == episodeNumber
                    ) {
                        if (!result.url.isNullOrBlank()) {
                            return result
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore transient network errors during git commit
            }

            delay(pollIntervalMs)
        }
        return null
    }
}
