package com.roninx.anime.data.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): Resource<T> {
    return withContext(Dispatchers.IO) {
        try {
            Resource.Success(apiCall.invoke())
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> Resource.Error("No Internet Connection", throwable)
                is SocketTimeoutException -> Resource.Error("Connection Timeout", throwable)
                is HttpException -> {
                    val code = throwable.code()
                    val rawBody = throwable.response()?.errorBody()?.string()
                    val cleanMsg = parseCleanErrorMessage(code, rawBody)
                    Resource.Error(cleanMsg, throwable)
                }
                else -> Resource.Error(throwable.localizedMessage ?: "Unknown Error occurred", throwable)
            }
        }
    }
}

private fun parseCleanErrorMessage(code: Int, rawBody: String?): String {
    if (rawBody.isNullOrBlank()) return "Server Error ($code)"
    return try {
        val json = JSONObject(rawBody)
        if (json.has("errors")) {
            val errorsArray = json.optJSONArray("errors")
            if (errorsArray != null && errorsArray.length() > 0) {
                val firstError = errorsArray.optJSONObject(0)
                val msg = firstError?.optString("message")
                if (!msg.isNullOrBlank()) {
                    if (msg.contains("AniList API has been temporarily disabled", ignoreCase = true) ||
                        msg.contains("disabled due to severe stability issues", ignoreCase = true)
                    ) {
                        return "AniList API is temporarily undergoing maintenance ($code)"
                    }
                    return msg
                }
            }
        }
        if (json.has("message")) {
            val msg = json.getString("message")
            if (msg.contains("AniList API has been temporarily disabled", ignoreCase = true)) {
                return "AniList API is temporarily undergoing maintenance ($code)"
            }
            return msg
        }
        "Server Error ($code)"
    } catch (e: Exception) {
        "Server Error ($code)"
    }
}

