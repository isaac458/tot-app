package com.empire.myapplication.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empire.myapplication.data.remote.UserData
import com.empire.myapplication.data.repository.BotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.empire.myapplication.data.local.MemoryProfile
import com.empire.myapplication.data.local.TootDao
import com.empire.myapplication.core.utils.ThemeManager
import com.empire.myapplication.core.utils.AnalyticsManager

@HiltViewModel
class SystemViewModel @Inject constructor(
    private val botRepository: BotRepository,
    private val tootDao: TootDao,
    private val themeManager: ThemeManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _generatedCode = MutableStateFlow<String?>(null)
    val generatedCode: StateFlow<String?> = _generatedCode.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    private val _isCheckingStatus = MutableStateFlow(true)
    val isCheckingStatus: StateFlow<Boolean> = _isCheckingStatus.asStateFlow()

    private val _unlinkMessage = MutableStateFlow<String?>(null)
    val unlinkMessage: StateFlow<String?> = _unlinkMessage.asStateFlow()

    fun checkLinkStatus() {
        viewModelScope.launch {
            _isCheckingStatus.value = true
            val result = botRepository.getUserStatus()
            if (result.isSuccess) {
                _userData.value = result.getOrNull()
            }
            _isCheckingStatus.value = false
        }
    }

    fun generateLinkCode() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _generatedCode.value = null

            val result = botRepository.generateInstagramCode()
            if (result.isSuccess) {
                _generatedCode.value = result.getOrNull()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "حدث خطأ غير معروف"
            }
            _isLoading.value = false
        }
    }

    fun unlinkAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = botRepository.unlinkAccount()
            if (result.isSuccess) {
                _unlinkMessage.value = result.getOrNull()
                _userData.value = UserData(linked = false)
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
            _isLoading.value = false
        }
    }

    fun clearState() {
        _generatedCode.value = null
        _error.value = null
        _unlinkMessage.value = null
    }

    private val _memoryProfile = MutableStateFlow<MemoryProfile?>(null)
    val memoryProfile: StateFlow<MemoryProfile?> = _memoryProfile.asStateFlow()

    fun loadMemoryProfile() {
        viewModelScope.launch {
            val userId = themeManager.getUserId()
            val profile = tootDao.getMemoryProfileOnce(userId)
            _memoryProfile.value = profile ?: MemoryProfile(userId = userId)
        }
    }

    fun saveMemoryProfile(profile: MemoryProfile) {
        viewModelScope.launch {
            val updated = profile.copy(userId = themeManager.getUserId(), updatedAt = System.currentTimeMillis())
            tootDao.insertMemoryProfile(updated)
            _memoryProfile.value = updated
        }
    }

    fun clearMemoryProfile() {
        viewModelScope.launch {
            val userId = themeManager.getUserId()
            tootDao.deleteMemoryProfile(userId)
            _memoryProfile.value = MemoryProfile(userId = userId)
        }
    }

    fun logMemoryEnabled() = analyticsManager.logMemoryEnabled()
    fun logMemoryDisabled() = analyticsManager.logMemoryDisabled()
    fun logLogout() = analyticsManager.logLogout()
    fun logSettingsOpened() = analyticsManager.logSettingsOpened()
}
