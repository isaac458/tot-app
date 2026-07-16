package com.empire.myapplication.ui.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.empire.myapplication.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    application: Application,
    private val repository: UpdateRepository
) : AndroidViewModel(application) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            val result = repository.checkUpdate()
            result.onSuccess { info ->
                val currentVersionCode = repository.getAppVersionCode(getApplication())
                if (info.latestVersionCode > currentVersionCode) {
                    val isForce = info.forceUpdate || info.minimumVersionCode > currentVersionCode
                    _updateState.value = UpdateState.UpdateAvailable(info, isForce)
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            }.onFailure {
                _updateState.value = UpdateState.Error(it.message ?: "Network error")
            }
        }
    }

    fun startDownload(url: String) {
        val destination = File(getApplication<Application>().cacheDir, "update.apk")
        if (destination.exists()) destination.delete()

        viewModelScope.launch {
            repository.downloadApk(url, destination).collect { status ->
                when (status) {
                    is UpdateRepository.DownloadStatus.Progress -> {
                        val currentMB = status.currentBytes.toDouble() / (1024 * 1024)
                        val totalMB = status.totalBytes.toDouble() / (1024 * 1024)
                        _updateState.value = UpdateState.Downloading(
                            status.progress,
                            currentMB,
                            totalMB,
                            "Calculating..." // Can be improved with timing
                        )
                    }
                    is UpdateRepository.DownloadStatus.Finished -> {
                        _updateState.value = UpdateState.DownloadCompleted(status.path)
                    }
                    is UpdateRepository.DownloadStatus.Error -> {
                        _updateState.value = UpdateState.DownloadError(status.message)
                    }
                }
            }
        }
    }
}
