package com.empire.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.empire.myapplication.ui.theme.MyApplicationTheme
import com.empire.myapplication.ui.chat.ChatScreen
import com.empire.myapplication.data.repository.AiRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject
    lateinit var themeManager: com.empire.myapplication.core.utils.ThemeManager

    @javax.inject.Inject
    lateinit var aiRepository: AiRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // تحسين استجابة لوحة المفاتيح
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // محادثات وضع الضيف لا تُحفظ أبداً بشكل دائم: أي عملية إطلاق جديدة للتطبيق
        // وكان آخر وضع معروف هو "ضيف" تمسح كل بيانات ذلك الضيف قبل عرض أي شاشة.
        if (themeManager.isGuest()) {
            lifecycleScope.launch {
                aiRepository.clearSessionsForOwner("guest")
            }
            themeManager.clearGuestProfileData()
            themeManager.clearSessionFlags()
        }
        
        setContent {
            val startDestination = if (themeManager.isLoggedIn()) "chat" else "auth"
            MyApplicationTheme {
                com.empire.myapplication.ui.navigation.AppNavigation(startDestination)
            }
        }
    }
}
