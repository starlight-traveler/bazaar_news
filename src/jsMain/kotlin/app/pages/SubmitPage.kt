package app.pages

import androidx.compose.runtime.*
import app.NewPostRequest
import app.submitPost
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.value
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

@Composable
fun SubmitPage() {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Div({ classes("card", "stack") }) {
        H2 { Text("Submit") }

        // Title
        Input(InputType.Text, attrs = {
            classes("input")
            placeholder("Title")
            value(title)
            addEventListener("input") { e ->
                val t = e.target as? HTMLInputElement
                title = t?.value.orEmpty()
            }
        })

        // URL (optional)
        Input(InputType.Url, attrs = {
            classes("input")
            placeholder("URL (optional if you write text)")
            value(url)
            addEventListener("input") { e ->
                val t = e.target as? HTMLInputElement
                url = t?.value.orEmpty()
            }
        })

        // Text (optional)
        TextArea(attrs = {
            classes("textarea")
            placeholder("Text (optional; Markdown allowed if backend supports)")
            value(text)
            addEventListener("input") { e ->
                val t = e.target as? HTMLTextAreaElement
                text = t?.value.orEmpty()
            }
        })

        // Submit button
        Button(attrs = {
            classes("btn")
            if (submitting) attr("disabled", "true")
            addEventListener("click") {
                if (title.isBlank()) {
                    status = "Title is required"
                    return@addEventListener
                }
                scope.launch {
                    submitting = true
                    status = null
                    try {
                        submitPost(
                            NewPostRequest(
                                title = title,
                                url = url.ifBlank { null },
                                text = text.ifBlank { null }
                            )
                        )
                        status = "Submitted!"
                    } catch (t: Throwable) {
                        status = "Error: ${t.message}"
                    } finally {
                        submitting = false
                    }
                }
            }
        }) { Text(if (submitting) "Submitting…" else "Submit") }

        if (status != null) {
            P { Text(status!!) }
        }
    }
}
