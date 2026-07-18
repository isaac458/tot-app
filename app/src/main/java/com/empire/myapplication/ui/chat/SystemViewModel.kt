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

@HiltViewModel
class SystemViewModel @Inject constructor(
    private val botRepository: BotRepository
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
}
