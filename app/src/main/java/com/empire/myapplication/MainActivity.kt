package com.empire.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.empire.myapplication.ui.update.UpdateViewModel
import com.empire.myapplication.ui.update.UpdateDialog
import com.empire.myapplication.ui.update.UpdateState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import com.empire.myapplication.ui.navigation.AppNavigation
import com.empire.myapplication.core.utils.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val updateViewModel: UpdateViewModel by viewModels()

    @javax.inject.Inject
    lateinit var themeManager: ThemeManager

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
        }

        // فحص التحديثات عند التشغيل والعودة للتطبيق
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateViewModel.checkForUpdate()
            }
        }
        
        setContent {
            val startDestination = if (themeManager.isLoggedIn()) "chat" else "auth"
            val updateState by updateViewModel.updateState.collectAsState()

            MyApplicationTheme {
                AppNavigation(startDestination)
                
                // عرض نافذة التحديث
                UpdateDialog(
                    state = updateState,
                    onDownloadClick = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    },
                    onDismiss = { updateViewModel.resetState() },
                    onInstallStarted = { updateViewModel.resetState() }
                )
            }
        }
    }
}
