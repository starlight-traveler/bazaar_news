plugins {
    kotlin("multiplatform") version "2.0.20"
    id("org.jetbrains.compose") version "1.7.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                // Compose runtime
                implementation(compose.runtime)

                // Compose for Web (HTML DOM + SVG)
                implementation(compose.html.core)
                implementation(compose.html.svg)

                // ✅ Add this to pull in org.jetbrains.compose.web.events.onInput and related event APIs
                implementation(compose.html.core)
                implementation(compose.web.core)

                // MPP libs you’re already using
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

            }
        }
    }
}
