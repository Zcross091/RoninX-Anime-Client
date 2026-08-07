package com.roninx.anime.data.repository

import com.roninx.anime.data.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun getChapterPages(title: String, chapter: Int): Resource<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val url = "https://roninx-app.vercel.app/api/manga?title=$encodedTitle&chapter=$chapter"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use Resource.Error("Empty response from server")
                    val json = JSONObject(body)
                    val pagesArray = json.optJSONArray("pages")
                    if (pagesArray != null && pagesArray.length() > 0) {
                        val pagesList = mutableListOf<String>()
                        for (i in 0 until pagesArray.length()) {
                            val imgUrl = pagesArray.getString(i)
                            if (imgUrl.isNotBlank()) {
                                pagesList.add(imgUrl)
                            }
                        }
                        Resource.Success(pagesList)
                    } else {
                        Resource.Error("No pages found for chapter $chapter")
                    }
                } else {
                    Resource.Error("HTTP Error ${response.code}")
                }
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch chapter pages")
        }
    }
}
