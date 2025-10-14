package app.components

import androidx.compose.runtime.Composable
import app.Route
import org.jetbrains.compose.web.dom.*

@Composable
fun NavBar(
    current: Route,
    loggedIn: Boolean,
    displayName: String,
    onLogout: () -> Unit,
    onNavigate: (Route) -> Unit
) {
    Div({
        classes("container")
        attr("style", "padding: 10px 0")
    }) {
        Div({ classes("row") }) {
            // App brand
            A("#/top", attrs = { classes("brand") }) { Text("Bazaar News") }

            Text(" ")

            // Navigation links
            A("#/top", attrs = {
                onClick { onNavigate(Route.Top) }
                if (current == Route.Top) classes("active")
            }) { Text("Top") }

            Text(" | ")

            A("#/new", attrs = {
                onClick { onNavigate(Route.New) }
                if (current == Route.New) classes("active")
            }) { Text("New") }

            Text(" | ")

            A("#/submit", attrs = {
                onClick { onNavigate(Route.Submit) }
                if (current == Route.Submit) classes("active")
            }) { Text("Submit") }

            Div({ classes("spacer") }) {}

            if (!loggedIn) {
                // Register
                A("#/register", attrs = {
                    onClick { onNavigate(Route.Register) }
                    if (current == Route.Register) classes("active")
                }) { Text("Register") }

                Text(" | ")

                // Login
                A("#/login", attrs = {
                    onClick { onNavigate(Route.Login) }
                    if (current == Route.Login) classes("active")
                }) { Text("Login") }
            } else {
                // Show name, divider, Logout
                Span({ classes("muted") }) { Text(displayName.ifBlank { "User" }) }
                Text(" | ")
                A("#/login", attrs = {
                    onClick {
                        onLogout()
                    }
                }) { Text("Logout") }
            }
        }
    }
}
