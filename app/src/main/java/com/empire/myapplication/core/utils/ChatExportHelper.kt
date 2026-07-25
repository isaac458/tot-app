package com.empire.myapplication.core.utils

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.empire.myapplication.data.local.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChatExportHelper {

    fun generateMarkdown(messages: List<Message>, chatTitle: String): String {
        val sb = java.lang.StringBuilder()
        sb.append("# $chatTitle\n\n")
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        sb.append("_تم التصدير بتاريخ: ${dateString}_\n\n")
        sb.append("---\n\n")

        for (msg in messages) {
            val roleName = if (msg.role == "user") "**المستخدم:**" else "**توت (الذكاء الاصطناعي):**"
            sb.append("$roleName\n")
            sb.append("${msg.content}\n\n")
            if (msg.imageUri != null) {
                sb.append("_[صورة مرفقة]_\n\n")
            }
            sb.append("---\n\n")
        }

        return sb.toString()
    }

    fun printChatAsPdf(context: Context, messages: List<Message>, chatTitle: String) {
        val htmlContent = generateHtml(messages, chatTitle)
        
        // We must create WebView on Main Thread. In Jetpack Compose, clicking a button is already on Main Thread.
        val webView = WebView(context)
        
        // Settings to make it look good
        webView.settings.javaScriptEnabled = false
        webView.settings.defaultTextEncodingName = "UTF-8"
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                printManager?.let { pm ->
                    val jobName = "Chat_${chatTitle.replace(" ", "_")}"
                    val printAdapter = view.createPrintDocumentAdapter(jobName)
                    pm.print(jobName, printAdapter, PrintAttributes.Builder().build())
                }
            }
        }
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun generateHtml(messages: List<Message>, chatTitle: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date())

        val sb = java.lang.StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        border-bottom: 2px solid #10B981;
                        padding-bottom: 10px;
                    }
                    .header h1 {
                        margin: 0;
                        color: #2c3e50;
                    }
                    .header p {
                        color: #7f8c8d;
                        font-size: 0.9em;
                    }
                    .message {
                        margin-bottom: 20px;
                        padding: 15px;
                        border-radius: 12px;
                        max-width: 85%;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
                        word-wrap: break-word;
                    }
                    .user {
                        background-color: #E3F2FD;
                        margin-right: auto;
                        border-right: 4px solid #2196F3;
                    }
                    .ai {
                        background-color: #FFFFFF;
                        margin-left: auto;
                        border-right: 4px solid #10B981;
                    }
                    .role-name {
                        font-weight: bold;
                        font-size: 0.85em;
                        margin-bottom: 5px;
                        color: #555;
                    }
                    .image-note {
                        display: inline-block;
                        margin-top: 10px;
                        padding: 5px 10px;
                        background-color: #eee;
                        border-radius: 4px;
                        font-size: 0.8em;
                        color: #666;
                    }
                    pre {
                        background-color: #2c3e50;
                        color: #f8f8f2;
                        padding: 10px;
                        border-radius: 6px;
                        overflow-x: auto;
                        direction: ltr;
                        text-align: left;
                    }
                    code {
                        font-family: Consolas, Monaco, monospace;
                    }
                    /* Simple markdown bold/italic parsing for HTML */
                    p { margin-top: 0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>$chatTitle</h1>
                    <p>تاريخ التصدير: $dateString</p>
                </div>
                <div class="chat-container">
        """.trimIndent())

        for (msg in messages) {
            val isUser = msg.role == "user"
            val cssClass = if (isUser) "user" else "ai"
            val roleName = if (isUser) "أنت" else "توت (الذكاء الاصطناعي)"
            
            // Basic formatting for Markdown to HTML (just replacing newlines with <br> for simple view)
            var htmlFormattedText = msg.content
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>")
                
            sb.append("""
                    <div class="message $cssClass">
                        <div class="role-name">$roleName</div>
                        <div>$htmlFormattedText</div>
            """.trimIndent())
            
            if (msg.imageUri != null) {
                sb.append("<div class='image-note'>🖼️ تم إرفاق صورة في هذه الرسالة</div>")
            }
            
            sb.append("</div>")
        }

        sb.append("""
                </div>
            </body>
            </html>
        """.trimIndent())

        return sb.toString()
    }
}
