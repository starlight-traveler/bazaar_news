package app.pages

import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.svg.*
import org.jetbrains.compose.web.attributes.*
import app.Post
import app.getPosts
import kotlinx.browser.window
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.w3c.dom.events.Event
import kotlin.math.pow
import app.getCommentsForPost
import app.getUpvoteCount




suspend fun normalizeTraction(posts: List<Post>): List<Post> {
    if (posts.isEmpty()) return posts
    val postsTraction = posts.map { post ->
        val commentCount = try {
            getCommentsForPost(post.id).size
        } catch (e: Exception) {
            console.error("Error fetching comments for post ${post.id}: $e")
            0
        }

        val upvoteCount = try {
            getUpvoteCount(post.id)
        } catch (e: Exception) {
            console.error("Error fetching upvotes for post ${post.id}: $e")
            0
        }

        // Calculate traction: combine comments and upvotes
        val traction = (commentCount + upvoteCount).toDouble()
        console.log(traction)
        post.copy(traction = traction)
    }
    val minT = postsTraction.minOf { it.traction }
    val maxT = postsTraction.maxOf { it.traction }
    val range = (maxT - minT).let { if (it == 0.0) 1.0 else it }

    // Initial placement based on traction
    val positioned = postsTraction.mapIndexed { index, p ->
        val normalized = (p.traction - minT) / range
        val spread = 400.0 * (1 - normalized).pow(1.5)

        // Use index-based offset instead of random
        val angle = (index * 2.4) // Golden angle in radians
        val offsetX = spread * kotlin.math.cos(angle)
        val offsetY = spread * kotlin.math.sin(angle)

        // Assign color based on traction (higher traction = more intense color)
        // normalized is 0.0 (low traction) to 1.0 (high traction)
        val colorIntensity = (normalized * 255).toInt()
        // Yellow (255, 255, 0) for high traction -> Orange (255, 165, 0) for low traction
        val red = 255
        val green = (165 + (normalized * 90)).toInt()  // 165 -> 255
        val blue = 0
        val color = "rgb($red, $green, $blue)"
        val radius = 50.0 + (normalized * 25.0)
        console.log("Radius is $radius")
        p.copy(
            x = 2000 + offsetX,
            y = 2000 + offsetY,
            color = color,  // Assuming Post has a color field
            radius = radius
        )
    }.toMutableList()

    // Separate overlapping posts (minimum 300px apart)
    val minDistance = 300.0
    repeat(50) { // Iterate to resolve overlaps
        for (i in positioned.indices) {
            for (j in i + 1 until positioned.size) {
                val p1 = positioned[i]
                val p2 = positioned[j]

                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                if (dist < minDistance && dist > 0) {
                    val pushDist = (minDistance - dist) / 2
                    val angle = kotlin.math.atan2(dy, dx)

                    positioned[i] = p1.copy(
                        x = p1.x - pushDist * kotlin.math.cos(angle),
                        y = p1.y - pushDist * kotlin.math.sin(angle)
                    )
                    positioned[j] = p2.copy(
                        x = p2.x + pushDist * kotlin.math.cos(angle),
                        y = p2.y + pushDist * kotlin.math.sin(angle)
                    )
                }
            }
        }
    }

    positioned.forEach { p ->
        console.log("rendering posts on map y:${p.y} x:${p.x} (traction=${p.traction}, color=${p.color})")
    }

    return positioned
}

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun MapPage() {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Panning and zoom state
    var offset by remember { mutableStateOf(0.0 to 0.0) }
    var dragging by remember { mutableStateOf(false) }
    var lastMouse by remember { mutableStateOf(0.0 to 0.0) }
    var scale by remember { mutableStateOf(1.0) }
    var isDraggingPost by remember { mutableStateOf(false) }

    // Global mouse event handlers
    DisposableEffect(dragging) {
        val handleMouseMove: (Event) -> Unit = { event ->
            if (dragging && !isDraggingPost) {
                val mouseEvent = event.unsafeCast<org.w3c.dom.events.MouseEvent>()
                val dx = mouseEvent.clientX.toDouble() - lastMouse.first
                val dy = mouseEvent.clientY.toDouble() - lastMouse.second
                offset = (offset.first + dx) to (offset.second + dy)
                lastMouse = mouseEvent.clientX.toDouble() to mouseEvent.clientY.toDouble()
            }
        }

        val handleMouseUp: (Event) -> Unit = { _ ->
            dragging = false
            isDraggingPost = false
        }

        if (dragging) {
            window.addEventListener("mousemove", handleMouseMove)
            window.addEventListener("mouseup", handleMouseUp)
        }

        onDispose {
            window.removeEventListener("mousemove", handleMouseMove)
            window.removeEventListener("mouseup", handleMouseUp)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val fetched = getPosts()
            console.log("Fetched ${fetched.size} posts: $fetched")
            posts = normalizeTraction(fetched)
            console.log("Normalized posts: ${posts.size}")
        } catch (t: Throwable) {
            error = t.message ?: "Unknown error"
            console.log("Error fetching posts: ${t.message}")
        } finally {
            loading = false
        }
    }

    // Center the map on initial load
    LaunchedEffect(Unit) {
        val viewportWidth = window.innerWidth.toDouble()
        val viewportHeight = window.innerHeight.toDouble() - 60.0 // Subtract navbar height

        // Calculate offset to center point (2000, 2000) in viewport
        offset = (viewportWidth / 2 - 2000) to (viewportHeight / 2 - 2000)
    }


    if (loading) {
        P { Text("Loading…") }
    } else if (error != null) {
        Div({ style { color(Color.red) } }) { Text("Error: $error") }
    } else {
        // Map viewport
        Div({
            style {
                position(Position.Fixed)
                top(41.33.px)
                left(0.px)
                width(100.vw)
                property("height", "calc(100vh - 60px)")  // Subtract navbar height
                overflow("hidden")
                cursor(if (dragging) "grabbing" else "grab")
                property("user-select", "none")
            }
            onMouseDown { event ->
                event.preventDefault()
                dragging = true
                isDraggingPost = false
                lastMouse = event.clientX.toDouble() to event.clientY.toDouble()
            }
            onWheel { event ->
                event.preventDefault()
                val delta = event.deltaY * -0.001
                scale = (scale + delta).coerceIn(0.5, 2.0)
            }
        }) {
            // Map container
            Div({
                style {
                    width(4000.px)
                    height(4000.px)
                    position(Position.Absolute)
                    property("transform", "translate(${offset.first}px, ${offset.second}px) scale($scale)")
                    property("transform-origin", "0 0")
                    property("will-change", "transform")
                    property("pointer-events", "none")
                }
            }) {
                // Draw connections first
                Svg(attrs = {
                    attr("width", "4000")
                    attr("height", "4000")
                    style {
                        position(Position.Absolute)
                        left(0.px)
                        top(0.px)
                        property("pointer-events", "none")
                    }
                }) {
                    posts.forEachIndexed { i, p1 ->
                        posts.drop(i + 1).forEach { p2 ->
                            val dx = p1.x - p2.x
                            val dy = p1.y - p2.y
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                            if (dist < 450) {
                                Line(
                                    x1 = p1.x,
                                    y1 = p1.y,
                                    x2 = p2.x,
                                    y2 = p2.y,
                                    attrs = {
                                        attr("stroke", "rgba(0,0,0,0.3)")
                                        attr("stroke-width", "1")
                                    }
                                )
                            }
                        }
                    }
                }

                // Then draw posts on top
                posts.forEach { post ->
                    A("#/post/${post.id}") {
                        Div({
                            style {
                                position(Position.Absolute)
                                left((post.x - post.radius).px)
                                top((post.y - post.radius).px)
                                width((post.radius * 2).px)
                                height((post.radius * 2).px)
                                backgroundColor(Color(post.color))
                                //border(1.px, LineStyle.Solid, Color.black)
                                property("box-shadow", "0 6px 10px rgba(0, 0, 0, 0.15), 0 3px 6px rgba(0, 0, 0, 0.1)")
                                borderRadius(50.percent)
                                padding(8.px)
                                cursor("pointer")
                                property("pointer-events", "auto")
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                justifyContent(JustifyContent.Center)
                                alignItems(AlignItems.Center)
                                property("text-align", "center")
                                overflow("hidden")
                            }
                        }) {
                            P({ style {
                                margin(0.px)
                                property("word-break", "break-word")
                                property("overflow", "hidden")
                                property("text-overflow", "ellipsis")
                                property("display", "-webkit-box")
                                property("-webkit-line-clamp", "4")
                                property("-webkit-box-orient", "vertical")
                                fontSize(12.px)
                            } }) {
                                Text(post.title)
                            }
                        }
                    }
                }
            }
        }
    }
}



