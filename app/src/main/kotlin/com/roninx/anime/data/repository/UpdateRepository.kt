package com.roninx.anime.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.roninx.anime.BuildConfig
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

            val bodyCommit = Regex("""\*\*Commit:\*\*\s*([a-f0-9]+)""").find(latestRelease.body)?.groupValues?.get(1)
            val tagCommit = if (latestRelease.tag_name.contains("-")) latestRelease.tag_name.substringAfter("-") else ""
            val releaseCommit = bodyCommit ?: tagCommit

            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                "1.0"
            }

            val currentSha = try { BuildConfig.COMMIT_SHA } catch (e: Exception) { "" }

            // 1. If running current build SHA, user is already on this exact build
            if (currentSha.isNotEmpty() && currentSha != "unknown") {
                if ((releaseCommit.isNotEmpty() && releaseCommit.startsWith(currentSha)) ||
                    (tagCommit.isNotEmpty() && tagCommit.startsWith(currentSha)) ||
                    (currentSha.startsWith(tagCommit) && tagCommit.isNotEmpty())) {
                    return null
                }
            }

            // 2. Base version comparison (clean semver without -commit suffix)
            val cleanTagName = latestRelease.tag_name.trimStart('v').substringBefore('-')
            val tagIsNewer = isNewerVersion(currentVersion, cleanTagName)

            // 3. Commit SHA comparison if version is same but commit is newer
            val lastInstalledCommit = prefs.getString("last_installed_commit", "") ?: ""
            val commitIsNewer = !tagIsNewer && cleanTagName == currentVersion.trimStart('v') &&
                    releaseCommit.isNotEmpty() && releaseCommit != lastInstalledCommit &&
                    (currentSha.isEmpty() || currentSha == "unknown" || !releaseCommit.startsWith(currentSha))

            val isNewer = tagIsNewer || commitIsNewer

            if (isNewer) {
                val displayVersion = if (latestRelease.tag_name == "latest") {
                    "v${currentVersion}-${releaseCommit.take(7)}"
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
        val cleanCurrent = current.trimStart('v').substringBefore('-')
        val cleanLatest = latest.trimStart('v').substringBefore('-')

        val curParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val latParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

        if (curParts.isEmpty() || latParts.isEmpty()) {
            return cleanLatest != cleanCurrent
        }

        val maxLen = maxOf(curParts.size, latParts.size)
        for (i in 0 until maxLen) {
            val c = curParts.getOrElse(i) { 0 }
            val l = latParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (c > l) return false
        }
        return false
    }
}

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val commitSha: String = ""
)
