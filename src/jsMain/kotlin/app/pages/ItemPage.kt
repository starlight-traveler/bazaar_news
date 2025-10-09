package app.pages

import androidx.compose.runtime.*
import app.Post
import app.Comment
import app.fetchComments
import app.fetchPost
import kotlinx.coroutines.*
import org.jetbrains.compose.web.dom.*

@Composable
fun ItemPage(id: Long) {
    var post by remember { mutableStateOf<Post?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(id) {
        loading = true
        error = null
        try {
            post = fetchPost(id)
            comments = fetchComments(id)
        } catch (t: Throwable) {
            error = t.message ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    if (loading) {
        P { Text("Loading…") }
        return
    }
    if (error != null) {
        Div({ classes("card") }) { P { Text("Error: ${error}") } }
        return
    }
    val p = post ?: return
    Div({ classes("stack") }) {
        Div({ classes("card") }) {
            H3 { Text(p.title) }
            if (!p.url.isNullOrBlank()) {
                P { A(p.url!!) { Text(p.url!!) } }
            }
            P({ classes("muted", "small") }) { Text("${p.score} points by ${p.by} · ${p.commentCount} comments") }
            if (!p.text.isNullOrBlank()) {
                P { Text(p.text!!) }
            }
        }
        Div({ classes("card") }) {
            H4 { Text("Comments (${comments.size})") }
            if (comments.isEmpty()) P({ classes("muted") }) { Text("No comments yet.") }
            comments.forEach { c ->
                CommentView(c)
            }
        }
    }
}

@Composable
fun CommentView(c: Comment) {
    Div({ attr("style", "margin-left: 0") }) {
        P({ classes("small") }) {
            B { Text(c.by) }
            Text(" — ")
            Span({ classes("muted") }) { Text("#${c.id}") }
        }
        P { Text(c.text) }
    }
}
