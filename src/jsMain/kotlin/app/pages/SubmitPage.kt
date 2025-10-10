package app.pages

import androidx.compose.runtime.*
import app.createPost
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
    var content by remember { mutableStateOf("") }
    var authorId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Div({ classes("card", "stack") }) {
        H2 { Text("Create a New Post") }

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

        // Author ID (temporary until login)
        Input(InputType.Number, attrs = {
            classes("input")
            placeholder("Author ID (temporary)")
            value(authorId)
            addEventListener("input") { e ->
                val t = e.target as? HTMLInputElement
                authorId = t?.value.orEmpty()
            }
        })

        // Submit button
        Button(attrs = {
            classes("btn")
            if (submitting) attr("disabled", "true")
            addEventListener("click") {
                if (title.isBlank() || content.isBlank() || authorId.isBlank()) {
                    status = "Please fill in all fields"
                    return@addEventListener
                }
                scope.launch {
                    submitting = true
                    status = null
                    try {
                        val response = createPost(
                            title = title,
                            content = content,
                            authorId = authorId.toInt()
                        )
                        status = "Post submitted successfully!"
                        title = ""
                        content = ""
                        authorId = ""
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
