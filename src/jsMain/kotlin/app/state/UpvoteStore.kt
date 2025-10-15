// File: src/jsMain/kotlin/app/state/UpvoteStore.kt
package app.state

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.w3c.dom.url.URLSearchParams

/**
 * Network-backed upvotes store (authoritative in DB via Ktor) with optimistic updates.
 *
 * Endpoints expected:
 *   GET  /api/posts/{id}/upvotes                 -> { "count": number }
 *   GET  /api/posts/{id}/upvotes/me?username=U   -> { "youVoted": boolean }
 *   POST /api/posts/{id}/upvote  (form username) -> { "count": number, "youVoted": true }
 */
object UpvoteStore {
    // NOTE: This points to localhost. If your frontend is served from a different host,
    // consider aligning this with your existing apiBaseUrl() logic.
    private const val API_BASE = "http://localhost:8079"

    /** Reactive count per postId */
    val counts: SnapshotStateMap<String, Int> = mutableStateMapOf()

    /** Reactive "you voted" per postId (for the current user) */
    val youVoted: SnapshotStateMap<String, Boolean> = mutableStateMapOf()

    fun getCount(postId: String): Int = counts[postId] ?: 0
    fun getYouVoted(postId: String): Boolean = youVoted[postId] == true

    /** Fetch authoritative count from server and update state. */
    suspend fun refreshCount(postId: String) {
        val resp = window.fetch("$API_BASE/api/posts/${postId}/upvotes").await()
        if (!resp.ok) return
        val obj = resp.json().await().unsafeCast<dynamic>()
        val n = (obj.count as? Double)?.toInt() ?: (obj.count as? Int) ?: 0
        counts[postId] = n
    }

    /** Fetch whether THIS username has upvoted the post; if no username, set false. */
    suspend fun refreshYouVoted(postId: String, username: String?) {
        if (username.isNullOrBlank()) {
            youVoted[postId] = false
            return
        }
        val resp = window.fetch("$API_BASE/api/posts/${postId}/upvotes/me?username=$username").await()
        if (!resp.ok) {
            youVoted[postId] = false
            return
        }
        val obj = resp.json().await().unsafeCast<dynamic>()
        val v = (obj.youVoted as? Boolean) ?: false
        youVoted[postId] = v
    }

    /**
     * Idempotent upvote with **optimistic** UI:
     * - Immediately set youVoted=true and +1 count (if not already true)
     * - POST to server
     * - On failure, roll back to previous values
     * - On success, set server-returned values (authoritative)
     */
    suspend fun upvote(postId: String, username: String): Boolean {
        if (username.isBlank()) return false

        // Snapshot + optimistic
        val prevCount = counts[postId] ?: 0
        val prevYou = youVoted[postId] ?: false
        if (!prevYou) {
            counts[postId] = prevCount + 1
            youVoted[postId] = true
        }

        return try {
            val form = URLSearchParams().apply { append("username", username) }
            val resp = window.fetch(
                "$API_BASE/api/posts/${postId}/upvote",
                RequestInit(
                    method = "POST",
                    headers = Headers().also {
                        it.append("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                        it.append("Accept", "application/json")
                    },
                    body = form
                )
            ).await()

            if (!resp.ok) {
                // Roll back on non-2xx
                counts[postId] = prevCount
                youVoted[postId] = prevYou
                return false
            }

            // Try to parse JSON; if it fails, keep optimistic UI and fall back to refresh.
            try {
                val obj = resp.json().await().unsafeCast<dynamic>()
                val n = (obj.count as? Double)?.toInt() ?: (obj.count as? Int)
                val v = (obj.youVoted as? Boolean)

                if (n != null) counts[postId] = maxOf(n, counts[postId] ?: n) // avoid dropping optimistic value
                if (v != null) youVoted[postId] = v
            } catch (_: Throwable) {
                // ignore parse error – refresh below
            }

            // Converge to server truth
            refreshCount(postId)
            refreshYouVoted(postId, username)

            true
        } catch (_: Throwable) {
            // Network error → rollback
            counts[postId] = prevCount
            youVoted[postId] = prevYou
            false
        }
    }
}
