package com.jeet.dockermonitor.data

import com.jeet.dockermonitor.data.model.Container
import com.jeet.dockermonitor.data.model.LiveStat
import com.jeet.dockermonitor.data.model.StatHistory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("containers/")
    suspend fun getContainers(): List<Container>

    @GET("containers/{id}/stats")
    suspend fun getStats(@Path("id")id: String): LiveStat

    @GET("containers/{id}/stats/history")
    suspend fun getHistory(
        @Path("id") id: String,
        @Query("limit") limit: Int = 60
    ): List<StatHistory>

    @GET("containers/{id}/logs")
    suspend fun getLogs(@Path("id")id: String): Map<String,String>

    @POST("containers/{id}/start")
    suspend fun startContainer(@Path("id")id: String): Map<String, String>

    @POST("containers/{id}/stop")
    suspend fun stopContainer(@Path("id")id: String): Map<String, String>

    @POST("containers/{id}/restart")
    suspend fun restartContainer(@Path("id")id: String): Map<String, String>
}

fun createApiService(baseUrl: String): ApiService {
    return retrofit2.Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

