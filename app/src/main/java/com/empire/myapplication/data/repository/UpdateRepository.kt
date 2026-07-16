package com.empire.myapplication.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.empire.myapplication.core.AiConstants
import com.empire.myapplication.data.model.GitHubRelease
import com.empire.myapplication.data.model.UpdateInfo
import com.empire.myapplication.data.remote.UpdateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Regex

@Singleton
class UpdateRepository @Inject constructor(
    private val apiService: UpdateApiService,
    private val client: OkHttpClient
) {
    suspend fun checkUpdate(): Result<UpdateInfo> {
        return try {
            // سنقوم بجلب أحدث إصدار من GitHub
            val response = apiService.getGitHubUpdateInfo(AiConstants.GITHUB_API_URL)
            if (response.isSuccessful && response.body() != null) {
                val github = response.body()!!
                
                // تحويل الـ Tag (مثلاً "v1.2.0") إلى رقم الإصدار 120
                // أو الأفضل أن يكون الـ Tag مجرد رقم مثل "2" ليتوافق مع VersionCode
                val latestVersionCode = github.tagName.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                val apkAsset = github.assets.find { it.downloadUrl.endsWith(".apk") }

                val info = UpdateInfo(
                    latestVersion = github.tagName,
                    latestVersionCode = latestVersionCode,
                    minimumVersionCode = if (github.body.contains("[FORCE]")) latestVersionCode else 0,
                    forceUpdate = github.body.contains("[FORCE]"),
                    message = "يتوفر تحديث جديد!",
                    changelog = github.body.replace("[FORCE]", "").trim(),
                    apkUrl = apkAsset?.downloadUrl ?: "",
                    apkSize = if (apkAsset != null) "${apkAsset.size / 1024 / 1024} MB" else "Unknown"
                )
                Result.success(info)
            } else {
                Result.failure(Exception("Failed to fetch from GitHub: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAppVersionCode(context: Context): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    fun downloadApk(url: String, destination: File): Flow<DownloadStatus> = flow {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            emit(DownloadStatus.Error("Failed to download file"))
            return@flow
        }

        val body = response.body ?: throw Exception("Response body is null")
        val totalBytes = body.contentLength()
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(destination)
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int = 0
        var totalRead = 0L
        var lastEmitTime = 0L

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastEmitTime > 100) { 
                    val progress = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt() else 0
                    emit(DownloadStatus.Progress(progress, totalRead, totalBytes))
                    lastEmitTime = currentTime
                }
            }
            emit(DownloadStatus.Finished(destination.absolutePath))
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e.message ?: "Unknown error"))
        } finally {
            outputStream.close()
            inputStream.close()
        }
    }.flowOn(Dispatchers.IO)

    sealed class DownloadStatus {
        data class Progress(val progress: Int, val currentBytes: Long, val totalBytes: Long) : DownloadStatus()
        data class Finished(val path: String) : DownloadStatus()
        data class Error(val message: String) : DownloadStatus()
    }
}
