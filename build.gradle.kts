import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

/**
 * Renders the most recent CHANGELOG.md section as the plugin's change notes.
 *
 * Reads everything between the first `## [x.y.z]` heading and the next `##` heading,
 * turning top-level `- ` bullets into an HTML list. Change notes must be HTML, and the
 * Marketplace rejects unescaped markup, so text is escaped. Returns an empty string if
 * the file or section is missing, which is valid -- change notes are optional.
 */
fun latestChangeNotes(): String {
    val changelog = file("CHANGELOG.md")
    if (!changelog.exists()) return ""

    val lines = changelog.readLines()
    val start = lines.indexOfFirst { it.startsWith("## [") }
    if (start < 0) return ""
    val rest = lines.drop(start + 1)
    val relativeEnd = rest.indexOfFirst { it.startsWith("## ") }
    val body = if (relativeEnd < 0) rest else rest.take(relativeEnd)

    fun String.escapeHtml() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    // Strip the ** markers Keep a Changelog uses for emphasis; they are not HTML.
    fun String.stripEmphasis() = replace("**", "")

    val items = body
        .map { it.trim() }
        .filter { it.startsWith("- ") }
        .map { "<li>${it.removePrefix("- ").stripEmphasis().escapeHtml()}</li>" }

    return if (items.isEmpty()) "" else items.joinToString("", "<ul>", "</ul>")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())
        bundledPlugin("org.intellij.plugins.markdown")
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        // "What's new" on the Marketplace, taken from the top section of CHANGELOG.md so
        // release notes are not maintained in two places.
        changeNotes = latestChangeNotes()
        ideaVersion {
            sinceBuild = "233"
            untilBuild = provider { null }
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            // Check if specific IDE is requested via command line for matrix builds
            val ideType = providers.gradleProperty("plugin.verifier.ide.type").orNull
            val ideVersion = providers.gradleProperty("plugin.verifier.ide.version").orNull

            if (ideType != null && ideVersion != null) {
                // Single IDE - used by the sharded CI matrix to bound runner disk usage
                create(IntelliJPlatformType.fromCode(ideType, ideVersion), ideVersion)
            } else {
                // Full local suite. Resolved dynamically rather than hardcoded, so the
                // verified range tracks new IDE releases instead of silently drifting
                // out of date the way the previous fixed 2023.3-2024.3 list did.
                //
                // IntelliJ IDEA Community stopped shipping as a separate product after
                // 2025.2, so later branches are covered by the unified IntelliJ IDEA type.
                select {
                    types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
                    channels = listOf(ProductRelease.Channel.RELEASE)
                    sinceBuild = "233"
                    untilBuild = "252.*"
                }
                select {
                    types = listOf(IntelliJPlatformType.IntellijIdea)
                    channels = listOf(ProductRelease.Channel.RELEASE)
                    sinceBuild = "253"
                }

                // Cross-IDE spot checks against other JetBrains products.
                // PyCharm Community (PyCharmCommunity/PC) is deliberately absent: the
                // Gradle plugin cannot resolve an installer for it at any version, so it
                // fails before downloading. The unified PyCharm type resolves correctly.
                select {
                    types = listOf(
                        IntelliJPlatformType.PyCharm,
                        IntelliJPlatformType.WebStorm,
                    )
                    channels = listOf(ProductRelease.Channel.RELEASE)
                    sinceBuild = "243"
                }
            }
        }
    }
}

// Sandboxed IDE launchers for manual testing. `runIde` uses the compile target
// (2023.3); these run the plugin on newer IDEs, where the platform APIs it relies
// on have actually changed. Development only -- no effect on the published plugin.
intellijPlatformTesting {
    runIde {
        register("runIde2026") {
            type = IntelliJPlatformType.IntellijIdea
            version = "2026.2"
        }
        register("runIde2025") {
            type = IntelliJPlatformType.IntellijIdea
            version = "2025.3"
        }
    }
}

tasks {
    test {
        // Using JUnit 4 with IntelliJ Platform test framework
    }
}
