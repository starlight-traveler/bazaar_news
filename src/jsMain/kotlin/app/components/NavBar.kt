package app.components

import androidx.compose.runtime.Composable
import app.Route
import org.jetbrains.compose.web.dom.*

@Composable
fun NavBar(current: Route, onNavigate: (Route) -> Unit) {
    Div({ classes("container"); attr("style", "padding: 10px 0") }) {
        Div({ classes("row") }) {
            A("#/top", attrs = { classes("brand") }) { Text("Bazaar News") }
            Text(" ")
            A("#/top", attrs = { onClick { onNavigate(Route.Top) } }) { Text("Top") }
            Text(" | ")
            A("#/new", attrs = { onClick { onNavigate(Route.New) } }) { Text("New") }
            Text(" | ")
            A("#/submit", attrs = { onClick { onNavigate(Route.Submit) } }) { Text("Submit") }
            Div({ classes("spacer") }) {}
            A("#/login", attrs = { onClick { onNavigate(Route.Login) } }) { Text("Login") }
        }
    }
}
