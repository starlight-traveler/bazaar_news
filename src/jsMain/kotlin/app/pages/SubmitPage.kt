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
    //authorId: String,
    username: String
): String {
    val url = "${apiBaseUrl()}/api/posts"

    // Build a form body because server reads call.receiveParameters()
    val form = URLSearchParams().apply {
        append("title", title)
        append("content", content)
       // append("authorId", authorId)
        append("username", username)
    // must be parseable as Int server-side
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
    var loggedIn by remember { mutableStateOf(window.localStorage.getItem("loggedIn") == "true") }
    var displayName by remember { mutableStateOf(window.localStorage.getItem("username") ?: "") }

    DisposableEffect(Unit) {
        val handler: (org.w3c.dom.events.Event) -> Unit = {
            loggedIn = (window.localStorage.getItem("loggedIn") == "true")
            displayName = (window.localStorage.getItem("username") ?: "")
        }
        window.addEventListener("storage", handler)
        window.addEventListener("auth-change", handler)
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

    // NEW: outer container centers the page content
    Div({ classes("container") }) {
        Div({ classes("card", "stack") }) {
            H2 { Text("Create a New Post") }

            if (!loggedIn) {
                Div({ classes("alert", "alert-warning") }) {
                    Text("You must be logged in to submit.")
                }
                Button(attrs = {
                    classes("btn")
                    addEventListener("click") { window.location.hash = "#/login" }
                }) { Text("Go to Login") }
                return@Div
            }

            P {
                B { Text("Author: ") }
                Text(displayName.ifBlank { "User" })
            }

            Input(InputType.Text, attrs = {
                classes("input")
                placeholder("Title")
                value(title)
                addEventListener("input") { e ->
                    title = (e.target as? HTMLInputElement)?.value.orEmpty()
                }
            })

            TextArea(attrs = {
                classes("textarea")
                placeholder("Write your post content here…")
                value(content)
                addEventListener("input") { e ->
                    content = (e.target as? HTMLTextAreaElement)?.value.orEmpty()
                }
            })

            Button(attrs = {
                classes("btn")
                if (submitting) attr("disabled", "true")
                addEventListener("click") {
                    val username = window.localStorage.getItem("username")
                    if (title.isBlank() || content.isBlank()) {
                        status = "Please fill in the Title and Content."
                        return@addEventListener
                    }
                    if (username == null) {
                        status = "Cannot submit: missing user id. Ensure login stores localStorage.userId."
                        return@addEventListener
                    }
                    scope.launch {
                        submitting = true
                        status = null
                        try {
                            val msg = createPost(title = title, content = content, username = username)
                            status = msg
                            title = ""
                            content = ""
                        } catch (t: Throwable) {
                            status = "Error: ${t.message}"
                        } finally {
                            submitting = false
                        }
                    }
                }
            }) {
                Text(if (submitting) "Submitting…" else "Submit")
            }

            if (status != null) {
                P { Text(status!!) }
            }
        }
    }
}

