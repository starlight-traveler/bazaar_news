// File: src/jsMain/kotlin/app/pages/PostDetailPage.kt
package app.pages

import androidx.compose.runtime.*
import app.Post
import app.util.ReversibleUserId32
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.*
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.jetbrains.compose.web.dom.Style
import org.jetbrains.compose.web.dom.Text
import androidx.compose.runtime.Composable
import kotlinx.browser.document

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
// Small local styles: minimal, complements your index.html classes
// ───────────────────────────────────────────────────────────────────────────────

// imports you need:

@Composable
fun InstallDetailStyles() {
    LaunchedEffect(Unit) {
        // Prevent duplicates on recomposition/hot-reload
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
    val scope = rememberCoroutineScope()

    LaunchedEffect(postId) {
        loading = true
        error = null
        post = null
        try {
            // tiny delay lets skeleton paint (optional)
            delay(60)
            post = fetchPostById(postId)
        } catch (t: Throwable) {
            error = t.message ?: "Unknown error"
        } finally {
            loading = false
        }
    }

    Div({ classes("container"); attr("style", "padding: 16px 0 24px;") }) {
        // Top actions (match your button look)
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
                // Skeleton card (uses your .card border radius & rhythm)
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
                        H2 {
                            Text(p.title)
                        }
                    }

                    // Meta row
                    Div({ classes("row") }) {
                        val author = ReversibleUserId32.decode(p.authorId)
                        Span({ classes("small", "muted") }) {
                            Text("by $author · ${p.createdAt}")
                        }
                        Div({ classes("spacer") }) {}
                        // Copy link action
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

                    // Divider (using your rhythm)
                    Hr()

                    // Content
                    P {
                        // Respect your base typography; no extra classes needed
                        Text(p.content)
                    }
                }
            }
        }
    }
}
