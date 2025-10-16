package app.pages

import androidx.compose.runtime.*
import app.Post
import app.getPosts
import app.components.PostRow
import kotlinx.coroutines.*
import org.jetbrains.compose.web.dom.*
import kotlin.js.Date

@Composable
fun NewPage() {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            // Fetch all posts from backend
            posts = getPosts("new")

            // Sort newest-first (desc) by createdAt.
            // Backend returns LocalDateTime.toString() (no zone), which JS Date can parse.
            // If parsing fails, push that item to the end (treat as very old).
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
            P { Text("Error: $error") }
        }
    } else {
        Div({ classes("list") }) {
            posts.forEachIndexed { idx, p ->
                PostRow(post = p, index = idx + 1)
            }
        }
    }
}

/**
 * Parse a timestamp string into epoch millis for sorting.
 * Accepts strings like "2025-10-15T12:34:56.789" (no timezone).
 * Returns Double.NEGATIVE_INFINITY on failure so bad/empty dates sort last.
 */
private fun createdAtMillis(ts: String): Double {
    val ms = Date(ts).getTime()
    return if (ms.isNaN()) Double.NEGATIVE_INFINITY else ms
}
