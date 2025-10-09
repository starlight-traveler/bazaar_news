package app

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Long,
    val title: String,
    val url: String? = null,
    val text: String? = null,
    val by: String,
    val time: Long,
    val score: Int = 0,
    val commentCount: Int = 0
)

@Serializable
data class Comment(
    val id: Long,
    val by: String,
    val time: Long,
    val text: String,
    val parent: Long
)

@Serializable
data class NewPostRequest(
    val title: String,
    val url: String?,
    val text: String?
)

@Serializable
data class ApiError(
    val message: String
)
