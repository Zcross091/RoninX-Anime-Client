package com.roninx.anime.data.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun downloadAndInstall(url: String): Flow<DownloadStatus> = callbackFlow {
        // Download to a private folder and share via FileProvider
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "roninx-update.apk")
        if (destinationFile.exists()) destinationFile.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("RoninX Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)

        val timer = java.util.Timer()
        timer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                val query = DownloadManager.Query().setFilterById(downloadId)
                try {
                    downloadManager.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                            val status = if (statusIndex != -1) cursor.getInt(statusIndex) else -1
                            val downloaded = if (downloadedIndex != -1) cursor.getLong(downloadedIndex) else 0L
                            val total = if (totalIndex != -1) cursor.getLong(totalIndex) else 0L
                            
                            if (total > 0) {
                                trySend(DownloadStatus.Progress(downloaded.toFloat() / total.toFloat()))
                            }

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                timer.cancel()
                                val contentUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    destinationFile
                                )
                                trySend(DownloadStatus.Finished(contentUri))
                                close()
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                timer.cancel()
                                trySend(DownloadStatus.Error("Download failed"))
                                close()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore timer polling errors
                }
            }
        }, 0, 500)

        awaitClose {
            timer.cancel()
        }
    }

    fun canInstallPackages(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun installApk(uri: Uri) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && !canInstallPackages()) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}

sealed class DownloadStatus {
    data class Progress(val progress: Float) : DownloadStatus()
    data class Finished(val uri: Uri) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}
