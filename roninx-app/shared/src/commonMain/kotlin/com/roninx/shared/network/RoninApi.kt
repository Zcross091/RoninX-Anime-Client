package com.roninx.shared.network

import com.roninx.shared.models.Anime
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StreamLink(
    val title: String,
    val url: String,
    val type: String
)

class RoninApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private val proxyBaseUrl = "https://ronin-api-proxy.vercel.app/api"

    // Ported from App.jsx: buildVariants
    fun buildVariants(title: String): List<String> {
        val base = title.lowercase().trim()
        val withSpaces = base.replace(Regex("[^a-z0-9]+"), " ").replace(Regex("\\s+"), " ").trim()
        val noSymbols = base.replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), " ").trim()
        val noSpaces = withSpaces.replace(Regex("\\s+"), "")
        val noSeason = withSpaces.replace(Regex("\\s*(season|part|tv|cour)\\s*\\d*\\s*$", RegexOption.IGNORE_CASE), "").trim()
        val withHyphens = withSpaces.replace(" ", "-")
        val pureAlphaNumeric = base.replace(Regex("[^a-z0-9]"), "")

        val subs = listOf(base, withSpaces, noSymbols, noSpaces, noSeason, withHyphens, pureAlphaNumeric)
        return (subs + subs.map { "$it dub" }).distinct()
    }

    suspend fun fetchStreams(title: String, episode: Int): List<StreamLink> {
        val variants = buildVariants(title)
        return try {
            client.get("$proxyBaseUrl/db") {
                parameter("episode", episode)
                parameter("title", title)
                parameter("searchVariants", Json.encodeToString(variants))
            }.body()
        } catch (e: Exception) {
            // If DB query fails, trigger miner as fallback
            triggerMiner(title, episode)
            emptyList()
        }
    }

    suspend fun triggerMiner(title: String, episode: Int) {
        try {
            client.get("$proxyBaseUrl/trigger-miner") {
                parameter("title", title)
                parameter("episode", episode)
            }
        } catch (e: Exception) { }
    }
}
