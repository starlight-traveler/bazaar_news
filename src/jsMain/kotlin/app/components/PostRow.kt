package app.components

import androidx.compose.runtime.*
import app.Post
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.url.URL

@Composable
fun PostRow(post: Post, index: Int? = null) {
    Div({ classes("card") }) {
        Div({ classes("row") }) {
            if (index != null) {
                Span({ classes("muted"); attr("style", "width: 28px; text-align: right") }) {
                    Text("${index}.")
                }
            }
            A("#/item/${post.id}") {
                B { Text(post.title) }
            }
            if (!post.url.isNullOrBlank()) {
                Text(" ")
                A(post.url!!) {
                    Span({ classes("muted", "small") }) {
                        Text("(${hostname(post.url)})")
                    }
                }
            }
        }
        Div({ classes("row") }) {
            Span({ classes("small", "muted") }) {
                Text("${post.score} points by ${post.by} · ${post.commentCount} comments")
            }
            Div({ classes("spacer") }) {}
            A("#/item/${post.id}") { Span({ classes("small", "linkish") }) { Text("discuss") } }
        }
    }
}

private fun hostname(url: String): String = try {
    URL(url).host
} catch (_: Throwable) {
    ""
}
