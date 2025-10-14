package app.pages

import androidx.compose.runtime.*
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.w3c.dom.url.URLSearchParams
import app.util.ReversibleUserId32.encode

// ───────────────────────────────────────────────────────────────────────────────
// Inline API client: submit as application/x-www-form-urlencoded to /posts (port 8079)
// Ktor route is expecting call.receiveParameters(), not JSON.
// ───────────────────────────────────────────────────────────────────────────────

private const val API_PORT = 8079

private fun apiBaseUrl(): String {
    val host = window.location.hostname // keep same hostname the app was served from
    return "http://$host:$API_PORT"
}

/**
 * POST /posts (form-encoded)
 * Body (x-www-form-urlencoded):
 *   title=<...>&content=<...>&authorId=<int>
 * Returns plain text like: "Post created with id 123"
 */
private suspend fun createPost(
    title: String,
    content: String,
    authorId: String
): String {
    val url = "${apiBaseUrl()}/api/posts"

    // Build a form body because server reads call.receiveParameters()
    val form = URLSearchParams().apply {
        append("title", title)
        append("content", content)
        append("authorId", authorId) // must be parseable as Int server-side
    }

    val init = RequestInit(
        method = "POST",
        // NOTE: When body is URLSearchParams, fetch will set the proper Content-Type automatically.
        // Some servers require it explicitly, so we also set it.
        headers = Headers().also {
            it.append("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            it.append("Accept", "text/plain, */*")
        },
        body = form
    )

    val resp = window.fetch(url, init).await()
    val text = try { resp.text().await() } catch (_: Throwable) { "" }

    if (!resp.ok) {
        throw RuntimeException("POST $url failed: HTTP ${resp.status} ${resp.statusText}${if (text.isNotBlank()) " – $text" else ""}")
    }

    return if (text.isNotBlank()) text else "Created"
}

// ───────────────────────────────────────────────────────────────────────────────
// UI: SubmitPage
// ───────────────────────────────────────────────────────────────────────────────

@Composable
fun SubmitPage() {
    // Reactive login state sourced from localStorage so we don't rely on App props
    var loggedIn by remember { mutableStateOf(window.localStorage.getItem("loggedIn") == "true") }
    var displayName by remember { mutableStateOf(window.localStorage.getItem("displayName") ?: "") }

    // Keep in sync if other code changes localStorage (multi-tab or after login)
    DisposableEffect(Unit) {
        val handler: (org.w3c.dom.events.Event) -> Unit = {
            loggedIn = (window.localStorage.getItem("loggedIn") == "true")
            displayName = (window.localStorage.getItem("displayName") ?: "")
        }

        // Add event listeners when this composable enters composition
        window.addEventListener("storage", handler)
        window.addEventListener("auth-change", handler)

        // Remove them when the composable leaves composition
        onDispose {
            window.removeEventListener("storage", handler)
            window.removeEventListener("auth-change", handler)
        }
    }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Div({ classes("card", "stack") }) {
        H2 { Text("Create a New Post") }

        if (!loggedIn) {
            Div({ classes("alert", "alert-warning") }) {
                Text("You must be logged in to submit.")
            }
            Button(attrs = {
                classes("btn")
                addEventListener("click") {
                    window.location.hash = "#/login"
                }
            }) { Text("Go to Login") }
            // Early return UI-only (don’t render the form when logged out)
            return@Div
        }

        // Author display (read-only)
        P {
            B { Text("Author: ") }
            Text(displayName.ifBlank { "User" })
        }

        // Title input
        Input(InputType.Text, attrs = {
            classes("input")
            placeholder("Title")
            value(title)
            addEventListener("input") { e ->
                val t = e.target as? HTMLInputElement
                title = t?.value.orEmpty()
            }
        })

        // Content textarea
        TextArea(attrs = {
            classes("textarea")
            placeholder("Write your post content here…")
            value(content)
            addEventListener("input") { e ->
                val t = e.target as? HTMLTextAreaElement
                content = t?.value.orEmpty()
            }
        })

        Button(attrs = {
            classes("btn")
            if (submitting) attr("disabled", "true")
            addEventListener("click") {
                // Pull userId from localStorage (must be set at login time)
                val authorId = window.localStorage.getItem("userId")
                console.log("DEBUG: localStorage.userId =", authorId)

                if (title.isBlank() || content.isBlank()) {
                    status = "Please fill in the Title and Content."
                    console.log("DEBUG: missing title or content", title, content)
                    return@addEventListener
                }
                if (authorId == null) {
                    status = "Cannot submit: missing user id. Ensure login stores localStorage.userId."
                    console.log("DEBUG: authorId missing")
                    return@addEventListener
                }

                // The server expects an Int; ensure the stored ID is parseable
                if (authorId.toIntOrNull() == null) {
                    status = "Stored userId is not an integer. Fix the login flow to save a numeric userId."
                    console.log("DEBUG: authorId not an integer:", authorId)
                    return@addEventListener
                }

//                console.log("DEBUG: preparing to submit form:", js("({ title: title, content: content, authorId: authorId })"))

                scope.launch {
                    submitting = true
                    status = null
                    try {
                        val msg = createPost(
                            title = title,
                            content = content,
                            authorId = authorId
                        )
                        console.log("DEBUG: createPost() returned:", msg)
                        status = msg // e.g., "Post created with id 123"
                        title = ""
                        content = ""
                    } catch (t: Throwable) {
                        console.error("DEBUG: Error submitting post:", t)
                        status = "Error: ${t.message}"
                    } finally {
                        submitting = false
                        console.log("DEBUG: submit complete, submitting=false")
                    }
                }
            }
        }) {
            Text(if (submitting) "Submitting…" else "Submit")
        }
    }
    }
