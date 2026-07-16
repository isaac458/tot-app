package com.empire.myapplication.data.remote

import com.empire.myapplication.data.model.GitHubRelease
import com.empire.myapplication.data.model.UpdateInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface UpdateApiService {
    @GET
    suspend fun getUpdateInfo(@Url url: String): Response<UpdateInfo>

    @GET
    suspend fun getGitHubUpdateInfo(@Url url: String): Response<GitHubRelease>
}
