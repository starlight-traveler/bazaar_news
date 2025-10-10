package app

import kotlinx.serialization.Serializable
import kotlinx.datetime.*
import kotlinx.datetime.toLocalDateTime

@Serializable
data class User(
    val id: Int,
    val username: String
)

@Serializable
data class Post(
    val id: Int,
    val title: String,
    val content: String,
    val authorId: Int,
    val createdAt: String
) {
    // Converts backend string timestamp -> Kotlinx datetime
    fun createdAtLocal(): LocalDateTime? {
        return try {
            Instant.parse(createdAt).toLocalDateTime(TimeZone.currentSystemDefault())
        } catch (e: Exception) {
            null // fallback if parsing fails (e.g. backend sends local datetime string)
        }
    }

    // Returns a nicely formatted date string for UI
    fun formattedDate(): String {
        return createdAtLocal()?.let {
            "${it.date} ${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
        } ?: createdAt
    }
}

@Serializable
data class Comment(
    val id: Int,
    val postId: Int,
    val authorId: Int,
    val content: String,
    val createdAt: String
) {
    fun createdAtLocal(): LocalDateTime? {
        return try {
            Instant.parse(createdAt).toLocalDateTime(TimeZone.currentSystemDefault())
        } catch (e: Exception) {
            null
        }
    }

    fun formattedDate(): String {
        return createdAtLocal()?.let {
            "${it.date} ${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
        } ?: createdAt
    }
}

/**
 * Helper extension to find the author's username for a given Post.
 */
fun Post.authorName(users: List<User>): String {
    return users.find { it.id == authorId }?.username ?: "Unknown"
}

/**
 * Helper extension to find comments belonging to this post.
 */
fun Post.comments(allComments: List<Comment>): List<Comment> {
    return allComments.filter { it.postId == id }
}


