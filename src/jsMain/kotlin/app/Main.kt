package app

import androidx.compose.runtime.*
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

// Attributes
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.value

// Raw DOM types for listeners
import org.w3c.dom.HTMLInputElement

import app.components.NavBar
import app.pages.ItemPage
import app.pages.NewPage
import app.pages.SubmitPage
import app.pages.TopPage

enum class Route {
    Top, New, Item, Submit, Login
}

@Composable
fun App() {
    var route by remember { mutableStateOf(currentRouteFromHash()) }
    var itemId by remember { mutableStateOf<Long?>(null) }

    // Simple hash-based router (#/top, #/item/123)
    LaunchedEffect(Unit) {
        window.onhashchange = {
            val (r, id) = parseHashRoute()
            route = r
            itemId = id
        }
    }

    Div({ classes("navbar") }) {
        Div({ classes("container") }) {
            NavBar(
                current = route,
                onNavigate = { r ->
                    when (r) {
                        Route.Top -> window.location.hash = "#/top"
                        Route.New -> window.location.hash = "#/new"
                        Route.Submit -> window.location.hash = "#/submit"
                        Route.Login -> window.location.hash = "#/login"
                        Route.Item -> { /* handled via Post click */ }
                    }
                }
            )
        }
    }

    Div({
        classes("container")
        attr("style", "padding: 16px 0 32px")
    }) {
        when (route) {
            Route.Top -> TopPage("top")
            Route.New -> NewPage()
            Route.Item -> ItemPage(itemId ?: -1L)
            Route.Submit -> SubmitPage()
            Route.Login -> LoginPage()
        }
    }

    Div({ classes("footer") }) {
        Text("Built with Kotlin + Compose Web · ")
        A("https://kotlinlang.org") { Text("Kotlin") }
        Text(" · ")
        A("https://www.jetbrains.com/lp/compose-mpp/") { Text("Compose Multiplatform") }
    }
}

fun parseHashRoute(): Pair<Route, Long?> {
    val h = window.location.hash.removePrefix("#")
    val parts = h.split("/").filter { it.isNotBlank() }

    return when (parts.firstOrNull()) {
        "top" -> Route.Top to null
        "new" -> Route.New to null
        "submit" -> Route.Submit to null
        "login" -> Route.Login to null
        "item" -> {
            val id = parts.getOrNull(1)?.toLongOrNull()
            Route.Item to id
        }
        else -> Route.New to null // default route (you can change to Submit/Login if you prefer)
    }
}


fun currentRouteFromHash(): Route = parseHashRoute().first

fun main() {
    renderComposable(rootElementId = "root") {
        App()
    }
}

@Composable
fun LoginPage() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Div({ classes("card", "stack") }) {
        H2 { Text("Login") }
        P({ classes("muted") }) {
            Text("Auth is handled by the backend; this demo keeps it simple.")
        }

        // Username
        Input(InputType.Text, attrs = {
            classes("input")
            placeholder("Username")
            value(username)
            // Raw DOM input listener (works on all Compose Web versions)
            addEventListener("input") { event ->
                val target = event.target as? HTMLInputElement
                username = target?.value.orEmpty()
            }
        })

        // Password
        Input(InputType.Password, attrs = {
            classes("input")
            placeholder("Password")
            value(password)
            addEventListener("input") { event ->
                val target = event.target as? HTMLInputElement
                password = target?.value.orEmpty()
            }
        })

        Button(attrs = {
            classes("btn")
            addEventListener("click") {
                js("console").asDynamic().log("Login clicked with user=$username")
            }
        }) { Text("Log in") }
    }
}
