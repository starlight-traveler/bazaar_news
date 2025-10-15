package app

import androidx.compose.runtime.*
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

// Pages
import app.pages.TopPage
import app.pages.NewPage
import app.pages.SubmitPage
import app.pages.RegisterPage
import app.pages.ItemPage              // keep if you still use "#/item/{id}"
import app.pages.PostDetailPage       // <-- new post detail page

import app.components.NavBar
import app.pages.MapPage
import app.util.ReversibleUserId32
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

// Point directly at your Ktor server during dev.
private const val API_BASE = "http://localhost:8079"

// Keep enum routes (we'll store the numeric id separately like you already do)
enum class Route {
    Top, Map, New, Item, Post, Submit, Login, Register
}

@Composable
fun App() {
    var route by remember { mutableStateOf(currentRouteFromHash()) }
    var itemId by remember { mutableStateOf<Long?>(null) }   // reused for both Item and Post ids

    // Centralized auth state (reactive)
    var loggedIn by remember { mutableStateOf(window.localStorage.getItem("loggedIn") == "true") }
    var username by remember { mutableStateOf(window.localStorage.getItem("username") ?: "") }

    // Ensure we have a sensible initial hash
    LaunchedEffect(Unit) {
        if (window.location.hash.isBlank() || window.location.hash == "#") {
            window.location.hash = "#/top"
        } else {
            // sync initial state if user came in deep-linked
            val (r, id) = parseHashRoute()
            route = r
            itemId = id
        }
    }

    // Listen for hash changes (routing) and storage changes (multi-tab or external updates)
    LaunchedEffect(Unit) {
        window.onhashchange = {
            val (r, id) = parseHashRoute()
            route = r
            itemId = id
        }
        // Keep in sync if other tabs change auth, or if you manually tweak localStorage
        window.addEventListener("storage", {
            loggedIn = (window.localStorage.getItem("loggedIn") == "true")
            username = (window.localStorage.getItem("username") ?: "")
        })
    }

    // Logout handler (clears state + storage, navigates to login)
    fun handleLogout() {
        window.localStorage.removeItem("loggedIn")
        window.localStorage.removeItem("username")
        window.localStorage.removeItem("userId")
        loggedIn = false
        username = ""
        window.location.hash = "#/login"
    }

    // ───────────────────────────────── Top Nav ─────────────────────────────────
    Div({ classes("navbar") }) {
        Div({ classes("container") }) {
            NavBar(
                current = route,
                loggedIn = loggedIn,
                displayName = username,
                onLogout = { handleLogout() },
                onNavigate = { r ->
                    when (r) {
                        Route.Map -> window.location.hash = "#/map"
                        Route.Top -> window.location.hash = "#/top"
                        Route.New -> window.location.hash = "#/new"
                        Route.Submit -> window.location.hash = "#/submit"
                        Route.Login -> window.location.hash = "#/login"
                        Route.Register -> window.location.hash = "#/register"
                        Route.Item, Route.Post -> { /* navigated elsewhere with an id */ }
                    }
                }
            )
        }
    }

    // ──────────────────────────────── Page Body ────────────────────────────────
    Div({
        classes("container")
        attr("style", "padding: 16px 0 32px")
    }) {
        when (route) {
            Route.Map -> MapPage()
            Route.Top -> TopPage("top")
            Route.New -> NewPage()
            Route.Item -> ItemPage(itemId ?: -1L)
            Route.Post -> PostDetailPage(itemId ?: -1L)
            Route.Submit -> SubmitPage()
            Route.Login -> LoginPage(
                onLoggedIn = { name ->
                    val userId = ReversibleUserId32.encode(name)
                    window.localStorage.setItem("loggedIn", "true")
                    window.localStorage.setItem("username", name)
                    window.localStorage.setItem("userId", userId.toString())

                    // Lift state immediately
                    loggedIn = true
                    username = name

                    // Notify any listeners
                    window.dispatchEvent(Event("auth-change"))

                    // Navigate after login
                    window.location.hash = "#/top"
                }
            )
            Route.Register -> RegisterPage()
        }
    }

    // ───────────────────────────────── Footer ──────────────────────────────────
    Div({ classes("footer") }) {
        Text("Built with Kotlin + Compose Web · ")
        A("https://kotlinlang.org") { Text("Kotlin") }
        Text(" · ")
        A("https://www.jetbrains.com/lp/compose-mpp/") { Text("Compose Multiplatform") }
    }
}

/**
 * Parses the current location hash into a Route and optional ID.
 *
 * Supported:
 *   #/top
 *   #/new
 *   #/submit
 *   #/login
 *   #/register
 *   #/item/{id}   (kept for compatibility)
 *   #/post/{id}   <-- NEW
 *
 * Unrecognized routes fall back to Top.
 */
fun parseHashRoute(): Pair<Route, Long?> {
    val raw = window.location.hash.removePrefix("#").let { if (it.startsWith("/")) it.drop(1) else it }
    val parts = raw.split("/").filter { it.isNotBlank() }

    if (parts.isEmpty()) return Route.Top to null

    return when (parts[0]) {
        "map" -> Route.Map to null
        "top" -> Route.Top to null
        "new" -> Route.New to null
        "submit" -> Route.Submit to null
        "login" -> Route.Login to null
        "register" -> Route.Register to null
        "item" -> {
            val id = parts.getOrNull(1)?.toLongOrNull()
            Route.Item to id
        }
        "post" -> {
            val id = parts.getOrNull(1)?.toLongOrNull()
            Route.Post to id
        }
        else -> Route.Top to null
    }
}

fun currentRouteFromHash(): Route = parseHashRoute().first

fun main() {
    renderComposable(rootElementId = "root") {
        App()
    }
}

// ─────────────────────────────── Login Page (unchanged) ───────────────────────
@Composable
fun LoginPage(onLoggedIn: (displayName: String) -> Unit) {
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    fun clearMessages() {
        error = null
        success = null
    }

    Div({ classes("card", "stack") }) {
        H2 { Text("Login") }
        P({ classes("muted") }) {
            Text("Credentials are sent to ")
            Code { Text("POST /api/login") }
            Text(" as ")
            Code { Text("application/x-www-form-urlencoded") }
            Text(".")
        }

        if (error != null) Div({ classes("alert", "alert-error") }) { Text(error!!) }
        if (success != null) Div({ classes("alert", "alert-success") }) { Text(success!!) }

        // Username
        Label(forId = "login-username") { Text("Username") }
        Input(InputType.Text, attrs = {
            id("login-username")
            classes("input")
            placeholder("Username")
            value(username)
            addEventListener("input") { event ->
                val target = event.target as? HTMLInputElement
                username = target?.value.orEmpty()
                clearMessages()
            }
        })

        // Password
        Label(forId = "login-password") { Text("Password") }
        Input(InputType.Password, attrs = {
            id("login-password")
            classes("input")
            placeholder("Password")
            value(password)
            addEventListener("input") { event ->
                val target = event.target as? HTMLInputElement
                password = target?.value.orEmpty()
                clearMessages()
            }
        })

        Button(attrs = {
            classes("btn")
            if (busy) attr("disabled", "")
            addEventListener("click") {
                scope.launch {
                    clearMessages()
                    if (username.isBlank() || password.isBlank()) {
                        error = "Username and password are required."
                        return@launch
                    }

                    busy = true
                    try {
                        val form = URLSearchParams().apply {
                            append("username", username)
                            append("password", password)
                        }

                        val resp = window.fetch(
                            "$API_BASE/api/login",
                            RequestInit(
                                method = "POST",
                                headers = Headers().also {
                                    it.append("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                                    it.append("Accept", "text/plain")
                                },
                                body = form
                            )
                        ).await()

                        val text = resp.text().await()

                        when (resp.status.toInt()) {
                            200 -> {
                                success = text.ifBlank { "Login successful!" }
                                onLoggedIn(username)
                                password = ""
                            }
                            400 -> error = if (text.isNotBlank()) text else "Invalid request."
                            401 -> error = "Invalid password."
                            404 -> error = "User not found."
                            else -> error = "Login failed (${resp.status}). ${if (text.isNotBlank()) text else ""}"
                        }
                    } catch (t: Throwable) {
                        error = "Network or server error: ${t.message ?: t.toString()}"
                    } finally {
                        busy = false
                    }
                }
            }
        }) {
            if (busy) Text("Signing in…") else Text("Log in")
        }

        P({ classes("muted") }) {
            Text("No account yet? ")
            A("#/register") { Text("Create one") }
        }
    }
}
