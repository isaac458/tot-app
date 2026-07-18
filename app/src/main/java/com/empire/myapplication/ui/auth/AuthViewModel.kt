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
import com.empire.myapplication.core.utils.AnalyticsManager
import com.empire.myapplication.data.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val analyticsManager: AnalyticsManager,
    private val repository: AiRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
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
                    val displayName = user.displayName ?: ""
                    val email = user.email ?: ""
                    val photoUrl = user.photoUrl?.toString()
                    if (!isNewUser && themeManager.getUserName().isBlank()) {
                        themeManager.setUserName(displayName)
                    }
                    themeManager.setUserEmail(email)
                    themeManager.saveAccount(user.uid, themeManager.getUserName().ifBlank { displayName }, email, photoUrl)
                    
                    // تسجيل بصمة الجهاز والبيانات في Firestore
                    analyticsManager.logUserPresence(context)

                    // حفظ توكن FCM للإشعارات
                    saveFcmToken(user.uid)

                    // إرسال رسالة ترحيبية إذا كان مستخدماً جديداً
                    if (isNewUser) {
                        sendWelcomeMessage(user.uid, displayName.ifBlank { "مستخدم جديد" })
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

    private fun sendWelcomeMessage(uid: String, name: String) {
        viewModelScope.launch {
            try {
                val chatId = repository.createNewSession("الترحيب بـ $name", uid)
                repository.sendMessage(
                    chatId,
                    "مرحباً توت، أنا مستخدم جديد اسمي $name. هل يمكنك الترحيب بي بأسلوبك اللطيف كفراشة ذكية ومساعدة؟ أخبريني باختصار عما يمكنك فعله."
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to send welcome message: ${e.message}")
            }
        }
    }
    
    fun saveOnboardingData(name: String, age: String, gender: String, acceptedTerms: Boolean, onComplete: () -> Unit) {
        if (!acceptedTerms) return
        themeManager.setUserName(name)
        themeManager.setUserAge(age)
        themeManager.setUserGender(gender)
        themeManager.setAcceptedTerms(true)
        onComplete()
    }

    private fun saveFcmToken(uid: String) {
        viewModelScope.launch {
            try {
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                Log.d("AuthViewModel", "FCM token saved: ${token.take(10)}...")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to save FCM token: ${e.message}")
            }
        }
    }
}
