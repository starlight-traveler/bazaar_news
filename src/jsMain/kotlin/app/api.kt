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



private const val BASE_URL = "http://localhost:8079/api"

// ✅ Shared HTTP client instance
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

suspend fun getPosts(): List<Post> {
    return httpClient.get("$BASE_URL/posts").body()
}

suspend fun getPost(id: Int): Post {
    return httpClient.get("$BASE_URL/posts/$id").body()
}

suspend fun createPost(title: String, content: String, authorId: String): String {
    return httpClient.submitForm(
        url = "$BASE_URL/posts",
        formParameters = parametersOf(
            "title" to listOf(title),
            "content" to listOf(content),
            "authorId" to listOf(authorId.toString())
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

//suspend fun getComments(): List<Comment> {
//    return httpClient.get("$BASE_URL/comments").body()
//}
//
//suspend fun getCommentsForPost(postId: Int): List<Comment> {
//    return httpClient.get("$BASE_URL/comments?postId=$postId").body()
//}
//
//suspend fun createComment(postId: Int, authorId: Int, content: String): String {
//    return httpClient.submitForm(
//        url = "$BASE_URL/comments",
//        formParameters = parametersOf(
//            "postId" to listOf(postId.toString()),
//            "authorId" to listOf(authorId.toString()),
//            "content" to listOf(content)
//        )
//    ).bodyAsText()
//}
