package com.example.findit.model

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val college_id: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class User(
    val id: Int,
    val name: String,
    val email: String
)

data class Item(
    val id: Int,
    val user_id: Int,
    val type: String,
    val title: String,
    val description: String?,
    val category: String,
    val image_url: String?,
    val location: String,
    val status: String,
    val created_at: String
)

data class ItemsResponse(
    val count: Int,
    val items: List<Item>
)

data class PostItemResponse(
    val message: String,
    val itemId: Int,
    val matchesFound: Int
)

data class Match(
    val id: Int,
    val similarity_score: Float,
    val status: String,
    val lost_item_id: Int,
    val lost_title: String,
    val lost_image: String?,
    val lost_location: String,
    val found_item_id: Int,
    val found_title: String,
    val found_image: String?,
    val found_location: String
)

data class MatchesResponse(
    val count: Int,
    val matches: List<Match>
)

