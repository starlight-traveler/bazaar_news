package app.pages

import androidx.compose.runtime.*
import app.Post
import app.fetchTopPosts
import app.components.PostRow
import kotlinx.coroutines.*
import org.jetbrains.compose.web.dom.*

@Composable
fun TopPage(sort: String = "top") {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sort) {
        loading = true
        error = null
        try {
            posts = fetchTopPosts(sort)
        } catch (t: Throwable) {
            error = t.message ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    if (loading) {
        P { Text("Loading…") }
    } else if (error != null) {
        Div({ classes("card") }) {
            P { Text("Error: ${error}") }
        }
    } else {
        Div({ classes("list") }) {
            posts.forEachIndexed { idx, p ->
                PostRow(post = p, index = idx + 1)
            }
        }
    }
}
