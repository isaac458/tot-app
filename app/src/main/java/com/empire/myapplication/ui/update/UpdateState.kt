package com.empire.myapplication.ui.update

import com.empire.myapplication.data.model.UpdateInfo

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo, val isForce: Boolean) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
    
    data class Downloading(val progress: Int, val currentMB: Double, val totalMB: Double, val speed: String) : UpdateState()
    data class DownloadCompleted(val apkPath: String) : UpdateState()
    data class DownloadError(val message: String) : UpdateState()
}
