package com.empire.myapplication.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empire.myapplication.core.utils.ThemeManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val themeManager: ThemeManager
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setError(message: String?) {
        _error.value = message
    }

    fun firebaseAuthWithGoogle(idToken: String, onSuccess: (isNewUser: Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                
                val user = authResult.user
                val isNewUser = authResult.additionalUserInfo?.isNewUser == true
                
                if (user != null) {
                    // ربط الحساب بمعرّفه الحقيقي (uid) حتى تكون محادثاته وبياناته
                    // معزولة تماماً عن أي حساب آخر أو وضع الضيف على نفس الجهاز.
                    themeManager.setUserId(user.uid)
                    themeManager.setGuest(false)
                    themeManager.setLoggedIn(true)
                    if (!isNewUser && themeManager.getUserName().isBlank()) {
                        themeManager.setUserName(user.displayName ?: "")
                    }
                    onSuccess(isNewUser)
                } else {
                    _error.value = "فشل الحصول على بيانات المستخدم"
                }
            } catch (e: Exception) {
                _error.value = "خطأ في مصادقة فايربيس: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun setGuestMode(onComplete: () -> Unit) {
        // كل جلسة ضيف جديدة تبدأ نظيفة تماماً: لا بيانات ملف شخصي ولا بقايا من جلسة ضيف سابقة.
        themeManager.setUserId("guest")
        themeManager.clearGuestProfileData()
        themeManager.setGuest(true)
        themeManager.setLoggedIn(true)
        onComplete()
    }
    
    fun saveOnboardingData(name: String, age: String, gender: String, acceptedTerms: Boolean, onComplete: () -> Unit) {
        if (!acceptedTerms) return
        themeManager.setUserName(name)
        themeManager.setUserAge(age)
        themeManager.setUserGender(gender)
        themeManager.setAcceptedTerms(true)
        onComplete()
    }
}
