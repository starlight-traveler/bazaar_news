// File: src/jsMain/kotlin/app/components/UpvoteButton.kt
package app.components

import androidx.compose.runtime.*
import app.state.UpvoteStore
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.events.Event

/**
 * Upvote button that talks to the server (DB-backed) with optimistic UI.
 *
 * Login contract:
 *   localStorage["loggedIn"] == "true"
 *   localStorage["username"] == <username>
 */
@Composable
fun UpvoteButton(
    postId: String,
    compact: Boolean = false,
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()

    // Reactive state from store
    val count = UpvoteStore.counts[postId] ?: 0
    val alreadyVoted = UpvoteStore.getYouVoted(postId)

    // Auth snapshot
    var loggedIn by remember { mutableStateOf(window.localStorage.getItem("loggedIn") == "true") }
    var username by remember { mutableStateOf(window.localStorage.getItem("username") ?: "") }

    // Request-in-flight guard to prevent double clicks
    var submitting by remember { mutableStateOf(false) }

    // Keep auth in sync across tabs and your App's custom event
    DisposableEffect(Unit) {
        val storageListener: (dynamic) -> Unit = {
            loggedIn = (window.localStorage.getItem("loggedIn") == "true")
            username = (window.localStorage.getItem("username") ?: "")
        }
        val authChangeListener: (Event) -> Unit = {
            loggedIn = (window.localStorage.getItem("loggedIn") == "true")
            username = (window.localStorage.getItem("username") ?: "")
        }
        window.addEventListener("storage", storageListener)
        window.addEventListener("auth-change", authChangeListener)
        onDispose {
            window.removeEventListener("storage", storageListener)
            window.removeEventListener("auth-change", authChangeListener)
        }
    }

    // Load server truth at mount and when identity changes
    LaunchedEffect(postId, username) {
        UpvoteStore.refreshCount(postId)
        UpvoteStore.refreshYouVoted(postId, if (loggedIn) username else null)
    }

    val label = remember(count, compact) {
        if (compact) "▲ $count" else "Upvote ▲ $count"
    }

    val buttonTitle = when {
        submitting -> "Submitting…"
        !loggedIn -> "Log in to upvote"
        alreadyVoted -> "You already upvoted"
        else -> "Upvote this post"
    }

    Button({
        classes("btn")
        title(buttonTitle)
        if (submitting || !loggedIn || alreadyVoted) disabled()

        onClick {
            if (!loggedIn) {
                window.location.hash = "#/login"
                return@onClick
            }
            if (alreadyVoted || submitting) return@onClick

            submitting = true
            scope.launch {
                try {
                    // Optimistic update happens inside UpvoteStore.upvote
                    UpvoteStore.upvote(postId, username)
                } finally {
                    submitting = false
                }
            }
        }

        attrs?.invoke(this)
    }) {
        Text(label)
    }
}
