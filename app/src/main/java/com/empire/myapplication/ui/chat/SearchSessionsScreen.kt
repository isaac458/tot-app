package com.empire.myapplication.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empire.myapplication.data.local.ChatSession
import com.empire.myapplication.ui.theme.GeminiBg
import com.empire.myapplication.ui.theme.GlassTextSecondary
import com.empire.myapplication.ui.theme.modernGradientBackground

/**
 * صفحة بحث مستقلة عن المحادثات (على غرار ChatGPT)، بدل حقل بحث داخل القائمة الجانبية
 * الذي كان يسبب ارتفاع مربع الكتابة أثناء البحث. هنا مربع الكتابة الرئيسي غير موجود إطلاقاً
 * في هذه الشاشة، لذلك لا يوجد أي تأثير عليه.
 */
@Composable
fun SearchSessionsScreen(
    sessions: List<ChatSession>,
    onDismiss: () -> Unit,
    onSessionClick: (Long) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val filtered = remember(query, sessions) {
        if (query.isBlank()) sessions else sessions.filter { it.title.contains(query, ignoreCase = true) }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(GeminiBg)) {
            Box(modifier = Modifier.fillMaxSize().background(modernGradientBackground()))

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = GlassTextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text("ابحث في المحادثات...", color = GlassTextSecondary, fontSize = 15.sp)
                                }
                                BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                                )
                            }
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, null, tint = GlassTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (query.isBlank()) "لا توجد محادثات بعد" else "لا توجد نتائج مطابقة",
                            color = GlassTextSecondary
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                        items(filtered, key = { it.id }) { session ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSessionClick(session.id) }
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(session.title, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
