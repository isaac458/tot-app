package com.empire.myapplication.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownloadClick: (String) -> Unit,
    onDismiss: () -> Unit,
    onInstallStarted: () -> Unit
) {
    val context = LocalContext.current

    when (state) {
        is UpdateState.UpdateAvailable -> {
            Dialog(
                onDismissRequest = { if (!state.isForce) onDismiss() },
                properties = DialogProperties(
                    dismissOnBackPress = !state.isForce,
                    dismissOnClickOutside = !state.isForce
                )
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تحديث جديد متوفر! 🚀",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "الإصدار: ${state.info.latestVersion}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        
                        state.info.apkSize?.let { size ->
                            Text(
                                text = "الحجم: $size",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = state.info.message,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = state.info.changelog,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (!state.isForce) {
                                TextButton(onClick = onDismiss) {
                                    Text("لاحقاً")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Button(
                                onClick = { onDownloadClick(state.info.apkUrl) },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("تحديث الآن")
                            }
                        }
                    }
                }
            }
        }

        is UpdateState.Downloading -> {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("جاري التحميل...", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = state.progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${state.progress}%", fontSize = 12.sp)
                            Text(
                                "${String.format("%.1f", state.currentMB)} MB / ${String.format("%.1f", state.totalMB)} MB",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        is UpdateState.DownloadCompleted -> {
            LaunchedEffect(state.apkPath) {
                installApk(context, File(state.apkPath))
                onInstallStarted()
            }
        }

        is UpdateState.DownloadError -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("خطأ في التحميل") },
                text = { Text(state.message) },
                confirmButton = {
                    Button(onClick = { onDismiss() }) {
                        Text("إعادة المحاولة")
                    }
                }
            )
        }
        
        else -> {}
    }
}

fun installApk(context: Context, apkFile: File) {
    val intent = Intent(Intent.ACTION_VIEW)
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
    } else {
        Uri.fromFile(apkFile)
    }
    intent.setDataAndType(uri, "application/vnd.android.package-archive")
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
