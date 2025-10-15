// File: src/jsMain/kotlin/app/pages/PostDetailPage.kt
package app.pages

import androidx.compose.runtime.*
import app.Post
import app.Comment
import app.getCommentsForPost
import app.createComment
import app.util.ReversibleUserId32
import app.components.UpvoteButton
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.dom.*
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement

// ───────────────────────────────────────────────────────────────────────────────
// API (mirrors your form/x-www-form-urlencoded server style; this endpoint is JSON)
// ───────────────────────────────────────────────────────────────────────────────

private const val API_PORT = 8079

private fun apiBaseUrl(): String {
    val host = window.location.hostname
    return "http://$host:$API_PORT"
}

private suspend fun fetchPostById(id: Long): Post {
    val url = "${apiBaseUrl()}/api/posts/$id"
    val resp = window.fetch(
        url,
        RequestInit(
            method = "GET",
            headers = Headers().also { it.append("Accept", "application/json") }
        )
    ).await()

    val body = try { resp.text().await() } catch (_: Throwable) { "" }
    if (!resp.ok) {
        throw RuntimeException("GET $url failed: HTTP ${resp.status} ${resp.statusText}${if (body.isNotBlank()) " – $body" else ""}")
    }

    return Json { ignoreUnknownKeys = true }.decodeFromString(Post.serializer(), body)
}

// ───────────────────────────────────────────────────────────────────────────────
// Small local styles
// ───────────────────────────────────────────────────────────────────────────────

@Composable
fun InstallDetailStyles() {
    LaunchedEffect(Unit) {
        if (document.getElementById("detail-styles") == null) {
            val css = """
                @keyframes fadeInUp {
                  from { opacity: 0; transform: translateY(6px); }
                  to   { opacity: 1; transform: translateY(0); }
                }
                @keyframes shimmer {
                  from { background-position: -200px 0; }
                  to   { background-position: 200px 0; }
                }
                .fade-in { animation: fadeInUp 220ms ease-out both; }
                .sk-block {
                  height: 12px;
                  width: 100%;
                  border-radius: 999px;
                  background: linear-gradient(90deg,#f3f4f6 25%,#e5e7eb 37%,#f3f4f6 63%);
                  background-size: 400px 100%;
                  animation: shimmer 1100ms linear infinite;
                }
                .sk-gap { height: 10px; }
                .sk-w-70 { width: 70%; }
                .sk-w-40 { width: 40%; }
                .comment-item {
                  padding: 12px;
                  background: #f9fafb;
                  border-radius: 8px;
                  margin-bottom: 12px;
                }
                .comment-meta {
                  font-size: 0.875rem;
                  color: #6b7280;
                  margin-bottom: 6px;
                }
            """.trimIndent()

            val styleEl = document.createElement("style")
            styleEl.setAttribute("id", "detail-styles")
            styleEl.setAttribute("type", "text/css")
            styleEl.textContent = css
            document.head?.appendChild(styleEl)
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────────
// Page
// ───────────────────────────────────────────────────────────────────────────────

@Composable
fun PostDetailPage(postId: Long) {
    InstallDetailStyles()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var post by remember { mutableStateOf<Post?>(null) }
    var copied by remember { mutableStateOf(false) }

    // Comments state
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentsLoading by remember { mutableStateOf(false) }
    var commentsError by remember { mutableStateOf<String?>(null) }

    // Comment form state
    var commentContent by remember { mutableStateOf("") }
    var submittingComment by remember { mutableStateOf(false) }
    var commentStatus by remember { mutableStateOf<String?>(null) }

    // Auth state
    var loggedIn by remember { mutableStateOf(window.localStorage.getItem("loggedIn") == "true") }
    var username by remember { mutableStateOf(window.localStorage.getItem("username") ?: "") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(postId) {
        loading = true
        error = null
        post = null
        try {
            delay(60)
            post = fetchPostById(postId)
        } catch (t: Throwable) {
            error = t.message ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    // Fetch comments
    LaunchedEffect(postId) {
        commentsLoading = true
        commentsError = null
        try {
            comments = getCommentsForPost(postId.toInt())
        } catch (t: Throwable) {
            commentsError = t.message ?: "Failed to load comments"
        } finally {
            commentsLoading = false
        }
    }

    Div({ classes("container"); attr("style", "padding: 16px 0 24px;") }) {
        // Top actions
        Div({ classes("row"); attr("style", "gap: 8px; margin-bottom: 12px;") }) {
            Button(attrs = {
                classes("btn")
                onClick { window.history.back() }
                attr("title", "Go back")
            }) { Text("← Back") }

            A("#/", attrs = {
                classes("btn")
                attr("title", "Home")
            }) { Text("Home") }
        }

        when {
            loading -> {
                Div({ classes("card") }) {
                    Div({ classes("sk-block") })
                    Div({ classes("sk-gap") })
                    Div({ classes("sk-block", "sk-w-70") })
                    Div({ classes("sk-gap") })
                    Div({ classes("sk-block") })
                    Div({ classes("sk-gap") })
                    Div({ classes("sk-block", "sk-w-40") })
                }
            }

            error != null -> {
                Div({
                    classes("card", "stack")
                    attr("style", "border:1px solid #fee2e2; background:#fef2f2;")
                }) {
                    H3 { Text("Error loading post") }
                    P({ classes("small"); attr("style", "color:#7f1d1d;") }) { Text(error!!) }
                    Div({ classes("row"); attr("style", "gap: 8px;") }) {
                        Button(attrs = {
                            classes("btn")
                            onClick { window.location.reload() }
                        }) { Text("Retry") }
                        Button(attrs = {
                            classes("btn")
                            onClick { window.history.back() }
                        }) { Text("Back") }
                    }
                }
            }

            else -> {
                val p = post!!
                Div({ classes("card", "stack", "fade-in") }) {
                    // Title row
                    Div({ classes("row") }) {
                        H2 { Text(p.title) }
                    }

                    // Meta row
                    Div({ classes("row") }) {
                        val author = ReversibleUserId32.decode(p.authorId)
                        Span({ classes("small", "muted") }) {
                            Text("by $author · ${p.createdAt}")
                        }
                        Div({ classes("spacer") }) {}

                        // Right-side controls stacked vertically:
                        Div({
                            attr(
                                "style",
                                "display:flex;flex-direction:column;gap:6px;align-items:flex-end;"
                            )
                        }) {
                            // ▲ Upvote button (compact) ABOVE the Copy link button
                            UpvoteButton(
                                postId = p.id.toString(),
                                compact = true
                            )

                            Button(attrs = {
                                classes("btn")
                                onClick {
                                    val link = "${window.location.origin}${window.location.pathname}#/post/${p.id}"
                                    scope.launch {
                                        try {
                                            window.navigator.clipboard.writeText(link).await()
                                            copied = true
                                            delay(1200)
                                            copied = false
                                        } catch (_: Throwable) {
                                            copied = false
                                        }
                                    }
                                }
                            }) { Text(if (copied) "Copied ✓" else "Copy link") }
                        }
                    }

                    // Divider
                    Hr()

                    // Content
                    P {
                        Text(p.content)
                    }

                    // Comments section
                    Div({ classes("card", "stack", "fade-in"); attr("style", "margin-top: 16px;") }) {
                        H3 { Text("Comments (${comments.size})") }

                        Hr()

                        // Comment form
                        if (loggedIn) {
                            Div({ classes("stack"); attr("style", "margin-bottom: 24px;") }) {
                                TextArea(attrs = {
                                    classes("textarea")
                                    attr("placeholder", "Write a comment...")
                                    attr("rows", "3")
                                    value(commentContent)
                                    addEventListener("input") { e ->
                                        val t = e.target as? HTMLTextAreaElement
                                        commentContent = t?.value.orEmpty()
                                    }
                                })

                                if (commentStatus != null) {
                                    Div({
                                        classes(
                                            "alert",
                                            if (commentStatus!!.startsWith("Error")) "alert-error" else "alert-success"
                                        )
                                    }) {
                                        Text(commentStatus!!)
                                    }
                                }

                                Button(attrs = {
                                    classes("btn")
                                    if (submittingComment || commentContent.isBlank()) attr("disabled", "true")
                                    onClick {
                                        scope.launch {
                                            submittingComment = true
                                            commentStatus = null
                                            try {
                                                createComment(
                                                    postId = postId.toInt(),
                                                    username = username,
                                                    content = commentContent
                                                )
                                                commentStatus = "Comment posted!"
                                                commentContent = ""
                                                // Refresh comments
                                                comments = getCommentsForPost(postId.toInt())
                                            } catch (t: Throwable) {
                                                commentStatus = "Error: ${t.message}"
                                            } finally {
                                                submittingComment = false
                                            }
                                        }
                                    }
                                }) {
                                    Text(if (submittingComment) "Posting..." else "Post Comment")
                                }
                            }
                        } else {
                            Div({ classes("alert", "alert-warning") }) {
                                Text("You must be ")
                                A("#/login") { Text("logged in") }
                                Text(" to comment.")
                            }
                        }

                        // Comments list
                        when {
                            commentsLoading -> {
                                Div({ classes("sk-block") })
                                Div({ classes("sk-gap") })
                                Div({ classes("sk-block", "sk-w-70") })
                            }

                            commentsError != null -> {
                                Div({ classes("alert", "alert-error") }) {
                                    Text("Error loading comments: $commentsError")
                                }
                            }

                            comments.isEmpty() -> {
                                P({ classes("muted", "small") }) {
                                    Text("No comments yet. Be the first to comment!")
                                }
                            }

                            else -> {
                                comments.forEach { comment ->
                                    Div({ attr("class", "comment-item") }) {
                                        Div({ attr("class", "comment-meta") }) {
                                            B { Text(comment.authorUsername) }
                                            Text(" · ${comment.formattedDate()}")
                                        }
                                        P({ attr("style", "margin: 0;") }) {
                                            Text(comment.content)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
