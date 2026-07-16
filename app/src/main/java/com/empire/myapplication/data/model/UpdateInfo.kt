package com.empire.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body") val body: String,
    @SerializedName("assets") val assets: List<GitHubAsset>
)

data class GitHubAsset(
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("size") val size: Long
)

data class UpdateInfo(
    val latestVersion: String,
    val latestVersionCode: Int,
    val minimumVersionCode: Int,
    val forceUpdate: Boolean,
    val message: String,
    val changelog: String,
    val apkUrl: String,
    val apkSize: String
)
