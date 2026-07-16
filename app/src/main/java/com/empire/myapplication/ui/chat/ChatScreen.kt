package com.empire.myapplication.ui.chat

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
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

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    if (showProfile) {
        ProfileScreen(themeManager = viewModel.themeManager, onDismiss = { showProfile = false })
    }

    if (showSystemPages) {
        SystemPagesDialog(
            isGuest = isGuest,
            onDismiss = { showSystemPages = false },
            onLogout = {
                if (isGuest) viewModel.clearGuestSessions()
                viewModel.themeManager.clearSessionFlags()
                showSystemPages = false
                onLogout()
            }
        )
    }

    if (showSearchPage) {
        SearchSessionsScreen(
            sessions = sessions,
            onDismiss = { showSearchPage = false },
            onSessionClick = {
                viewModel.loadChat(it)
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
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = Color.Transparent
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(GeminiBg)) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // ===== رأس القائمة الجانبية: الملف الشخصي أو زر تسجيل الدخول =====
                            if (isGuest) {
                                Surface(
                                    onClick = {
                                        viewModel.clearGuestSessions()
                                        viewModel.themeManager.clearSessionFlags()
                                        scope.launch { drawerState.close() }
                                        onLogout()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x1A4CAF50),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x554CAF50)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Login, null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("تسجيل الدخول", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                val userName = viewModel.themeManager.getUserName()
                                val avatarUri = viewModel.themeManager.getUserAvatarUri()
                                Surface(
                                    onClick = {
                                        showProfile = true
                                        scope.launch { drawerState.close() }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x14FFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0x22FFFFFF)),
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
                                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            userName.ifBlank { "ملفي الشخصي" },
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White.copy(0.6f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        viewModel.createNewSession("محادثة جديدة")
                                        scope.launch { drawerState.close() }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0x1AFFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("محادثة جديدة", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                                Surface(
                                    onClick = { showSearchPage = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0x1AFFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(sessions, key = { it.id }) { session ->
                                    SessionRow(
                                        title = session.title,
                                        onClick = {
                                            viewModel.loadChat(session.id)
                                            scope.launch { drawerState.close() }
                                        },
                                        onRename = { sessionToRename = session },
                                        onDelete = { sessionToDelete = session }
                                    )
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
                                MessageItem(
                                    message = message,
                                    streamingText = if (message == messages.last() && isTyping) streamingText else null,
                                    isSpeaking = speakingMessageId == message.id,
                                    isPaused = isPaused && speakingMessageId == message.id,
                                    onSpeak = { viewModel.speak(message.id, message.content) },
                                    onPauseSpeak = { viewModel.pauseSpeaking() },
                                    onResumeSpeak = { viewModel.resumeSpeaking() },
                                    onStopSpeak = { viewModel.stopSpeaking() },
                                    onShare = { viewModel.shareChat("توت") },
                                    getSources = { viewModel.getSourcesForMessage(message.id) }
                                )
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
                            if (inputText.isNotBlank() || capturedImages.isNotEmpty()) {
                                viewModel.sendMessage(inputText, capturedImages)
                                inputText = ""
                                capturedImages = emptyList()
                            }
                        },
                        onMicClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                            }
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        },
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

@Composable
private fun SessionRow(
    title: String,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f).padding(vertical = 8.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, maxLines = 1)
        }
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color(0xFF2A2A2A))) {
                DropdownMenuItem(
                    text = { Text("تعديل الاسم", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = Color.White) },
                    onClick = { showMenu = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text("حذف المحادثة", color = Color(0xFFFF5252)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252)) },
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
    onSpeak: () -> Unit = {},
    onPauseSpeak: () -> Unit = {},
    onResumeSpeak: () -> Unit = {},
    onStopSpeak: () -> Unit = {},
    onShare: () -> Unit = {},
    getSources: () -> kotlinx.coroutines.flow.Flow<List<SourceRef>> = { kotlinx.coroutines.flow.flowOf(emptyList()) }
) {
    val isUser = message.role == "user"
    val content = streamingText ?: message.content
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

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
                        color = Color(0x1FFFFFFF),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.widthIn(max = 300.dp).combinedClickable(
                            onLongClick = {
                                clipboard.setText(AnnotatedString(content))
                                Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                            },
                            onClick = {}
                        )
                    ) {
                        SelectionContainer {
                            Text(content, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                        }
                    }
                }
            }
        } else {
            SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                MarkdownMessage(text = content, textColor = Color.White, modifier = Modifier.fillMaxWidth())
            }

            if (message.hasSources) {
                SourcesBox(getSources = getSources)
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
 * مربع المصادر: غير ثابت — لا يظهر إلا إذا احتوى رد الذكاء الاصطناعي على روابط مرجعية
 * تم استخراجها تلقائياً (انظر AiRepository.extractSources).
 */
@Composable
private fun SourcesBox(getSources: () -> kotlinx.coroutines.flow.Flow<List<SourceRef>>) {
    val sources by remember(getSources) { getSources() }.collectAsState(initial = emptyList())
    if (sources.isEmpty()) return

    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Link, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("المصادر", color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        sources.forEach { source ->
            Text(
                "• ${source.title}",
                color = Color(0xFF6EA8FF),
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { try { uriHandler.openUri(source.url) } catch (e: Exception) { } }
                    .padding(vertical = 3.dp)
            )
        }
    }
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
                    placeholder = { Text("رسالة توت...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White
                    )
                )

                if (text.isNotBlank() || images.isNotEmpty()) {
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
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, BorderColor, CircleShape)
                            .clickable { onMicClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, null, tint = if (isListening) Color.Red else Color.LightGray, modifier = Modifier.size(18.dp))
                    }
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
