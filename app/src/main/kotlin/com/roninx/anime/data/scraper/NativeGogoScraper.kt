package com.roninx.anime.data.scraper

import android.util.Base64
import com.roninx.anime.data.api.StreamLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeGogoScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val defaultDomains = listOf(
        "https://anitaku.pe",
        "https://gogoanime3.co",
        "https://gogoanime.or.at"
    )

    private val fastClient: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    /**
     * Mines stream link directly on-device in < 1-2 seconds (AniBay architecture).
     */
    suspend fun extractStreamLink(query: String, episodeNumber: Int): StreamLink? = withContext(Dispatchers.IO) {
        val cleanQuery = query.lowercase().trim()
        val querySlug = cleanQuery.replace(Regex("[^a-z0-9]+"), "-").replace(Regex("^-+|-+$"), "")

        // ⚡ Stage 0: RoninX Vercel Serverless Scraper (< 1 sec)
        val vercelStream = fetchFromRoninVercelApi(query, episodeNumber)
        if (vercelStream != null) {
            return@withContext vercelStream
        }

        // ⚡ Stage 0.5: Instant Consumet / Anime API Provider (~200ms AniBay speed)
        val apiStream = fetchFromConsumetApi(querySlug, episodeNumber)
        if (apiStream != null) {
            return@withContext apiStream
        }

        for (domain in defaultDomains) {
            try {
                // ── Stage 1: Speculative Direct Episode GET (Instant Match ~300ms) ──
                val directUrl = "$domain/$querySlug-episode-$episodeNumber"
                val directIframe = fetchIframeFromPage(directUrl, domain)
                if (!directIframe.isNullOrBlank()) {
                    val mediaUrl = extractDirectMediaFromIframe(directIframe, directUrl)
                    if (!mediaUrl.isNullOrBlank()) {
                        return@withContext StreamLink(
                            title = "$query - Ep $episodeNumber",
                            url = mediaUrl,
                            type = "native_direct"
                        )
                    }
                }

                // ── Stage 2: Catalogue Search via HTTP ──
                val searchUrl = "$domain/search.html?keyword=${URLEncoder.encode(query, "UTF-8")}"
                val searchHtml = fetchHtml(searchUrl, domain) ?: continue
                val doc = Jsoup.parse(searchHtml)

                var categoryHref = ""
                doc.select("ul.items li p.name a").forEach { el ->
                    val text = el.text().lowercase().trim()
                    val href = el.attr("href")
                    if (text == cleanQuery || href.contains(querySlug)) {
                        categoryHref = href
                    }
                }

                if (categoryHref.isBlank()) {
                    categoryHref = doc.select("ul.items li p.name a").firstOrNull()?.attr("href") ?: ""
                }

                if (categoryHref.isBlank()) continue

                val categoryUrl = if (categoryHref.startsWith("http")) categoryHref else "$domain${if (categoryHref.startsWith("/")) "" else "/"}$categoryHref"
                val seriesSlug = categoryHref.replace("/category/", "").replace("/anime/", "").replace("/", "")

                // ── Stage 3: Gogo CDN AJAX Engine for Ongoing / Complex Shows ──
                var exactEpUrl: String? = null
                val catHtml = fetchHtml(categoryUrl, searchUrl)
                if (!catHtml.isNullOrBlank()) {
                    val catDoc = Jsoup.parse(catHtml)
                    val movieId = catDoc.select("#movie_id, input#movie_id").`val`().ifBlank { catDoc.select("#movie_id").attr("value") }
                    val aliasId = catDoc.select("#alias_anime, input#alias_anime").`val`().ifBlank { seriesSlug }
                    val epEnd = catDoc.select("ul#episode_page li a").lastOrNull()?.attr("ep_end") ?: "9999"

                    if (movieId.isNotBlank()) {
                        val ajaxUrl = "https://ajax.gogocdn.net/ajax/load-list-episode?ep_start=0&ep_end=$epEnd&id=$movieId&default_ep=0&alias=$aliasId"
                        val ajaxHtml = fetchHtml(ajaxUrl, categoryUrl)
                        if (!ajaxHtml.isNullOrBlank()) {
                            val ajaxDoc = Jsoup.parse(ajaxHtml)
                            ajaxDoc.select("#episode_related li a").forEach { el ->
                                val href = el.attr("href").trim()
                                val match = Regex("-episode-(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE).find(href)
                                if (match != null) {
                                    val ep = match.groupValues[1].toFloatOrNull()
                                    if (ep == episodeNumber.toFloat()) {
                                        exactEpUrl = if (href.startsWith("http")) href else "$domain${if (href.startsWith("/")) "" else "/"}$href"
                                    }
                                }
                            }
                        }
                    }
                }

                if (exactEpUrl == null) {
                    exactEpUrl = "$domain/$seriesSlug-episode-$episodeNumber"
                }

                // ── Stage 4: Extract Stream Iframe and parse direct video media ──
                val streamIframe = fetchIframeFromPage(exactEpUrl!!, categoryUrl)
                if (!streamIframe.isNullOrBlank()) {
                    val mediaUrl = extractDirectMediaFromIframe(streamIframe, exactEpUrl!!)
                    if (!mediaUrl.isNullOrBlank()) {
                        return@withContext StreamLink(
                            title = "$query - Ep $episodeNumber",
                            url = mediaUrl,
                            type = "native_ajax"
                        )
                    }
                }
            } catch (e: Exception) {
                // Try next mirror domain
                continue
            }
        }

        return@withContext null
    }

    private fun fetchIframeFromPage(pageUrl: String, referer: String): String? {
        val html = fetchHtml(pageUrl, referer) ?: return null
        val doc = Jsoup.parse(html)
        val iframe = doc.select(".play-video iframe, div.anime_video_body iframe, iframe").firstOrNull()?.attr("src")
        if (!iframe.isNullOrBlank()) {
            return if (iframe.startsWith("http")) iframe else "https:$iframe"
        }
        return null
    }

    private fun extractDirectMediaFromIframe(iframeUrl: String, pageUrl: String): String? {
        if (iframeUrl.contains(".m3u8", ignoreCase = true) || iframeUrl.contains(".mp4", ignoreCase = true)) {
            return iframeUrl
        }

        val iframeHtml = fetchHtml(iframeUrl, pageUrl) ?: return null

        val fileRegex = Regex("""(?:file|source|src)\s*:\s*["'](https?://[^"']+\.(?:m3u8|mp4)[^"']*)["']""", RegexOption.IGNORE_CASE)
        val match1 = fileRegex.find(iframeHtml)
        if (match1 != null) {
            return match1.groupValues[1]
        }

        val m3u8Regex = Regex("""(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)""", RegexOption.IGNORE_CASE)
        val match2 = m3u8Regex.find(iframeHtml)
        if (match2 != null) {
            return match2.groupValues[1]
        }

        val mp4Regex = Regex("""(https?://[^\s"'<>]+\.mp4[^\s"'<>]*)""", RegexOption.IGNORE_CASE)
        val match3 = mp4Regex.find(iframeHtml)
        if (match3 != null) {
            return match3.groupValues[1]
        }

        return null
    }

    private fun fetchFromRoninVercelApi(query: String, episodeNumber: Int): StreamLink? {
        return try {
            val encodedTitle = URLEncoder.encode(query, "UTF-8")
            val url = "https://roninx-app.vercel.app/api/stream?title=$encodedTitle&episode=$episodeNumber"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()
            fastClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    val json = JSONObject(body)
                    val streamUrl = json.optString("url")
                    if (!streamUrl.isNullOrBlank()) {
                        StreamLink(
                            title = "$query - Ep $episodeNumber",
                            url = streamUrl,
                            type = "roninx_vercel_api"
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromConsumetApi(slug: String, episodeNumber: Int): StreamLink? {
        val slugs = listOf(
            "$slug-episode-$episodeNumber",
            "$slug-tv-episode-$episodeNumber",
            "${slug.replace("-tv", "")}-episode-$episodeNumber"
        ).distinct()

        val apiBases = listOf(
            "https://consumet-api-v2.vercel.app/anime/gogoanime/watch/",
            "https://api.consumet.org/anime/gogoanime/watch/",
            "https://consumet-api-clone.vercel.app/anime/gogoanime/watch/",
            "https://gogoanime-api.vercel.app/watch/"
        )

        for (epSlug in slugs) {
            for (base in apiBases) {
                try {
                    val url = "$base$epSlug"
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", userAgent)
                        .build()
                    fastClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: return@use
                            val json = JSONObject(body)
                            val sources = json.optJSONArray("sources")
                            if (sources != null && sources.length() > 0) {
                                val firstSource = sources.getJSONObject(0)
                                val streamUrl = firstSource.optString("url")
                                if (!streamUrl.isNullOrBlank() && (streamUrl.contains(".m3u8") || streamUrl.contains(".mp4"))) {
                                    return StreamLink(
                                        title = "$slug - Ep $episodeNumber",
                                        url = streamUrl,
                                        type = "consumet_api"
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return null
    }

    private fun fetchHtml(url: String, referer: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Referer", referer)
                .build()
            fastClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
