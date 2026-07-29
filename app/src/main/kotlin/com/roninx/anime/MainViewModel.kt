package com.roninx.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.anime.data.repository.UpdateInfo
import com.roninx.anime.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val info = updateRepository.checkForUpdate()
            _updateInfo.value = info
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }
}
