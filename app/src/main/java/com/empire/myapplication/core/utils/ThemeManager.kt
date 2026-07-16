package com.empire.myapplication.core.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeType { AURA_BLUE, AURA_PINK, AURA_VIOLET, AURA_EMERALD, CUSTOM_IMAGE }

/**
 * ملاحظة مهمة حول عزل الحسابات:
 * بيانات الملف الشخصي (الاسم/العمر/الجنس/الصورة) أصبحت مخزّنة بمفتاح مختلف لكل مستخدم
 * (مبني على uid الحالي)، بدل مفتاح ثابت واحد يتشارك فيه كل من يستخدم الجهاز.
 * هذا يحل مشكلتين معاً:
 *  1) اختلاط بيانات الملف الشخصي بين الحسابات المختلفة على نفس الجهاز.
 *  2) اختفاء البيانات بعد تسجيل الخروج ثم العودة لنفس الحساب (لأنها كانت تُمسح بالكامل
 *     بدل أن تبقى محفوظة تحت مفتاح خاص بذلك الحساب).
 */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("toot_theme", Context.MODE_PRIVATE)

    private val _themeType = MutableStateFlow(getThemeTypeInternal())
    val themeType: StateFlow<ThemeType> = _themeType.asStateFlow()

    private val _bgImageUri = MutableStateFlow(getBackgroundImageUriInternal())
    val bgImageUri: StateFlow<String?> = _bgImageUri.asStateFlow()

    private val _bgOpacity = MutableStateFlow(getBackgroundOpacityInternal())
    val bgOpacity: StateFlow<Float> = _bgOpacity.asStateFlow()

    private fun getThemeTypeInternal(): ThemeType {
        val name = prefs.getString("theme_type", ThemeType.AURA_BLUE.name) ?: ThemeType.AURA_BLUE.name
        return try { ThemeType.valueOf(name) } catch (e: Exception) { ThemeType.AURA_BLUE }
    }

    fun setThemeType(type: ThemeType) {
        prefs.edit().putString("theme_type", type.name).apply()
        _themeType.value = type
    }

    private fun getBackgroundImageUriInternal(): String? = prefs.getString("bg_image_uri", null)

    fun setBackgroundImageUri(uri: String?) {
        prefs.edit().putString("bg_image_uri", uri).apply()
        _bgImageUri.value = uri
    }

    private fun getBackgroundOpacityInternal(): Float = prefs.getFloat("bg_opacity", 0.3f)

    fun setBackgroundOpacity(opacity: Float) {
        prefs.edit().putFloat("bg_opacity", opacity).apply()
        _bgOpacity.value = opacity
    }

    // ===== هوية المستخدم الحالي =====
    fun getUserId(): String = prefs.getString("user_id", "guest") ?: "guest"
    fun setUserId(uid: String) { prefs.edit().putString("user_id", uid).apply() }

    // ===== بيانات الملف الشخصي (معزولة لكل حساب عبر بادئة uid) =====
    private fun scopedKey(base: String): String = "${base}_${getUserId()}"

    fun getUserName(): String = prefs.getString(scopedKey("user_name"), "") ?: ""
    fun setUserName(name: String) { prefs.edit().putString(scopedKey("user_name"), name).apply() }

    fun getUserAge(): String = prefs.getString(scopedKey("user_age"), "") ?: ""
    fun setUserAge(age: String) { prefs.edit().putString(scopedKey("user_age"), age).apply() }

    fun getUserGender(): String = prefs.getString(scopedKey("user_gender"), "") ?: ""
    fun setUserGender(gender: String) { prefs.edit().putString(scopedKey("user_gender"), gender).apply() }

    fun getUserEmail(): String = prefs.getString(scopedKey("user_email"), "") ?: ""
    fun setUserEmail(email: String) { prefs.edit().putString(scopedKey("user_email"), email).apply() }

    fun getUserAvatarUri(): String? = prefs.getString(scopedKey("user_avatar"), null)
    fun setUserAvatarUri(uri: String?) { prefs.edit().putString(scopedKey("user_avatar"), uri).apply() }

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)
    fun setLoggedIn(loggedIn: Boolean) { prefs.edit().putBoolean("is_logged_in", loggedIn).apply() }

    fun isGuest(): Boolean = prefs.getBoolean("is_guest", false)
    fun setGuest(isGuest: Boolean) { prefs.edit().putBoolean("is_guest", isGuest).apply() }

    fun hasAcceptedTerms(): Boolean = prefs.getBoolean(scopedKey("accepted_terms"), false)
    fun setAcceptedTerms(accepted: Boolean) { prefs.edit().putBoolean(scopedKey("accepted_terms"), accepted).apply() }

    /**
     * يمسح فقط علم تسجيل الدخول/الضيف عند الخروج. بيانات الملف الشخصي للحساب المسجّل
     * تبقى محفوظة تحت مفتاحها الخاص (uid) وتظهر تلقائياً عند العودة لنفس الحساب.
     * بيانات وضع الضيف تُمسح بشكل منفصل ودائماً عبر clearGuestProfileData().
     */
    fun clearSessionFlags() {
        prefs.edit()
            .remove("is_logged_in")
            .remove("is_guest")
            .putString("user_id", "guest")
            .apply()
    }

    /** يمسح بيانات الملف الشخصي الخاصة بوضع الضيف تحديداً (لا تُحفظ أي بيانات ضيف نهائياً). */
    fun clearGuestProfileData() {
        prefs.edit()
            .remove("user_name_guest")
            .remove("user_age_guest")
            .remove("user_gender_guest")
            .remove("user_avatar_guest")
            .remove("accepted_terms_guest")
            .apply()
    }

    @Deprecated("Use clearSessionFlags() + clearGuestProfileData() as appropriate", ReplaceWith("clearSessionFlags()"))
    fun clearUserData() {
        clearSessionFlags()
    }

    // ===== إدارة الحسابات المتعددة على الجهاز =====
    fun getSavedAccounts(): List<SavedAccount> {
        val raw = prefs.getString("saved_accounts", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 3) {
                SavedAccount(
                    uid = parts[0],
                    name = parts[1],
                    email = parts[2],
                    avatarUri = parts.getOrNull(3).takeIf { it?.isNotBlank() == true }
                )
            } else null
        }
    }

    fun saveAccount(uid: String, name: String, email: String, avatarUri: String?) {
        val currentList = getSavedAccounts().toMutableList()
        currentList.removeAll { it.uid == uid }
        currentList.add(SavedAccount(uid, name, email, avatarUri))
        val raw = currentList.joinToString(";") { "${it.uid}|${it.name}|${it.email}|${it.avatarUri.orEmpty()}" }
        prefs.edit().putString("saved_accounts", raw).apply()
    }

    fun removeAccount(uid: String) {
        val currentList = getSavedAccounts().toMutableList()
        currentList.removeAll { it.uid == uid }
        val raw = currentList.joinToString(";") { "${it.uid}|${it.name}|${it.email}|${it.avatarUri.orEmpty()}" }
        prefs.edit().putString("saved_accounts", raw).apply()
    }

    fun switchAccount(uid: String) {
        setUserId(uid)
        setGuest(false)
        setLoggedIn(true)
    }

    // ===== تثبيت المحادثات =====
    fun getPinnedSessions(): Set<Long> {
        val raw = prefs.getString("pinned_sessions", "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun togglePinSession(id: Long) {
        val current = getPinnedSessions().toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        prefs.edit().putString("pinned_sessions", current.joinToString(",")).apply()
    }

    fun isSessionPinned(id: Long): Boolean {
        return getPinnedSessions().contains(id)
    }
}

data class SavedAccount(
    val uid: String,
    val name: String,
    val email: String,
    val avatarUri: String?
)
