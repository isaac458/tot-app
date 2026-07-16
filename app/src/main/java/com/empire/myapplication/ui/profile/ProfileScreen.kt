package com.empire.myapplication.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.empire.myapplication.core.utils.ThemeManager
import com.empire.myapplication.core.utils.ThemeType
import com.empire.myapplication.ui.theme.*

/**
 * صفحة الملف الشخصي بتصميم موحّد:
 * - رأس متدرّج بلون الثيم الحالي (بدل مستطيل ثابت اللون)
 * - بطاقات زجاجية موحّدة (glassCard) لجميع الحقول بدل الألوان المتضاربة
 * - لون الأفاتار والأزرار يتبع لون ثيم المستخدم الحالي بدل لون أزرق ثابت
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    themeManager: ThemeManager,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(themeManager.getUserName()) }
    var age by remember { mutableStateOf(themeManager.getUserAge()) }
    var gender by remember { mutableStateOf(themeManager.getUserGender()) }
    var avatarUri by remember { mutableStateOf(themeManager.getUserAvatarUri()) }
    val themeTypeState by themeManager.themeType.collectAsState()
    val accentColor = when (themeTypeState) {
        ThemeType.AURA_BLUE -> CosmicBlue
        ThemeType.AURA_PINK -> SoftPink
        ThemeType.AURA_VIOLET -> CosmicViolet
        ThemeType.AURA_EMERALD -> Color(0xFF10B981)
        else -> CosmicBlue
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = copyUriToInternalStorage(context, it, "avatar_${themeManager.getUserId()}.jpg")
            if (localPath != null) {
                themeManager.setUserAvatarUri(localPath)
                avatarUri = localPath
            } else {
                themeManager.setUserAvatarUri(it.toString())
                avatarUri = it.toString()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = GeminiBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ===== الرأس: خلفية متدرجة بلون الثيم بدل مستطيل ثابت =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(accentColor.copy(alpha = 0.35f), GeminiBg)
                            )
                        )
                        .padding(bottom = 28.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                            Text(
                                "الملف الشخصي",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            TextButton(onClick = {
                                themeManager.setUserName(name)
                                themeManager.setUserAge(age)
                                themeManager.setUserGender(gender)
                                onDismiss()
                            }) {
                                Text("حفظ", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(112.dp)
                                    .clip(CircleShape)
                                    .background(GlassWhiteStrong)
                                    .border(3.dp, accentColor, CircleShape)
                                    .clickable { avatarLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUri != null) {
                                    AsyncImage(
                                        model = avatarUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(52.dp), tint = Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(accentColor, CircleShape)
                                        .padding(6.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                }
                            }
                        }

                        if (name.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ===== الحقول: بطاقات زجاجية موحّدة =====
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileInputField("الاسم المستعار", name, accentColor) { name = it }
                    Spacer(modifier = Modifier.height(16.dp))
                    ProfileInputField("العمر", age, accentColor) { age = it.filter { c -> c.isDigit() } }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("الجنس", color = GlassTextSecondary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(16.dp))
                            .padding(6.dp)
                    ) {
                        listOf("أنثى", "ذكر").forEach { g ->
                            val selected = gender == g
                            Surface(
                                onClick = { gender = g },
                                color = if (selected) accentColor else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    g,
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 15.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInputField(label: String, value: String, accentColor: Color, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = GlassTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp), textAlign = TextAlign.Right)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GlassWhite,
                unfocusedContainerColor = GlassWhite,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = accentColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )
    }
}

private fun copyUriToInternalStorage(context: android.content.Context, uri: Uri, fileName: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
