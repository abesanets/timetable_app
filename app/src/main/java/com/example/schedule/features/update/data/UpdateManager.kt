package com.example.schedule.features.update.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val GITHUB_LATEST_RELEASE_URL =
            "https://api.github.com/repos/abesanets/timetable_app/releases/latest"

        /**
         * Сравнивает две версии в формате semver (например, "4.5" и "v4.6" или "4.5.1").
         * Возвращает true, если версия [latest] новее, чем [current].
         */
        fun isNewerVersion(current: String, latest: String): Boolean {
            val cleanCurrent = current.trimStart('v', 'V').trim()
            val cleanLatest = latest.trimStart('v', 'V').trim()

            val currentParts = cleanCurrent.split(".").map { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }
            val latestParts = cleanLatest.split(".").map { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }

            val maxLen = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLen) {
                val curr = currentParts.getOrElse(i) { 0 }
                val lat = latestParts.getOrElse(i) { 0 }
                if (lat > curr) return true
                if (lat < curr) return false
            }
            return false
        }
    }

    /**
     * Проверяет наличие нового релиза на GitHub.
     */
    suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "TimetableApp-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    return@withContext Result.failure(
                        Exception("GitHub API вернул статус $code")
                    )
                }

                val bodyString = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Пустой ответ от сервера"))

                val release = gson.fromJson(bodyString, GitHubRelease::class.java)
                val releaseVersion = release.tagName.trimStart('v', 'V').trim()

                if (isNewerVersion(currentVersionName, release.tagName)) {
                    val apkAsset = release.assets?.firstOrNull {
                        it.name.endsWith(".apk", ignoreCase = true)
                    }

                    if (apkAsset != null) {
                        val updateInfo = AppUpdateInfo(
                            versionName = releaseVersion,
                            releaseTitle = release.name ?: "Версия $releaseVersion",
                            changelog = release.body.orEmpty(),
                            apkDownloadUrl = apkAsset.downloadUrl,
                            apkFileName = apkAsset.name,
                            apkSize = apkAsset.size,
                            publishedAt = release.publishedAt
                        )
                        return@withContext Result.success(updateInfo)
                    }
                }

                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Скачивает APK-файл обновления во внутренний кэш и сообщает прогресс в [onProgress].
     */
    suspend fun downloadApk(
        updateInfo: AppUpdateInfo,
        onProgress: (DownloadProgress) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }

        val apkFile = File(updatesDir, updateInfo.apkFileName)
        if (apkFile.exists()) {
            apkFile.delete()
        }

        try {
            onProgress(DownloadProgress.Downloading(0f, 0L, updateInfo.apkSize))

            val request = Request.Builder()
                .url(updateInfo.apkDownloadUrl)
                .addHeader("User-Agent", "TimetableApp-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    onProgress(DownloadProgress.Error("Ошибка загрузки: HTTP ${response.code}"))
                    return@withContext null
                }

                val body = response.body
                    ?: run {
                        onProgress(DownloadProgress.Error("Пустое тело ответа"))
                        return@withContext null
                    }

                val totalLength = if (body.contentLength() > 0) body.contentLength() else updateInfo.apkSize

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            val progress = if (totalLength > 0) {
                                (totalBytesRead.toFloat() / totalLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }

                            onProgress(
                                DownloadProgress.Downloading(
                                    progress = progress,
                                    downloadedBytes = totalBytesRead,
                                    totalBytes = totalLength
                                )
                            )
                        }
                        output.flush()
                    }
                }
            }

            onProgress(DownloadProgress.Completed(apkFile))
            apkFile
        } catch (e: Exception) {
            if (apkFile.exists()) {
                apkFile.delete()
            }
            onProgress(DownloadProgress.Error(e.localizedMessage ?: "Сбой при скачивании файла"))
            null
        }
    }

    /**
     * Запускает процесс установки скачанного APK.
     * При необходимости запрашивает разрешение на установку из неизвестных источников.
     */
    fun installApk(apkFile: File) {
        if (!apkFile.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }

    /**
     * Проверяет, дано ли разрешение на установку неизвестных приложений (Android 8+).
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }
}
