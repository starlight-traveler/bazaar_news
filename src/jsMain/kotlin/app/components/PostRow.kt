package app.components

import androidx.compose.runtime.*
import app.Post
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.url.URL

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*
import app.util.ReversibleUserId32

@Composable
fun PostRow(post: Post, index: Int? = null) {
    Div({ classes("card") }) {

        // 🔹 Row 1: Title
        Div({ classes("row") }) {
            if (index != null) {
                Span({
                    classes("muted")
                    attr("style", "width: 28px; text-align: right; margin-right: 8px;")
                }) {
                    Text("$index.")
                }
            }

            // Post title as a link
            A("#/post/${post.id}") {
                B { Text(post.title) }
            }
        }

        // 🔹 Row 2: Content (snippet)
        Div({ classes("row") }) {
            P({
                classes("small")
                attr("style", "color: #666; margin-top: 4px;")
            }) {
                // Show a short preview if content is long
                val preview = if (post.content.length > 150)
                    post.content.take(150) + "..."
                else post.content

                Text(preview)
            }
        }

        // 🔹 Row 3: Meta info
        Div({ classes("row") }) {
            Span({ classes("small", "muted") }) {
                Text("Author ID: ${ReversibleUserId32.decode(post.authorId)} · Posted on ${post.createdAt}")
            }

            Div({ classes("spacer") }) {}

            // Link to view full post
//            A("#/post/${post.id}") {
//                Span({ classes("small", "linkish") }) { Text("View details") }
//            }
        }
    }
}

private fun hostname(url: String): String = try {
    URL(url).host
} catch (_: Throwable) {
    ""
}
