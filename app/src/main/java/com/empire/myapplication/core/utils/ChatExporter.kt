package com.empire.myapplication.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.empire.myapplication.data.local.Message
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatExporter(private val context: Context) {

    fun exportToText(messages: List<Message>, chatTitle: String) {
        val fileName = "Toot_Chat_${System.currentTimeMillis()}.txt"
        val fileContent = buildString {
            appendLine("--- محادثة توت: $chatTitle ---")
            appendLine("تاريخ التصدير: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
            appendLine("-------------------------------------------")
            appendLine()
            
            messages.forEach { message ->
                val role = if (message.role == "user") "أنت" else "توت"
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                appendLine("[$time] $role:")
                appendLine(message.content)
                if (message.imageUri != null) {
                    appendLine("[تحتوي هذه الرسالة على صورة]")
                }
                appendLine()
            }
        }

        try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { 
                it.write(fileContent.toByteArray())
            }
            shareFile(file, "text/plain")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "تصدير المحادثة عبر..."))
    }
}
