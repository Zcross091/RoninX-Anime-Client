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
    private val prefs = context.getSharedPreferences("roninx_update_prefs", Context.MODE_PRIVATE)

    suspend fun checkForUpdate(): UpdateInfo? {
        return try {
            val latestRelease = gitHubApi.getLatestRelease()
            val apkAsset = latestRelease.assets.find { it.name.endsWith(".apk") } ?: return null

            val releaseCommit = Regex("""\*\*Commit:\*\*\s*([a-f0-9]+)""").find(latestRelease.body)?.groupValues?.get(1) 
                ?: latestRelease.tag_name

            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                "1.0"
            }

            val lastInstalledCommit = prefs.getString("last_installed_commit", "") ?: ""

            val isNewer = if (latestRelease.tag_name == "latest") {
                releaseCommit.isNotEmpty() && releaseCommit != lastInstalledCommit
            } else {
                isNewerVersion(currentVersion, latestRelease.tag_name)
            }

            if (isNewer) {
                val displayVersion = if (latestRelease.tag_name == "latest") {
                    "v1.0-${releaseCommit.take(7)}"
                } else {
                    latestRelease.tag_name
                }

                UpdateInfo(
                    versionName = displayVersion,
                    releaseNotes = latestRelease.body,
                    downloadUrl = apkAsset.browser_download_url,
                    commitSha = releaseCommit
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun markUpdateInstalled(commitSha: String) {
        if (commitSha.isNotEmpty()) {
            prefs.edit().putString("last_installed_commit", commitSha).apply()
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
    val downloadUrl: String,
    val commitSha: String = ""
)
