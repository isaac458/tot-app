package com.empire.myapplication.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

object WebSearchHelper {
    
    suspend fun performSearch(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            
            // Fetch the HTML from DuckDuckGo
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9,ar;q=0.8")
                .timeout(5000)
                .get()
                
            val results = document.select(".result__body")
            if (results.isEmpty()) {
                return@withContext "لم يتم العثور على نتائج واضحة عبر الإنترنت."
            }
            
            val stringBuilder = java.lang.StringBuilder()
            stringBuilder.append("نتائج البحث من الإنترنت للسؤال '$query':\n\n")
            
            var count = 0
            for (result in results) {
                if (count >= 3) break // We only need top 3 results to keep prompt short
                
                val title = result.select(".result__title").text()
                val snippet = result.select(".result__snippet").text()
                
                if (title.isNotBlank() && snippet.isNotBlank()) {
                    stringBuilder.append("${count + 1}. **$title**\n$snippet\n\n")
                    count++
                }
            }
            
            return@withContext stringBuilder.toString().trim()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "عذراً، حدث خطأ أثناء البحث في الويب: ${e.localizedMessage}"
        }
    }
}
