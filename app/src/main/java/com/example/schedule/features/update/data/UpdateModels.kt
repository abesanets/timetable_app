package com.example.schedule.features.update.data

import com.google.gson.annotations.SerializedName
import java.io.File

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("size") val size: Long,
    @SerializedName("content_type") val contentType: String? = null
)

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("assets") val assets: List<GitHubAsset>?
)

data class AppUpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSize: Long,
    val publishedAt: String?
)

sealed interface DownloadProgress {
    data object Idle : DownloadProgress
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadProgress
    data class Completed(val apkFile: File) : DownloadProgress
    data class Error(val message: String) : DownloadProgress
}
