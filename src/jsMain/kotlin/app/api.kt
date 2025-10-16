package app

import kotlinx.serialization.json.Json

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.forms.*
import kotlin.random.Random



private const val BASE_URL = "http://localhost:8079/api"

// Shared HTTP client instance
val httpClient = HttpClient(Js) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

/* -------------------- POSTS -------------------- */

suspend fun getPosts(sort: String = "new"): List<Post> {
    return httpClient.get("$BASE_URL/posts?sort=$sort").body()
}

suspend fun getPost(id: Int): Post {
    return httpClient.get("$BASE_URL/posts/$id").body()
}

suspend fun createPost(title: String, content: String, authorId: String): String {
    // Generate random coordinates
    val randomX = Random.nextDouble(0.0, 2000.0) // adjust max as needed
    val randomY = Random.nextDouble(0.0, 1500.0)
    return httpClient.submitForm(
        url = "$BASE_URL/posts",
        formParameters = parametersOf(
            "title" to listOf(title),
            "content" to listOf(content),
            "authorId" to listOf(authorId.toString()),
            "x" to listOf(randomX.toString()),
            "y" to listOf(randomY.toString())
        )
    ).bodyAsText()
}

/* -------------------- USERS -------------------- */

suspend fun registerUser(username: String, password: String): String {
    return httpClient.submitForm(
        url = "$BASE_URL/register",
        formParameters = parametersOf(
            "username" to listOf(username),
            "password" to listOf(password)
        )
    ).bodyAsText()
}

suspend fun loginUser(username: String, password: String): String {
    return httpClient.submitForm(
        url = "$BASE_URL/login",
        formParameters = parametersOf(
            "username" to listOf(username),
            "password" to listOf(password)
        )
    ).bodyAsText()
}

/* -------------------- COMMENTS -------------------- */

suspend fun getCommentsForPost(postId: Int): List<Comment> {
    try {
        return httpClient.get("$BASE_URL/posts/$postId/comments").body()
    } catch (e: Exception) {
        // Try to get the error text from the response
        console.error("Error fetching comments:", e)
        throw e
    }
}

suspend fun createComment(postId: Int, username: String, content: String): String {
    return httpClient.submitForm(
        url = "$BASE_URL/posts/$postId/comments",
        formParameters = parametersOf(
            "username" to listOf(username),
            "content" to listOf(content)
        )
    ).bodyAsText()
}


// function to count upvotes
suspend fun getUpvoteCount(postId: Int): Int {
    return try {
        val response: Map<String, Int> = httpClient.get("$BASE_URL/posts/$postId/upvotes").body()
        response["count"] ?: 0
    } catch (e: Exception) {
        console.error("Error fetching upvote count for post $postId: $e")
        0
    }
}