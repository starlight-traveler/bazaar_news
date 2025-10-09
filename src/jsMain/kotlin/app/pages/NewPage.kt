package app.pages

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

@Composable
fun NewPage() {
    // Backend should accept ?sort=new on /api/posts; we reuse TopPage if you prefer.
    P { Text("This page can reuse the TopPage with sort = 'new' or show a different view.") }
    P { A("#/top") { Text("Go to Top") } }
}
