package app.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.Post
import app.util.ReversibleUserId32
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

/**
 * Install once (e.g., inside your App()) to register the styles.
 */
object PostRowTheme : StyleSheet() {

    // --- Motion keyframes (string-based for compat) ---
    @OptIn(ExperimentalComposeWebApi::class)
    private val appearKeyframes by keyframes {
        from {
            property("opacity", "0")
            property("transform", "translateY(6px) scale(0.992)")
        }
        to {
            property("opacity", "1")
            property("transform", "translateY(0px) scale(1)")
        }
    }

    // --- Vars (hooks into your scheme if you already define CSS variables) ---
    private const val C_BG = "var(--card-bg, #0f1115)"
    private const val C_BORDER = "var(--card-border, rgba(255,255,255,.08))"
    private const val C_TEXT = "var(--text, #e6e6e6)"
    private const val C_MUTED = "var(--muted, #9aa0a6)"
    private const val C_ACCENT = "var(--accent, #8ab4f8)"
    private const val C_ACCENT_SOFT = "var(--accent-soft, rgba(138,180,248,.15))"

    // --- Enhanced “card” look that layers onto your existing .card class ---
    val cardEnhanced by style {
        property("will-change", "transform, box-shadow, border-color, background-color")
        property("background-color", C_BG)
        border { width(1.px); style(LineStyle.Solid); color(Color("transparent")) }
        property("border-color", C_BORDER)
        borderRadius(16.px)
        padding(14.px)
        property("backdrop-filter", "saturate(120%) blur(4px)")
        property("box-shadow", "0px 1px 12px rgba(0,0,0,0.35)")
        property(
            "transition",
            "transform 180ms cubic-bezier(.2,.8,.2,1), " +
                    "box-shadow 180ms cubic-bezier(.2,.8,.2,1), " +
                    "border-color 180ms ease"
        )

        // Entry animation (unconditional for widest DSL compatibility)
        property("animation", "${appearKeyframes.name} 260ms cubic-bezier(.2,.8,.2,1) both")

        self + hover style {
            property("transform", "translateY(-2px)")
            property("box-shadow", "0px 6px 22px rgba(0,0,0,0.45)")
            property("border-color", "color-mix(in oklab, $C_BORDER, $C_ACCENT 16%)")
        }

        self + focus style {
            property("outline", "2px solid $C_ACCENT")
            property("outline-offset", "2px")
        }
    }

    // Row system that feels tighter and consistent
    val rowTight by style {
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
        gap(10.px)
        paddingTop(4.px)
        paddingBottom(4.px)
    }

    // Title link style—keeps your anchor but ensures overflow is graceful
    val titleLink by style {
        fontWeight(600)
        property("color", C_TEXT)
        property("text-decoration", "none")
        property("transition", "opacity 140ms ease")
        property("text-wrap", "balance")
        property("overflow-wrap", "anywhere")
        self + hover style { opacity(0.86) }
        self + focus style {
            property("outline", "2px solid $C_ACCENT")
            property("outline-offset", "2px")
            borderRadius(8.px)
        }
    }

    // Index badge (when present)
    val indexBadge by style {
        width(28.px)
        textAlign("right")
        marginRight(4.px)
        property("color", C_MUTED)
        fontSize(12.px)
        property("letter-spacing", ".02em")
        property("user-select", "none")
    }

    // Spacer (use flex shorthand for compatibility)
    val flexGrow by style {
        property("flex", "1 1 auto")
    }

    // Content preview: 2-line clamp, nicer rhythm
    val previewText by style {
        property("color", C_MUTED)
        fontSize(14.px)
        property("line-height", "1.35")
        marginTop(2.px)
        property("display", "-webkit-box")
        property("-webkit-line-clamp", "2")
        property("-webkit-box-orient", "vertical")
        property("overflow", "hidden")
    }

    // Meta line style
    val metaLine by style {
        property("color", C_MUTED)
        fontSize(12.px)
        gap(8.px)
        display(DisplayStyle.Flex)
        alignItems(AlignItems.Center)
    }

    // Small dot divider for meta
    val metaDot by style {
        width(3.px)
        height(3.px)
        borderRadius(999.px)
        property("background-color", C_MUTED)
        opacity(0.6)
    }

    // Accent capsule for the inline UpvoteButton parent (keeps your same classes)
    val upvotePill by style {
        borderRadius(999.px)
        paddingLeft(6.px)
        paddingRight(6.px)
        paddingTop(2.px)
        paddingBottom(2.px)
        property("background-color", "transparent")
        property("transition", "background-color 120ms ease")
        self + hover style {
            property("background-color", C_ACCENT_SOFT)
        }
        self + focus style {
            property("outline", "2px solid $C_ACCENT")
            property("outline-offset", "2px")
        }
    }

    // Optional subtle divider between rows (uses border-image to avoid heavy lines)
    val rowDivider by style {
        property("border-top", "1px solid transparent")
        property(
            "border-image",
            "linear-gradient(to right, transparent, rgba(255,255,255,.08), transparent) 1"
        )
        marginTop(6.px)
        paddingTop(6.px)
    }

    @Composable
    fun Install() {
        Style(this)
    }
}

@Composable
fun PostRowTheme.Install() = PostRowTheme.Install()

@Composable
fun PostRow(post: Post, index: Int? = null) {
    // Root card with enhanced style layered on top of your existing "card" class
    Div({
        classes("card")
        classes(PostRowTheme.cardEnhanced)
        attr("data-post-id", post.id.toString())
    }) {

        // ROW 1: title (+ optional index) + inline compact upvote
        Div({
            classes("row")
            classes(PostRowTheme.rowTight)
        }) {
            if (index != null) {
                Span({ classes(PostRowTheme.indexBadge) }) { Text("$index.") }
            }

            A("#/post/${post.id}", {
                classes(PostRowTheme.titleLink)
            }) {
                B { Text(post.title) }
            }

            // fill remaining space then put the compact upvote on the right
            Span({ classes(PostRowTheme.flexGrow) }) {}

            Div({
                classes(PostRowTheme.upvotePill)
            }) {
                UpvoteButton(
                    postId = post.id.toString(),
                    compact = true,
                    attrs = { classes("btn", "btn-ghost", "btn-compact") }
                )
            }
        }

        // ROW 2: content (snippet)
        Div({
            classes("row")
            classes(PostRowTheme.rowTight)
        }) {
            P({
                classes("small")
                classes(PostRowTheme.previewText)
            }) {
                val preview = remember(post.content) {
                    if (post.content.length > 160)
                        post.content.take(160) + "…"
                    else post.content
                }
                Text(preview)
            }
        }

        // ROW divider (light)
        Div({ classes(PostRowTheme.rowDivider) }) {}

        // ROW 3: meta info (author + date)
        Div({
            classes("row")
            classes(PostRowTheme.rowTight)
        }) {
            // Left-side meta block
            Div({ classes(PostRowTheme.metaLine) }) {
                Span({ classes("small", "muted") }) {
                    Text("Author ")
                    B { Text(ReversibleUserId32.decode(post.authorId)) }
                }

                // dot separator
                Div({ classes(PostRowTheme.metaDot) }) {}

                Span({ classes("small", "muted") }) {
                    Text("Posted ${post.createdAt}")
                }
            }

            // Right spacer to keep layout stable if you later re-enable a full-size upvote
            Div({ classes(PostRowTheme.flexGrow) }) {}

            // If you later want a big upvote here, uncomment:
            // UpvoteButton(
            //   postId = post.id.toString(),
            //   compact = false,
            //   attrs = { classes("btn", "btn-primary") }
            // )
        }
    }
}
