package com.roninx.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.repository.UpdateInfo
import com.roninx.anime.data.repository.UpdateRepository
import com.roninx.anime.data.util.DownloadStatus
import com.roninx.anime.data.util.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val info = updateRepository.checkForUpdate()
            _updateInfo.value = info
        }
    }

    fun startUpdate(info: UpdateInfo) {
        viewModelScope.launch {
            updateManager.downloadAndInstall(info.downloadUrl).collectLatest { status ->
                when (status) {
                    is DownloadStatus.Progress -> {
                        _downloadProgress.value = status.progress
                    }
                    is DownloadStatus.Finished -> {
                        _downloadProgress.value = 1f
                        updateRepository.markUpdateInstalled(info.commitSha)
                        updateManager.installApk(status.uri)
                    }
                    is DownloadStatus.Error -> {
                        _downloadProgress.value = null
                        _updateError.value = status.message
                    }
                }
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
        _downloadProgress.value = null
    }
    
    fun dismissError() {
        _updateError.value = null
    }
}
