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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // قسم الحسابات المحفوظة
        Text(
            "الحسابات المحفوظة على هذا الجهاز",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = GlassText,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        
        if (savedAccountsList.isEmpty()) {
            Text(
                "لا توجد حسابات أخرى محفوظة حالياً",
                fontSize = 13.sp,
                color = GlassTextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            savedAccountsList.forEach { account ->
                val isCurrent = account.uid == currentUid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) GlowBlue else GlassBorderSubtle,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .background(if (isCurrent) Color(0x1F4285F4) else GlassDark)
                        .clickable {
                            if (!isCurrent) {
                                themeManager.switchAccount(account.uid)
                                onSwitchAccount(account.uid)
                            }
                        }
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
                        if (account.avatarUri != null) {
                            coil.compose.AsyncImage(
                                model = account.avatarUri,
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
                        Text(account.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text(account.email, fontSize = 11.sp, color = GlassTextSecondary)
                    }
                    
                    if (isCurrent) {
                        Text("الحالي", color = GlowBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
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
            }
        }
        
        if (!isGuest) {
            Button(
                onClick = {
                    // Sign out from Firebase but keep saved accounts
                    FirebaseAuth.getInstance().signOut()
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GlassWhiteStrong),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("إضافة حساب آخر للتبديل", color = Color.White, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(4.dp))

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
