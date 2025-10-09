package app

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Headers

// Change this to your backend origin (no trailing slash).
// Example: "http://localhost:8080"
const val API_BASE_URL: String = "http://localhost:8080"

private val json = Json { ignoreUnknownKeys = true }

suspend fun fetchTopPosts(sort: String = "top"): List<Post> {
    val resp = window.fetch("$API_BASE_URL/api/posts?sort=$sort").await()
    val text = resp.text().await()
    if (!resp.ok) error("Failed to fetch posts: $text")
    return json.decodeFromString(text)
}

suspend fun fetchPost(id: Long): Post {
    val resp = window.fetch("$API_BASE_URL/api/post/$id").await()
    val text = resp.text().await()
    if (!resp.ok) error("Failed to fetch post: $text")
    return json.decodeFromString(text)
}

suspend fun fetchComments(postId: Long): List<Comment> {
    val resp = window.fetch("$API_BASE_URL/api/post/$postId/comments").await()
    val text = resp.text().await()
    if (!resp.ok) error("Failed to fetch comments: $text")
    return json.decodeFromString(text)
}

suspend fun submitPost(req: NewPostRequest): Post {
    val body = json.encodeToString(req)
    val resp = window.fetch("$API_BASE_URL/api/post", RequestInit(
        method = "POST",
        headers = Headers().also {
            it.append("Content-Type", "application/json")
        },
        body = body
    )).await()

    val text = resp.text().await()
    if (!resp.ok) error("Submit failed: $text")
    return json.decodeFromString(text)
}

suspend fun vote(postId: Long, dir: Int): Post {
    val resp = window.fetch("$API_BASE_URL/api/vote?postId=$postId&dir=$dir", RequestInit(
        method = "POST"
    )).await()
    val text = resp.text().await()
    if (!resp.ok) error("Vote failed: $text")
    return json.decodeFromString(text)
}
