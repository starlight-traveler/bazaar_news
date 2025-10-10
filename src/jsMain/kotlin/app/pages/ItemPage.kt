package app.pages

import androidx.compose.runtime.*
import app.Post
import app.Comment
//import app.getComments
import app.getPost
import kotlinx.coroutines.*
import org.jetbrains.compose.web.dom.*

@Composable
fun ItemPage(id: Long) {
    var post by remember { mutableStateOf<Post?>(null) }
    //var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(id) {
        loading = true
        error = null
        try {
            post = getPost(id.toInt())
           // comments = getComments(id.toInt())
        } catch (t: Throwable) {
            error = t.message ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    when {
        loading -> P { Text("Loading…") }
        error != null -> Div({ classes("card") }) { P { Text("Error: $error") } }
        else -> {
            val p = post ?: return
            Div({ classes("stack") }) {

                // 🔹 Post content
                Div({ classes("card") }) {
                    H3 { Text(p.title) }
                    P({ classes("muted", "small") }) {
                        Text("Author ID: ${p.authorId} · Posted on ${p.createdAt}")
                    }
                    P {
                        Text(p.content)
                    }
                }

                // 🔹 Comments section
//                Div({ classes("card") }) {
//                    H4 { Text("Comments (${comments.size})") }
//
//                    if (comments.isEmpty()) {
//                        P({ classes("muted") }) { Text("No comments yet.") }
//                    } else {
//                        comments.forEach { c ->
//                            CommentView(c)
//                        }
//                    }
//                }
            }
        }
    }
}

@Composable
fun CommentView(c: Comment) {
    Div({ attr("style", "margin-left: 8px; margin-bottom: 12px;") }) {
        P({ classes("small") }) {
            B { Text("User #${c.authorId}") }
            Text(" · ${c.createdAt}")
        }
        P {
            Text(c.content)
        }
    }
}
