import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

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
                // Single IDE verification for matrix builds
                when (ideType) {
                    "IC" -> ide(IntelliJPlatformType.IntellijIdeaCommunity, ideVersion)
                    "PC" -> ide(IntelliJPlatformType.PyCharmCommunity, ideVersion)
                    "WS" -> ide(IntelliJPlatformType.WebStorm, ideVersion)
                }
            } else {
                // Full verification suite
                ide(IntelliJPlatformType.IntellijIdeaCommunity, "2023.3.8")    // 233 - minimum supported
                ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1.7")    // 241
                ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2.4")    // 242
                ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3.1")    // 243 - latest stable

                // Also verify with other JetBrains IDEs to ensure cross-IDE compatibility
                ide(IntelliJPlatformType.PyCharmCommunity, "2024.3.1")
                ide(IntelliJPlatformType.WebStorm, "2024.3.1")
            }
        }
    }
}

tasks {
    test {
        // Using JUnit 4 with IntelliJ Platform test framework
    }
}
