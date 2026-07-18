package com.empire.myapplication.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empire.myapplication.ui.theme.*
import com.empire.myapplication.core.utils.ThemeManager
import com.empire.myapplication.core.utils.SavedAccount
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemPagesDialog(
    themeManager: ThemeManager,
    isGuest: Boolean = false,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onSwitchAccount: (String) -> Unit
) {
    var currentPage by remember { mutableStateOf("menu") }
    val context = LocalContext.current
    val webClientId = "764601305581-sh36efhgg918hagaqbi113h1q78p0l9r.apps.googleusercontent.com"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GeminiBg)
                .clickable(enabled = false) {},
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPage != "menu") {
                        IconButton(onClick = { currentPage = "menu" }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GlassText)
                        }
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GlassText)
                        }
                    }
                    
                    Text(
                        text = getPageTitle(currentPage),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassText,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)

                // Content Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Transparent)
                ) {
                    when (currentPage) {
                        "menu" -> MenuContent(
                            themeManager = themeManager,
                            isGuest = isGuest,
                            onNavigate = { currentPage = it },
                            onLogout = onLogout,
                            onSwitchAccount = onSwitchAccount,
                            context = context,
                            webClientId = webClientId
                        )
                        "terms" -> TermsContent()
                        "privacy" -> PrivacyContent()
                        "devices" -> DevicesContent()
                        "last_login" -> LastLoginContent()
                        "link_instagram" -> LinkInstagramContent(themeManager = themeManager)
                    }
                }
            }
        }
    }
}

private fun getPageTitle(page: String): String {
    return when (page) {
        "menu" -> "صفحات النظام"
        "terms" -> "شروط الاستخدام"
        "privacy" -> "سياسة الخصوصية"
        "devices" -> "الأجهزة المتصلة"
        "last_login" -> "آخر تسجيل دخول"
        "link_instagram" -> "ربط انستغرام"
        else -> ""
    }
}

@Composable
private fun MenuContent(
    themeManager: ThemeManager,
    isGuest: Boolean,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onSwitchAccount: (String) -> Unit,
    context: android.content.Context,
    webClientId: String
) {
    var savedAccountsList by remember { mutableStateOf(themeManager.getSavedAccounts()) }
    val currentUid = remember { themeManager.getUserId() }
    var isAccountsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // قسم الحسابات المحفوظة
        Text(
            "الحسابات",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = GlassText,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        
        if (savedAccountsList.isEmpty() || isGuest) {
            Text(
                "لا توجد حسابات أخرى محفوظة حالياً",
                fontSize = 13.sp,
                color = GlassTextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassDark)
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                val currentAccount = savedAccountsList.find { it.uid == currentUid }
                
                // الحساب الحالي (يظهر دائماً)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAccountsExpanded = !isAccountsExpanded }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GlassWhiteStrong),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentAccount?.avatarUri != null) {
                            coil.compose.AsyncImage(
                                model = currentAccount.avatarUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentAccount?.name ?: "حسابي", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text(currentAccount?.email ?: "", fontSize = 11.sp, color = GlassTextSecondary)
                    }
                    Text("الحالي", color = GlowBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(if (isAccountsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = GlassTextSecondary)
                }

                // الحسابات البديلة وزر الإضافة (تظهر عند التوسيع)
                AnimatedVisibility(visible = isAccountsExpanded) {
                    Column {
                        HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                        val otherAccounts = savedAccountsList.filter { it.uid != currentUid }
                        
                        otherAccounts.forEach { account ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        themeManager.switchAccount(account.uid)
                                        onSwitchAccount(account.uid)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(GlassWhiteStrong),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (account.avatarUri != null) {
                                        coil.compose.AsyncImage(
                                            model = account.avatarUri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(account.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.White)
                                }
                                IconButton(
                                    onClick = {
                                        themeManager.removeAccount(account.uid)
                                        savedAccountsList = themeManager.getSavedAccounts()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الحساب", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // زر الإضافة مخفي إذا وصل عدد الحسابات لـ 3
                        if (savedAccountsList.size < 3) {
                            HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        FirebaseAuth.getInstance().signOut()
                                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(webClientId)
                                            .requestEmail()
                                            .build()
                                        GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                                            onLogout()
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إضافة حساب آخر للتبديل", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(4.dp))
        
        MenuItem(
            icon = Icons.Default.Link,
            title = "ربط حساب انستغرام",
            subtitle = "أوامر البوت والتحكم",
            onClick = { onNavigate("link_instagram") },
            bgColor = Color(0xFFE1306C)
        )

        MenuItem(
            icon = Icons.Default.Description,
            title = "شروط الاستخدام",
            onClick = { onNavigate("terms") },
            bgColor = GlowPurple
        )
        MenuItem(
            icon = Icons.Default.PrivacyTip,
            title = "سياسة الخصوصية",
            onClick = { onNavigate("privacy") },
            bgColor = GlowBlue
        )
        MenuItem(
            icon = Icons.Default.Devices,
            title = "الأجهزة المتصلة",
            subtitle = "أجهزة تستخدم حسابك حالياً",
            onClick = { onNavigate("devices") },
            bgColor = GlowPink
        )
        MenuItem(
            icon = Icons.Default.History,
            title = "آخر تسجيل دخول",
            onClick = { onNavigate("last_login") },
            bgColor = GlowViolet
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isGuest) {
            // في وضع الضيف يظهر زر "تسجيل الدخول" بدل "تسجيل الخروج"
            Button(
                onClick = { onLogout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(backgroundColor = Color(0x334CAF50), borderColor = Color(0x804CAF50), blurRadius = 20f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الدخول", fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            Button(
                onClick = {
                    // تسجيل الخروج التام من الحساب الحالي ومسحه من القائمة المحفوظة
                    val currentUidNow = themeManager.getUserId()
                    themeManager.removeAccount(currentUidNow)
                    
                    // Sign out from Firebase
                    FirebaseAuth.getInstance().signOut()
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                        onLogout()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(backgroundColor = Color(0x33FF5252), borderColor = Color(0x80FF5252), blurRadius = 20f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج وإزالة الحساب", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    bgColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = bgColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = Color.White)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = GlassTextSecondary)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GlassTextSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TermsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("شروط الاستخدام لـ توت (Toot AI)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GlassText)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "مرحباً بك في تطبيق توت للذكاء الاصطناعي. باستخدامك لهذا التطبيق، فإنك توافق على الشروط التالية:\n\n" +
            "1. الاستخدام المقبول: يجب استخدام التطبيق لأغراض قانونية فقط.\n\n" +
            "2. المحتوى: نحن لا نتحمل مسؤولية المحتوى الذي يتم إنشاؤه بواسطة الذكاء الاصطناعي، فهو للعلم والمساعدة فقط.\n\n" +
            "3. الخصوصية: نحترم خصوصيتك، يرجى مراجعة سياسة الخصوصية لمعرفة كيف نعالج بياناتك.\n\n" +
            "4. التعديلات: نحتفظ بالحق في تعديل هذه الشروط في أي وقت.\n\n" +
            "شكراً لاستخدامك توت!",
            lineHeight = 24.sp,
            color = GlassTextSecondary
        )
    }
}

@Composable
private fun PrivacyContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("سياسة الخصوصية لـ توت (Toot AI)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GlassText)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "نحن نأخذ خصوصيتك بجدية تامة. إليك كيف نتعامل مع بياناتك:\n\n" +
            "• البيانات المجمعة: نجمع فقط البيانات الأساسية اللازمة لعمل التطبيق (مثل الاسم والعمر لتحسين الردود).\n\n" +
            "• الرسائل: يتم معالجة الرسائل بواسطة محرك الذكاء الاصطناعي الخاص بنا.\n\n" +
            "• المشاركة: نحن لا نبيع بياناتك لأي أطراف ثالثة.\n\n" +
            "• الحماية: نستخدم تقنيات التشفير لحماية بياناتك من الوصول غير المصرح به.",
            lineHeight = 24.sp,
            color = GlassTextSecondary
        )
    }
}

@Composable
private fun DevicesContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("أجهزتك المتصلة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GlassText, modifier = Modifier.padding(bottom = 16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(backgroundColor = GlassDark, borderColor = GlassBorderSubtle, blurRadius = 20f)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Devices, null, tint = GlowBlue)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("هذا الجهاز (Android)", fontWeight = FontWeight.Bold, color = GlassText)
                Text("متصل الآن", fontSize = 12.sp, color = GlassTextSecondary)
            }
        }
    }
}

@Composable
private fun LastLoginContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("سجل الدخول الأخير", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GlassText, modifier = Modifier.padding(bottom = 16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(backgroundColor = GlassDark, borderColor = GlassBorderSubtle, blurRadius = 20f)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.Login, null, tint = GlowPurple)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("اليوم", fontWeight = FontWeight.Bold, color = GlassText)
                Text("الموقع: الجزائر", fontSize = 12.sp, color = GlassTextSecondary)
            }
        }
    }
}

@Composable
fun LinkInstagramContent(
    themeManager: com.empire.myapplication.core.utils.ThemeManager,
    viewModel: com.empire.myapplication.ui.chat.SystemViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isCheckingStatus by viewModel.isCheckingStatus.collectAsState()
    val generatedCode by viewModel.generatedCode.collectAsState()
    val error by viewModel.error.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val unlinkMessage by viewModel.unlinkMessage.collectAsState()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkLinkStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        if (isCheckingStatus) {
            CircularProgressIndicator(color = GlowBlue)
            Spacer(modifier = Modifier.height(16.dp))
            Text("جاري التحقق من حالة الربط...", color = GlassTextSecondary)
        } else if (userData?.linked == true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(backgroundColor = Color(0x3310B981), borderColor = Color(0x8010B981), blurRadius = 20f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✅", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("حسابك مرتبط بنجاح!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                if (userData?.instagramUsername != null) {
                    Text("@${userData!!.instagramUsername}", fontSize = 16.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (userData?.quota != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${userData!!.quota!!.used}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                            Text("مستخدم اليوم", fontSize = 11.sp, color = GlassTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${userData!!.quota!!.daily}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                            Text("الحد اليومي", fontSize = 11.sp, color = GlassTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(userData?.plan?.uppercase() ?: "FREE", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF10B981))
                            Text("الخطة", fontSize = 11.sp, color = GlassTextSecondary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.unlinkAccount() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF5252)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("فك ربط الحساب", color = Color(0xFFFF5252))
            }
            if (unlinkMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(unlinkMessage!!, color = Color(0xFF10B981), fontSize = 13.sp)
            }
        } else if (isLoading) {
            CircularProgressIndicator(color = GlowBlue)
            Spacer(modifier = Modifier.height(16.dp))
            Text("جاري توليد كود الربط...", color = GlassTextSecondary)
        } else if (generatedCode != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(backgroundColor = Color(0x33E1306C), borderColor = Color(0x80E1306C), blurRadius = 20f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("كود الربط الخاص بك:", color = GlassTextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = generatedCode!!, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(generatedCode!!))
                        android.widget.Toast.makeText(context, "تم نسخ الكود!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassWhiteStrong),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("نسخ الكود", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("يرجى الذهاب إلى البوت في انستغرام وإرسال:\n\n!link $generatedCode", textAlign = TextAlign.Center, color = GlassText, lineHeight = 22.sp)
        } else if (error != null) {
            Text("حدث خطأ:", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = GlassTextSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.generateLinkCode() },
                colors = ButtonDefaults.buttonColors(containerColor = GlassDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إعادة المحاولة", color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(backgroundColor = GlassDark, borderColor = GlassBorderSubtle, blurRadius = 20f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔗", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("حسابك غير مرتبط", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("اربط حسابك في انستغرام للتحكم بالبوت واستخدام الأوامر.", textAlign = TextAlign.Center, color = GlassTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.generateLinkCode() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("توليد كود الربط", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
