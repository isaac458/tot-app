package com.empire.myapplication.ui.chat

import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

@Composable
fun MarkdownMessage(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { parseBlocks(text) }

    Column(modifier = modifier) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Code -> CodeBlockView(language = block.language, code = block.code)
                is MdBlock.Callout -> CalloutView(type = block.type, content = block.content)
                is MdBlock.Text -> MarkwonTextView(text = block.content, textColor = textColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private sealed class MdBlock {
    data class Text(val content: String) : MdBlock()
    data class Code(val language: String, val code: String) : MdBlock()
    data class Callout(val type: String, val content: String) : MdBlock()
}

private fun parseBlocks(raw: String): List<MdBlock> {
    val result = mutableListOf<MdBlock>()
    // Regex matches code blocks, including those that might not have a closing tag if it's the end of string
    val codeRegex = Regex("```([a-zA-Z0-9+#.]*)\\n([\\s\\S]*?)(?:```|$)")
    var lastIndex = 0

    for (match in codeRegex.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val before = raw.substring(lastIndex, match.range.first)
            result.addAll(splitCallouts(before))
        }
        val lang = match.groupValues[1].ifBlank { "text" }
        val code = match.groupValues[2].trimEnd('\n')
        result.add(MdBlock.Code(language = lang, code = code))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < raw.length) {
        result.addAll(splitCallouts(raw.substring(lastIndex)))
    }
    return result.ifEmpty { listOf(MdBlock.Text(raw)) }
}

private fun splitCallouts(raw: String): List<MdBlock> {
    // Better callout regex: matches > [!TYPE] ...
    val calloutRegex = Regex("(?m)^> \\[!([A-Z]+)\\](?:[ \t]*(.*))?((?:\n^>.*)*)")
    val result = mutableListOf<MdBlock>()
    var lastIndex = 0
    for (match in calloutRegex.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val before = raw.substring(lastIndex, match.range.first)
            if (before.isNotBlank()) result.add(MdBlock.Text(before))
        }
        val type = match.groupValues[1]
        val headerText = match.groupValues[2].orEmpty().trim()
        val bodyLines = match.groupValues[3].lines()
            .map { it.removePrefix(">").trim() }
            .filter { it.isNotEmpty() || it.isNotBlank() }
            .joinToString("\n")
        
        val fullContent = if (headerText.isNotEmpty()) {
            if (bodyLines.isNotEmpty()) "$headerText\n$bodyLines" else headerText
        } else bodyLines
        
        result.add(MdBlock.Callout(type = type, content = fullContent))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < raw.length) {
        val rest = raw.substring(lastIndex)
        if (rest.isNotBlank()) result.add(MdBlock.Text(rest))
    }
    return result
}

@Composable
private fun MarkwonTextView(text: String, textColor: Color) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .build()
    }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
                movementMethod = LinkMovementMethod.getInstance()
                setLinkTextColor(android.graphics.Color.parseColor("#6EA8FF"))
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, text.trim())
        }
    )
}

@Composable
private fun CalloutView(type: String, content: String) {
    val (icon, color, label) = when (type.uppercase()) {
        "WARNING", "ALERT" -> Triple(Icons.Default.WarningAmber, Color(0xFFF5A623), "تنبيه")
        "ERROR", "DANGER" -> Triple(Icons.Default.WarningAmber, Color(0xFFFF5252), "خطأ")
        "SUCCESS", "DONE", "CHECK" -> Triple(Icons.Default.CheckCircle, Color(0xFF4CAF50), "نجاح")
        "TIP", "HINT", "IMPORTANT" -> Triple(Icons.Default.Info, Color(0xFF29B6F6), "نصيحة")
        "INFO", "NOTE" -> Triple(Icons.Default.Info, Color(0xFF9E9E9E), "ملاحظة")
        else -> Triple(Icons.Default.Info, Color(0xFF9E9E9E), type.lowercase().replaceFirstChar { it.uppercase() })
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(content, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
        }
    }
}

private val CODE_KEYWORDS = setOf(
    "fun", "val", "var", "if", "else", "for", "while", "return", "class", "object", "interface",
    "import", "package", "when", "null", "true", "false", "private", "public", "override",
    "def", "print", "function", "const", "let", "new", "try", "catch", "finally", "throw",
    "async", "await", "from", "as", "in", "is", "this", "super", "void", "int", "String",
    "boolean", "static", "final", "break", "continue", "switch", "case", "default", "extends",
    "implements", "constructor", "export", "typeof"
)

private fun highlightCode(code: String): AnnotatedString {
    return buildAnnotatedString {
        val tokenRegex = Regex(
            "(\"(?:[^\"\\\\]|\\\\.)*\")|" +          // 1: Strings
            "('(?:[^'\\\\]|\\\\.)*')|" +            // 2: Char literals
            "(//.*)|" +                             // 3: Line comments
            "(#.*)|" +                              // 4: Hash comments
            "(/\\*[\\s\\S]*?\\*/)|" +               // 5: Block comments
            "(\\b\\d+(\\.\\d+)?\\b)|" +             // 6: Numbers
            "(\\b[A-Za-z_][A-Za-z0-9_]*\\b(?=\\s*\\())|" + // 7: Functions
            "(\\b[A-Z][A-Za-z0-9_]*\\b)|" +         // 8: Types
            "(\\b[A-Za-z_][A-Za-z0-9_]*\\b)"        // 9: Keywords/Identifiers
        )
        
        var last = 0
        for (m in tokenRegex.findAll(code)) {
            if (m.range.first > last) append(code.substring(last, m.range.first))
            
            val token = m.value
            when {
                m.groups[1] != null || m.groups[2] != null -> 
                    withStyle(SpanStyle(color = Color(0xFFCE9178))) { append(token) }
                m.groups[3] != null || m.groups[4] != null || m.groups[5] != null -> 
                    withStyle(SpanStyle(color = Color(0xFF6A9955))) { append(token) }
                m.groups[6] != null -> 
                    withStyle(SpanStyle(color = Color(0xFFB5CEA8))) { append(token) }
                m.groups[7] != null -> 
                    withStyle(SpanStyle(color = Color(0xFFDCDCAA))) { append(token) }
                m.groups[8] != null -> 
                    withStyle(SpanStyle(color = Color(0xFF4EC9B0))) { append(token) }
                token in CODE_KEYWORDS -> 
                    withStyle(SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.Bold)) { append(token) }
                else -> append(token)
            }
            last = m.range.last + 1
        }
        if (last < code.length) append(code.substring(last))
    }
}

@Composable
private fun CodeBlockView(language: String, code: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val lines = code.lines()
    val isLong = lines.size > 14
    var expanded by remember { mutableStateOf(!isLong) }
    var copied by remember { mutableStateOf(false) }

    val displayedCode = if (expanded || !isLong) code else lines.take(14).joinToString("\n")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161616))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF232323))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(language, color = Color(0xFFAAAAAA), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                    Toast.makeText(context, "تم نسخ الكود", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (copied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                    contentDescription = "نسخ",
                    tint = if (copied) Color(0xFF4CAF50) else Color(0xFFAAAAAA),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Box(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) {
            Text(
                text = highlightCode(displayedCode),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFFD4D4D4)
            )
        }

        if (isLong) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (expanded) "إخفاء" else "إظهار المزيد (${lines.size - 14}+ سطر)",
                    color = Color(0xFF6EA8FF),
                    fontSize = 12.sp
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF6EA8FF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
