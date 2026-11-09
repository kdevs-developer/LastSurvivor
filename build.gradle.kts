plugins {
    // AGP 9.0 requires Gradle 9.1.0 and ships Kotlin built in,
    // so org.jetbrains.kotlin.android is no longer applied.
    id("com.android.application") version "9.0.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
