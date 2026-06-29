package com.example.findit.api

import com.example.findit.model.AuthResponse
import com.example.findit.model.ItemsResponse
import com.example.findit.model.LoginRequest
import com.example.findit.model.MatchesResponse
import com.example.findit.model.PostItemResponse
import com.example.findit.model.RegisterRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Map<String, Any>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @Multipart
    @POST("items")
    suspend fun postItem(
        @Header("Authorization") token: String,
        @Part("type") type: RequestBody,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("location") location: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<PostItemResponse>

    @GET("items")
    suspend fun getItems(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null
    ): Response<ItemsResponse>

    @GET("items/{id}/matches")
    suspend fun getMatches(
        @Header("Authorization") token: String,
        @Path("id") itemId: Int
    ): Response<MatchesResponse>
}

