package com.roninx.anime.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.roninx.anime.data.api.GitHubApi
import com.roninx.anime.data.api.GitHubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    private val gitHubApi: GitHubApi,
    @ApplicationContext private val context: Context
) {
    suspend fun checkForUpdate(): UpdateInfo? {
        return try {
            val latestRelease = gitHubApi.getLatestRelease()
            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                "0.0.0"
            }
            
            // Basic version comparison (e.g. "1.0.0" vs "1.0.1")
            if (isNewerVersion(currentVersion, latestRelease.tag_name)) {
                val apkAsset = latestRelease.assets.find { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    UpdateInfo(
                        versionName = latestRelease.tag_name,
                        releaseNotes = latestRelease.body,
                        downloadUrl = apkAsset.browser_download_url
                    )
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewerVersion(current: String?, latest: String): Boolean {
        if (current == null) return true
        val cur = current.replace(Regex("[^0-9.]"), "").split(".").filter { it.isNotEmpty() }
        val lat = latest.replace(Regex("[^0-9.]"), "").split(".").filter { it.isNotEmpty() }
        
        for (i in 0 until minOf(cur.size, lat.size)) {
            val c = cur[i].toIntOrNull() ?: 0
            val l = lat[i].toIntOrNull() ?: 0
            if (l > c) return true
            if (c > l) return false
        }
        return lat.size > cur.size
    }
}

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String
)
