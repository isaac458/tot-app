package com.empire.myapplication.ui.chat

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.empire.myapplication.core.utils.ThemeManager
import com.empire.myapplication.core.utils.TtsManager
import com.empire.myapplication.data.local.Message
import com.empire.myapplication.data.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val repository: AiRepository,
    val themeManager: ThemeManager
) : AndroidViewModel(application) {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _sessions = MutableStateFlow<List<com.empire.myapplication.data.local.ChatSession>>(emptyList())
    val sessions: StateFlow<List<com.empire.myapplication.data.local.ChatSession>> = _sessions.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _botStatus = MutableStateFlow<String?>(null)
    val botStatus: StateFlow<String?> = _botStatus.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val ttsManager = TtsManager(application)
    private var currentChatId: Long = -1L
    private var chatJob: kotlinx.coroutines.Job? = null

    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    init {
        // Load sessions only. Do not create a session immediately (lazy creation)
        viewModelScope.launch {
            val ownerId = themeManager.getUserId()
            repository.getSessionsForOwner(ownerId).collect { sessionList ->
                _sessions.value = sessionList
            }
        }
    }

    fun createNewSession(title: String) {
        viewModelScope.launch {
            val ownerId = themeManager.getUserId()
            val newId = repository.createNewSession(title, ownerId)
            loadChat(newId)
        }
    }

    fun loadChat(chatId: Long) {
        currentChatId = chatId
        viewModelScope.launch {
            repository.getMessages(chatId).collect {
                _messages.value = it
            }
        }
    }

    fun shareChat(title: String) {
        viewModelScope.launch {
            val currentMessages = _messages.value
            if (currentMessages.isEmpty()) return@launch

            val text = buildString {
                appendLine("💬 محادثة: $title")
                appendLine("━".repeat(30))
                appendLine()
                currentMessages.forEach { msg ->
                    val prefix = if (msg.role == "user") "👤 أنت" else "🤖 توت"
                    appendLine("$prefix:")
                    appendLine(msg.content)
                    appendLine()
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            getApplication<Application>().startActivity(
                Intent.createChooser(shareIntent, "مشاركة المحادثة").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun clearChat() {
        if (currentChatId == -1L) return
        viewModelScope.launch {
            repository.deleteMessages(currentChatId)
        }
    }

    fun clearGuestSessions() {
        viewModelScope.launch {
            repository.clearSessionsForOwner("guest")
        }
    }

    fun updateSessionTitle(chatId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(chatId, newTitle)
        }
    }

    /** حذف محادثة كاملة (تُستخدم من قائمة المحادثات وشاشة البحث). */
    fun deleteSession(chatId: Long) {
        viewModelScope.launch {
            repository.deleteMessages(chatId)
            if (currentChatId == chatId) {
                currentChatId = -1L
                _messages.value = emptyList()
            }
        }
    }

    /** مربع المصادر: يجلب المصادر المستخرجة تلقائياً لرسالة معيّنة. */
    fun getSourcesForMessage(messageId: Long) = repository.getSourcesForMessage(messageId)

    fun stopGeneration() {
        chatJob?.cancel()
        _isTyping.value = false
        _botStatus.value = null
        // If we were streaming, just stop it and save it
        if (_streamingText.value.isNotEmpty()) {
            _streamingText.value = ""
        }
    }

    fun sendMessage(content: String, images: List<Bitmap> = emptyList()) {
        chatJob = viewModelScope.launch {
            if (currentChatId == -1L) {
                // Create a session right before sending the first message
                val ownerId = themeManager.getUserId()
                currentChatId = repository.createNewSession("محادثة جديدة", ownerId)
                // Start listening to this new chat's messages
                launch {
                    repository.getMessages(currentChatId).collect {
                        _messages.value = it
                    }
                }
            }

            // Auto-rename if it's the first message
            val currentSession = _sessions.value.find { it.id == currentChatId }
            if (_messages.value.isEmpty() && (currentSession?.title == "محادثة جديدة" || currentSession == null)) {
                val words = content.split("\\s+".toRegex()).take(4).joinToString(" ")
                updateSessionTitle(currentChatId, words.ifEmpty { "محادثة" })
            }

            _isTyping.value = true
            _botStatus.value = "توت تكتب..."
            _streamingText.value = ""

            // We only support sending the first image via the API currently, but we accept a list
            val imageBase64 = images.firstOrNull()?.let {
                val outputStream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val byteArray = outputStream.toByteArray()
                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            }

            try {
                // The repository.sendMessage saves BOTH the user message and the bot's full response to DB immediately.
                // We'll capture the returned text to simulate streaming.
                val fullResponse = repository.sendMessage(currentChatId, content, imageBase64) { attempt ->
                    _botStatus.value = "جاري إعادة المحاولة (${attempt}/3)..."
                }

                // Simulate Streaming
                _botStatus.value = null
                val words = fullResponse.split(" ")
                var currentText = ""
                for (word in words) {
                    currentText += "$word "
                    _streamingText.value = currentText
                    delay(30) // 30ms delay per word
                }
                
                // Clear streaming text once done (the full message is already in DB and collected by Flow)
                _streamingText.value = ""
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelled
            } finally {
                _isTyping.value = false
                _botStatus.value = null
            }
        }
    }

    fun setListening(listening: Boolean) {
        _isListening.value = listening
    }

    fun speak(messageId: Long, text: String) {
        _speakingMessageId.value = messageId
        _isPaused.value = false
        ttsManager.speak(text) { 
            _speakingMessageId.value = null
            _isPaused.value = false
        }
    }

    fun pauseSpeaking() {
        ttsManager.pause()
        _isPaused.value = true
    }

    fun resumeSpeaking() {
        ttsManager.resume {
            _speakingMessageId.value = null
            _isPaused.value = false
        }
        _isPaused.value = false
    }

    fun stopSpeaking() {
        ttsManager.stop()
        _speakingMessageId.value = null
        _isPaused.value = false
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
