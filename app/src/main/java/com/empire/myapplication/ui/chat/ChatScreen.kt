package com.empire.myapplication.ui.chat

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.empire.myapplication.data.local.Message
import com.empire.myapplication.data.local.SourceRef
import com.empire.myapplication.ui.theme.*
import android.Manifest
import kotlinx.coroutines.launch
import com.empire.myapplication.ui.profile.ProfileScreen
import java.net.URI
import java.io.File
import androidx.compose.foundation.text.selection.SelectionContainer

private val BorderColor = Color(0x26FFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onLogout: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val speakingMessageId by viewModel.speakingMessageId.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showProfile by remember { mutableStateOf(false) }
    var showSearchPage by remember { mutableStateOf(false) }
    var showSystemPages by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<com.empire.myapplication.data.local.ChatSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<com.empire.myapplication.data.local.ChatSession?>(null) }
    var drawerSearchQuery by remember { mutableStateOf("") }
    val streamingText by viewModel.streamingText.collectAsState()
    val isGuest = viewModel.themeManager.isGuest()

    val themeType by viewModel.themeManager.themeType.collectAsState()
    val themeColor = when(themeType) {
        com.empire.myapplication.core.utils.ThemeType.AURA_BLUE -> CosmicBlue
        com.empire.myapplication.core.utils.ThemeType.AURA_PINK -> SoftPink
        com.empire.myapplication.core.utils.ThemeType.AURA_VIOLET -> CosmicViolet
        com.empire.myapplication.core.utils.ThemeType.AURA_EMERALD -> Color(0xFF10B981)
        else -> CosmicBlue
    }

    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var capturedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var cameraImageUriString by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }

    fun requireAccount(action: () -> Unit) {
        if (isGuest) {
            Toast.makeText(context, "هذه الميزة متاحة فقط للحسابات المسجّلة", Toast.LENGTH_SHORT).show()
        } else {
            action()
        }
    }

    fun openDrawer() {
        keyboardController?.hide()
        scope.launch { drawerState.open() }
    }

    fun closeDrawerAndSearch() {
        showSearchPage = false
        keyboardController?.hide()
        scope.launch { drawerState.close() }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUriString?.let { uriStr ->
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val uri = Uri.parse(uriStr)
                        val bitmap = decodeSampledBitmap(context, uri, 1024, 1024)
                        if (bitmap != null) {
                            scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                capturedImages = capturedImages + bitmap
                            }
                        }
                    } catch (e: Exception) {
                        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "فشل معالجة الصورة", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val launchCamera = {
        try {
            val imageFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
            cameraImageUriString = uri.toString()
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "لا يمكن فتح الكاميرا: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) launchCamera() }

    // ===== الميكروفون: نستخدم SpeechRecognizer مباشرة بدل إطلاق Intent ضمني =====
    // (الطريقة القديمة كانت تطلق Intent لا يوجد من يستقبل نتيجته، ولا تتحقق من توفر خدمة التعرف على الصوت،
    // فكان الزر لا يعمل فعلياً حتى لو ظهرت شاشة التعرف على الصوت لبرهة).
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun stopSpeechRecognizer() {
        speechRecognizer?.let {
            it.stopListening()
            it.destroy()
        }
        speechRecognizer = null
        viewModel.setListening(false)
    }

    val voiceRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull().orEmpty()
            if (recognized.isNotBlank()) {
                inputText = if (inputText.isBlank()) recognized else "$inputText $recognized"
            }
        }
    }

    fun startVoiceActivityFallback() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدّث الآن...")
            }
            voiceRequestLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "التعرف على الصوت غير مدعوم على جهازك", Toast.LENGTH_SHORT).show()
        }
    }

    fun startSpeechRecognition() {
        if (isListening) return
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                startVoiceActivityFallback()
                return
            }
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    stopSpeechRecognizer()
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                        startVoiceActivityFallback()
                    } else if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Toast.makeText(context, "تعذّر التعرف على الصوت، حاول مرة أخرى", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognized = matches?.firstOrNull().orEmpty()
                    if (recognized.isNotBlank()) {
                        inputText = if (inputText.isBlank()) recognized else "$inputText $recognized"
                    }
                    stopSpeechRecognizer()
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            viewModel.setListening(true)
            recognizer.startListening(intent)
        } catch (e: Exception) {
            startVoiceActivityFallback()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSpeechRecognition() else Toast.makeText(context, "يلزم إذن الميكروفون لاستخدام هذه الميزة", Toast.LENGTH_SHORT).show()
    }

    fun onMicButtonClick() {
        if (isListening) {
            stopSpeechRecognizer()
            return
        }
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) startSpeechRecognition() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer?.destroy() }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)) { uris ->
        uris.forEach { uri ->
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val bitmap = decodeSampledBitmap(context, uri, 1024, 1024)
                    if (bitmap != null) {
                        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            capturedImages = capturedImages + bitmap
                        }
                    }
                } catch (e: Exception) { }
            }
        }
    }

    LaunchedEffect(messages.size, streamingText, isTyping) {
        if (messages.isNotEmpty()) {
            kotlinx.coroutines.delay(50)
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                try {
                    listState.scrollToItem(totalItems - 1)
                } catch (e: Exception) { }
            }
        }
    }

    if (showProfile) {
        ProfileScreen(themeManager = viewModel.themeManager, onDismiss = { showProfile = false })
    }

    if (showSystemPages) {
        SystemPagesDialog(
            themeManager = viewModel.themeManager,
            isGuest = isGuest,
            onDismiss = { showSystemPages = false },
            onLogout = {
                if (isGuest) viewModel.clearGuestSessions()
                viewModel.themeManager.clearSessionFlags()
                showSystemPages = false
                onLogout()
            },
            onSwitchAccount = { uid ->
                viewModel.reloadForCurrentUser()
                showSystemPages = false
            }
        )
    }

    if (showSearchPage) {
        SearchSessionsScreen(
            sessions = sessions,
            onDismiss = { showSearchPage = false },
            onSessionClick = {
                scope.launch { closeDrawerAndSearch(); kotlinx.coroutines.delay(120); viewModel.loadChat(it) }
                closeDrawerAndSearch()
            }
        )
    }

    sessionToRename?.let { session ->
        RenameSessionDialog(
            initialTitle = session.title,
            onDismiss = { sessionToRename = null },
            onConfirm = { newTitle ->
                viewModel.updateSessionTitle(session.id, newTitle)
                sessionToRename = null
            }
        )
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("حذف المحادثة") },
            text = { Text("هل تريد حذف \"${session.title}\"؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(session.id)
                    sessionToDelete = null
                }) { Text("حذف", color = Color(0xFFFF5252)) }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(GeminiBg)) {
        Box(modifier = Modifier.fillMaxSize().background(modernGradientBackground()))

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                var pinChangeTrigger by remember { mutableStateOf(0) }
                
                ModalDrawerSheet(
                    modifier = Modifier.width(320.dp),
                    drawerContainerColor = Color(0xFF171717)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF171717))) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // ===== أعلى القائمة: الحساب وإغلاق =====
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isGuest) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x1F4CAF50)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("حساب ضيف", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("سجّل الدخول للمزيد", color = Color(0xFFA1A1AA), fontSize = 11.sp)
                                    }
                                } else {
                                    val userName = viewModel.themeManager.getUserName()
                                    val userEmail = viewModel.themeManager.getUserEmail().ifBlank { "مستخدم مسجّل" }
                                    val avatarUri = viewModel.themeManager.getUserAvatarUri()
                                    
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { 
                                                showProfile = true 
                                                scope.launch { drawerState.close() }
                                            }
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x22FFFFFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (avatarUri != null) {
                                                coil.compose.AsyncImage(
                                                    model = avatarUri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(userName.ifBlank { "مستكشف توت" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(userEmail, color = Color(0xFFA1A1AA), fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                                
                                IconButton(onClick = { scope.launch { drawerState.close() } }) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFFA1A1AA), modifier = Modifier.size(20.dp))
                                }
                            }

                            // ===== زر محادثة جديدة =====
                            Surface(
                                onClick = {
                                    viewModel.createNewSession("محادثة جديدة")
                                    scope.launch { drawerState.close() }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x1AFFFFFF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14FFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("محادثة جديدة", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            // ===== شريط بحث أعلى المحادثات =====
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                onClick = { showSearchPage = true },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF232323),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("بحث في المحادثات...", color = Color(0xFFA1A1AA), fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ===== قائمة المحادثات مصنفة ومثبتة =====
                            val groupedSessions = remember(sessions, pinChangeTrigger) {
                                val now = System.currentTimeMillis()
                                val oneDayMs = 24 * 60 * 60 * 1000L
                                
                                val pinned = mutableListOf<com.empire.myapplication.data.local.ChatSession>()
                                val today = mutableListOf<com.empire.myapplication.data.local.ChatSession>()
                                val yesterday = mutableListOf<com.empire.myapplication.data.local.ChatSession>()
                                val thisWeek = mutableListOf<com.empire.myapplication.data.local.ChatSession>()
                                val older = mutableListOf<com.empire.myapplication.data.local.ChatSession>()
                                
                                for (session in sessions) {
                                    if (viewModel.themeManager.isSessionPinned(session.id)) {
                                        pinned.add(session)
                                    } else {
                                        val diff = now - session.createdAt
                                        when {
                                            diff < oneDayMs -> today.add(session)
                                            diff < 2 * oneDayMs -> yesterday.add(session)
                                            diff < 7 * oneDayMs -> thisWeek.add(session)
                                            else -> older.add(session)
                                        }
                                    }
                                }
                                
                                val list = mutableListOf<Pair<String, List<com.empire.myapplication.data.local.ChatSession>>>()
                                if (pinned.isNotEmpty()) list.add(Pair("📌 مثبت", pinned))
                                if (today.isNotEmpty()) list.add(Pair("📅 اليوم", today))
                                if (yesterday.isNotEmpty()) list.add(Pair("📅 أمس", yesterday))
                                if (thisWeek.isNotEmpty()) list.add(Pair("📅 هذا الأسبوع", thisWeek))
                                if (older.isNotEmpty()) list.add(Pair("📅 محادثات سابقة", older))
                                list
                            }

                            LazyColumn(modifier = Modifier.weight(1f)) {
                                groupedSessions.forEach { (groupTitle, groupItems) ->
                                    item {
                                        Text(
                                            text = groupTitle,
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                                        )
                                    }
                                    items(groupItems, key = { it.id }) { session ->
                                        SessionRow(
                                            title = session.title,
                                            isSelected = session.id == viewModel.getActiveChatId(),
                                            isPinned = viewModel.themeManager.isSessionPinned(session.id),
                                            onClick = {
                                                scope.launch { drawerState.close(); kotlinx.coroutines.delay(120); viewModel.loadChat(session.id) }
                                            },
                                            onRename = { sessionToRename = session },
                                            onDelete = { sessionToDelete = session },
                                            onTogglePin = {
                                                viewModel.themeManager.togglePinSession(session.id)
                                                pinChangeTrigger++
                                            },
                                            onShare = {
                                                Toast.makeText(context, "تم نسخ رابط المشاركة للمحادثة!", Toast.LENGTH_SHORT).show()
                                            },
                                            onMove = {
                                                Toast.makeText(context, "ميزة المجلدات قادمة قريباً!", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // ===== أسفل القائمة: المساعدة فقط =====
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                DrawerBottomItem(label = "❓ المساعدة") {
                                    if (isGuest) {
                                        Toast.makeText(context, "يجب تسجيل الدخول أولاً للحصول على الدعم والمساعدة", Toast.LENGTH_LONG).show()
                                    } else {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                            data = android.net.Uri.parse("mailto:isshakbnhassi@gmail.com")
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "طلب دعم ومساعدة - تطبيق توت")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "لا يوجد تطبيق بريد إلكتروني مثبت لإرسال الرسالة", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("توت", fontWeight = FontWeight.Bold, color = Color.White) },
                        navigationIcon = {
                            BorderedIconButton(onClick = { openDrawer() }) {
                                Icon(Icons.Default.Menu, null, tint = Color.White)
                            }
                        },
                        actions = {
                            BorderedIconButton(onClick = {
                                if (isGuest) {
                                    Toast.makeText(context, "سجّل الدخول للوصول إلى الإعدادات", Toast.LENGTH_SHORT).show()
                                    viewModel.clearGuestSessions()
                                    viewModel.themeManager.clearSessionFlags()
                                    onLogout()
                                } else {
                                    showSystemPages = true
                                }
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (messages.isEmpty()) {
                        WelcomeGrid { inputText = it }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            // مساحة كافية أسفل القائمة حتى لا يغطي مربع الكتابة العائم آخر رسالة
                            contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
                        ) {
                            items(messages, key = { it.id }) { message ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    visible = true
                                }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                                        initialOffsetY = { it / 3 },
                                        animationSpec = tween(400)
                                    ),
                                    exit = fadeOut(animationSpec = tween(400))
                                ) {
                                    MessageItem(
                                        message = message,
                                        streamingText = if (message == messages.last() && isTyping) streamingText else null,
                                        isSpeaking = speakingMessageId == message.id,
                                        isPaused = isPaused && speakingMessageId == message.id,
                                        isLastBotMessage = (message == messages.lastOrNull { it.role != "user" }),
                                        onSpeak = { viewModel.speak(message.id, message.content) },
                                        onPauseSpeak = { viewModel.pauseSpeaking() },
                                        onResumeSpeak = { viewModel.resumeSpeaking() },
                                        onStopSpeak = { viewModel.stopSpeaking() },
                                        onShare = { viewModel.shareChat("توت") },
                                        onRegenerate = { viewModel.regenerateLastResponse() },
                                        getSources = { viewModel.getSourcesForMessage(message.id) }
                                    )
                                }
                            }
                            if (isTyping && streamingText.isBlank()) {
                                item {
                                    ThinkingItem()
                                }
                            }
                        }

                        val showScrollDown by remember {
                            derivedStateOf {
                                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                messages.isNotEmpty() && lastVisible < messages.size - 2
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = showScrollDown,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                                shape = CircleShape,
                                color = Color(0xFF2F2F2F),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // ===== مربع الكتابة العائم: يطفو فوق الرسائل بدل أن يكون جزءاً ثابتاً من التخطيط =====
                    ChatInputArea(
                        text = inputText,
                        onTextChange = { inputText = it },
                        isListening = isListening,
                        isTyping = isTyping,
                        images = capturedImages,
                        onClearImage = { capturedImages = capturedImages.toMutableList().apply { removeAt(it) } },
                        onSend = {
                            if (isTyping) {
                                viewModel.stopGeneration()
                            } else {
                                if (inputText.isNotBlank() || capturedImages.isNotEmpty()) {
                                    viewModel.sendMessage(inputText, capturedImages)
                                    inputText = ""
                                    capturedImages = emptyList()
                                }
                            }
                        },
                        onMicClick = { onMicButtonClick() },
                        onCameraClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        onGalleryClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        themeColor = themeColor,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .imePadding()
                    )
                }
            }
        }
    }
}

@Composable
private fun BorderedIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(38.dp)
            .clip(CircleShape)
            .border(1.dp, BorderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SessionRow(
    title: String,
    isSelected: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
    onMove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF2A2A2A)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFFA1A1AA),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isPinned) "📌 $title" else title,
                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                maxLines = 1,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF1E1E1E))
            ) {
                DropdownMenuItem(
                    text = { Text("✏️ إعادة تسمية", color = Color.White, fontSize = 13.sp) },
                    onClick = { showMenu = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text(if (isPinned) "📌 إلغاء التثبيت" else "📌 تثبيت", color = Color.White, fontSize = 13.sp) },
                    onClick = { showMenu = false; onTogglePin() }
                )
                DropdownMenuItem(
                    text = { Text("📤 مشاركة", color = Color.White, fontSize = 13.sp) },
                    onClick = { showMenu = false; onShare() }
                )
                DropdownMenuItem(
                    text = { Text("📁 نقل إلى مجلد", color = Color.White, fontSize = 13.sp) },
                    onClick = { showMenu = false; onMove() }
                )
                DropdownMenuItem(
                    text = { Text("🗑 حذف", color = Color(0xFFFF5252), fontSize = 13.sp) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun RenameSessionDialog(initialTitle: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل اسم المحادثة") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    streamingText: String? = null,
    isSpeaking: Boolean = false,
    isPaused: Boolean = false,
    isLastBotMessage: Boolean = false,
    onSpeak: () -> Unit = {},
    onPauseSpeak: () -> Unit = {},
    onResumeSpeak: () -> Unit = {},
    onStopSpeak: () -> Unit = {},
    onShare: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    getSources: () -> kotlinx.coroutines.flow.Flow<List<SourceRef>> = { kotlinx.coroutines.flow.flowOf(emptyList()) }
) {
    val isUser = message.role == "user"
    val content = streamingText ?: message.content
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // ===== نمط ChatGPT: بدون فقاعات للردود، وفقاعة خفيفة فقط لرسائل المستخدم =====
        if (isUser) {
            Column(horizontalAlignment = Alignment.End) {
                if (message.imageUri != null) {
                    val bitmap = remember(message.imageUri) {
                        try {
                            val imageBytes = android.util.Base64.decode(message.imageUri, android.util.Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .sizeIn(maxWidth = 240.dp, maxHeight = 240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                if (content.isNotBlank()) {
                    Surface(
                        color = Color(0xFF2F6FED),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.widthIn(max = 300.dp).combinedClickable(
                            onLongClick = {
                                showContextMenu = true
                            },
                            onClick = {}
                        )
                    ) {
                        Box {
                            SelectionContainer {
                                Text(
                                    text = content,
                                    color = Color.White,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { showContextMenu = false },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                    .background(Color(0xFF2A2A2A))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("نسخ النص", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = Color.White) },
                                    onClick = {
                                        showContextMenu = false
                                        clipboard.setText(AnnotatedString(content))
                                        Toast.makeText(context, "تم نسخ النص", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مشاركة", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.White) },
                                    onClick = {
                                        showContextMenu = false
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, content)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الرسالة"))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box {
                Column {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onLongClick = {
                                    showContextMenu = true
                                },
                                onClick = {}
                            )
                    ) {
                        Box(modifier = Modifier.padding(vertical = 4.dp, horizontal = 0.dp)) {
                            SelectionContainer {
                                Column {
                                    MarkdownMessage(text = content, textColor = Color.White, modifier = Modifier.fillMaxWidth())
                                    if (streamingText != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ThreeDotsIndicator()
                                    }
                                }
                            }
                        }
                    }

                    if (message.hasSources) {
                        SourcesClickableIndicator(getSources = getSources)
                    }

                    if (streamingText == null && content.isNotBlank()) {
                        MessageActionsRow(
                            isSpeaking = isSpeaking,
                            isPaused = isPaused,
                            onListen = onSpeak,
                            onPauseListen = onPauseSpeak,
                            onResumeListen = onResumeSpeak,
                            onStopListen = onStopSpeak,
                            onCopy = {
                                clipboard.setText(AnnotatedString(content))
                                Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                            },
                            onShare = onShare
                        )
                    }
                }

                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .background(Color(0xFF2A2A2A))
                ) {
                    DropdownMenuItem(
                        text = { Text("نسخ النص", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = Color.White) },
                        onClick = {
                            showContextMenu = false
                            clipboard.setText(AnnotatedString(content))
                            Toast.makeText(context, "تم نسخ النص", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("تحديد الكل والنسخ", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.SelectAll, null, tint = Color.White) },
                        onClick = {
                            showContextMenu = false
                            clipboard.setText(AnnotatedString(content))
                            Toast.makeText(context, "تم تحديد الكل ونسخ النص", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("مشاركة", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.White) },
                        onClick = {
                            showContextMenu = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, content)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الرسالة"))
                        }
                    )
                    if (isLastBotMessage) {
                        DropdownMenuItem(
                            text = { Text("إعادة توليد الرد", color = Color(0xFF6EA8FF)) },
                            leadingIcon = { Icon(Icons.Default.Refresh, null, tint = Color(0xFF6EA8FF)) },
                            onClick = {
                                showContextMenu = false
                                onRegenerate()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActionsRow(
    isSpeaking: Boolean,
    isPaused: Boolean,
    onListen: () -> Unit,
    onPauseListen: () -> Unit,
    onResumeListen: () -> Unit,
    onStopListen: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isSpeaking) {
            SmallActionIcon(
                icon = Icons.Default.VolumeUp,
                contentDescription = "استماع",
                onClick = onListen
            )
        } else {
            SmallActionIcon(
                icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "استئناف" else "إيقاف مؤقت",
                onClick = { if (isPaused) onResumeListen() else onPauseListen() }
            )
            SmallActionIcon(
                icon = Icons.Default.Stop,
                contentDescription = "إيقاف",
                onClick = onStopListen
            )
        }
        SmallActionIcon(icon = Icons.Default.ContentCopy, contentDescription = "نسخ", onClick = onCopy)
        SmallActionIcon(icon = Icons.Default.Share, contentDescription = "مشاركة المحادثة", onClick = onShare)
    }
}

@Composable
private fun SmallActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .border(1.dp, BorderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White.copy(0.75f), modifier = Modifier.size(15.dp))
    }
}

/**
 * مؤشر المصادر: أيقونة صغيرة ونبضة خفيفة تفتح نافذة المصادر عند النقر
 */
@Composable
private fun SourcesClickableIndicator(getSources: () -> kotlinx.coroutines.flow.Flow<List<SourceRef>>) {
    val sources by remember(getSources) { getSources() }.collectAsState(initial = emptyList())
    if (sources.isEmpty()) return

    var showSourcesDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x12FFFFFF))
            .clickable { showSourcesDialog = true }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Explore,
            contentDescription = null,
            tint = Color(0xFF6EA8FF),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${sources.size} مصادر",
            color = Color(0xFF6EA8FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }

    if (showSourcesDialog) {
        SourcesDialog(sources = sources, onDismiss = { showSourcesDialog = false })
    }
}

/**
 * نافذة المصادر بنمط ChatGPT: تحتوي على شعارات المواقع الرسمية وعناوينها
 */
@Composable
private fun SourcesDialog(sources: List<SourceRef>, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("المصادر المراجعة", color = Color.White, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sources.forEach { source ->
                    val domain = try {
                        val uri = URI(source.url)
                        val hostStr = uri.host ?: source.url
                        if (hostStr.startsWith("www.")) hostStr.substring(4) else hostStr
                    } catch (e: Exception) {
                        source.url
                    }

                    Surface(
                        onClick = {
                            try {
                                var targetUrl = source.url.trim()
                                if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                                    targetUrl = "https://$targetUrl"
                                }
                                uriHandler.openUri(targetUrl)
                            } catch (e: Exception) { }
                        },
                        color = Color(0x1AFFFFFF),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // لوجو الموقع الرسمي (Favicon)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                coil.compose.AsyncImage(
                                    model = "https://www.google.com/s2/favicons?sz=128&domain=$domain",
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = domain,
                                    color = Color.White.copy(0.5f),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color.White.copy(0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = Color(0xFF6EA8FF))
            }
        }
    )
}

@Composable
fun ChatInputArea(
    text: String,
    onTextChange: (String) -> Unit,
    isListening: Boolean,
    isTyping: Boolean,
    images: List<Bitmap>,
    onClearImage: (Int) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (images.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                items(images.size) { i ->
                    Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, BorderColor, RoundedCornerShape(8.dp))) {
                        Image(images[i].asImageBitmap(), null, contentScale = ContentScale.Crop)
                        IconButton(onClick = { onClearImage(i) }, modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(Color.Black.copy(0.6f), CircleShape)) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }

        // مربع كتابة عائم بحواف دائرية وحدود بسيطة، بدون أي مستطيل خلفي إضافي
        Surface(
            color = Color(0xE61D1D1D),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, BorderColor, CircleShape)
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .background(Color(0xFF2A2A2A))
                    ) {
                        DropdownMenuItem(
                            text = { Text("الكاميرا", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, null, tint = Color.White) },
                            onClick = { showMenu = false; onCameraClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("المعرض", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, null, tint = Color.White) },
                            onClick = { showMenu = false; onGalleryClick() }
                        )
                    }
                }

                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("رسالة توت...", color = Color(0xFF9E9E9E)) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White
                    )
                )

                if (isTyping) {
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(0.5f), CircleShape)
                            .clickable { onSend() }, // سنقوم بتعديل ViewModel ليوقف الإرسال عند استدعاء onSend والحالة typing
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "إيقاف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (text.isNotBlank() || images.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, BorderColor, CircleShape)
                            .clickable { onSend() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = themeColor, modifier = Modifier.size(18.dp))
                    }
                } else {
                    PulsingMicButton(isListening = isListening, onClick = onMicClick)
                }
            }
        }
    }
}

@Composable
fun WelcomeGrid(onPromptClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("✨", fontSize = 48.sp)
        Text("كيف يمكنني مساعدتك؟", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(24.dp))
        val prompts = listOf("اكتبي لي قصة", "خطة يومية", "نصيحة تقنية", "حل مسألة كود")
        for (p in prompts) {
            Surface(
                onClick = { onPromptClick(p) },
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(p, color = Color.White, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun DrawerMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = color)
    }
}

private fun decodeSampledBitmap(context: android.content.Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            context.contentResolver.openInputStream(uri)?.use { inputStream2 ->
                BitmapFactory.decodeStream(inputStream2, null, options)
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

@Composable
fun PulsingMicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isListening) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Box(
            modifier = modifier
                .size(36.dp)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    } else {
        Box(
            modifier = modifier
                .padding(2.dp)
                .size(36.dp)
                .clip(CircleShape)
                .border(1.dp, BorderColor, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ThinkingItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            color = Color(0x0FFFFFFF),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = 200.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .graphicsLayer { alpha = pulseAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("توت تفكّر...", color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                ThreeDotsIndicator()
            }
        }
    }
}

@Composable
fun ThreeDotsIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(6.dp).graphicsLayer { scaleX = dot1Scale; scaleY = dot1Scale }.clip(CircleShape).background(Color.White))
        Box(modifier = Modifier.size(6.dp).graphicsLayer { scaleX = dot2Scale; scaleY = dot2Scale }.clip(CircleShape).background(Color.White))
        Box(modifier = Modifier.size(6.dp).graphicsLayer { scaleX = dot3Scale; scaleY = dot3Scale }.clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun DrawerBottomItem(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
