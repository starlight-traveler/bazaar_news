package app.pages

import androidx.compose.runtime.*
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.coroutines.await
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.value
import org.jetbrains.compose.web.attributes.forId
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

private const val API_BASE = "http://localhost:8079" // <-- your Ktor host:port

@Composable
fun RegisterPage() {
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    fun validate(): String? {
        if (username.isBlank()) return "Username is required."
        if (password.isBlank()) return "Password is required."
        if (password.length < 6) return "Password must be at least 6 characters."
        if (password != confirm) return "Passwords do not match."
        return null
    }

    fun clearMessages() {
        error = null
        success = null
    }

    Div({ classes("card", "stack") }) {
        H2 { Text("Create your account") }
        P({ classes("muted") }) {
            Text("Your credentials are sent to ")
            Code { Text("POST /api/register") }
            Text(" as form data (x-www-form-urlencoded).")
        }

        if (error != null) {
            Div({ classes("alert", "alert-error") }) { Text(error!!) }
        }
        if (success != null) {
            Div({ classes("alert", "alert-success") }) { Text(success!!) }
        }

        // Username
        Label(forId = "reg-username") { Text("Username") }
        Input(InputType.Text, attrs = {
            id("reg-username")
            classes("input")
            placeholder("e.g. alice")
            value(username)
            addEventListener("input") { ev ->
                val v = (ev.target as? HTMLInputElement)?.value.orEmpty()
                username = v
                clearMessages()
            }
        })

        // Password
        Label(forId = "reg-password") { Text("Password") }
        Input(InputType.Password, attrs = {
            id("reg-password")
            classes("input")
            placeholder("••••••••")
            value(password)
            addEventListener("input") { ev ->
                val v = (ev.target as? HTMLInputElement)?.value.orEmpty()
                password = v
                clearMessages()
            }
        })

        // Confirm Password
        Label(forId = "reg-confirm") { Text("Confirm Password") }
        Input(InputType.Password, attrs = {
            id("reg-confirm")
            classes("input")
            placeholder("••••••••")
            value(confirm)
            addEventListener("input") { ev ->
                val v = (ev.target as? HTMLInputElement)?.value.orEmpty()
                confirm = v
                clearMessages()
            }
        })

        Button(attrs = {
            classes("btn")
            if (busy) attr("disabled", "")
            addEventListener("click") {
                scope.launch {
                    clearMessages()
                    val v = validate()
                    if (v != null) {
                        error = v
                        return@launch
                    }

                    busy = true
                    try {
                        // Build x-www-form-urlencoded body via URLSearchParams
                        val form = URLSearchParams()
                        form.append("username", username)
                        form.append("password", password)

                        val resp = window.fetch(
                            "$API_BASE/api/register",      // <--- was "/api/register"
                            RequestInit(
                                method = "POST",
                                headers = Headers().also {
                                    it.append("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                                },
                                body = form
                            )
                        ).await()

                        val text = resp.text().await()

                        when (resp.status.toInt()) {
                            201 -> {
                                success = "Registered successfully. $text"
                                // Optional: navigate to login after a short delay
                                window.setTimeout({
                                    window.location.hash = "#/login"
                                }, 800)
                                // Clear sensitive fields
                                password = ""
                                confirm = ""
                            }
                            409 -> error = "Username already exists."
                            400 -> error = if (text.isNotBlank()) text else "Invalid request."
                            else -> error = "Registration failed (${resp.status}). ${if (text.isNotBlank()) text else ""}"
                        }
                    } catch (t: Throwable) {
                        error = "Network or server error: ${t.message ?: t.toString()}"
                    } finally {
                        busy = false
                    }
                }
            }
        }) {
            if (busy) Text("Creating…") else Text("Create account")
        }

        P({ classes("muted") }) {
            Text("Already have an account? ")
            A("#/login") { Text("Log in") }
        }
    }
}
