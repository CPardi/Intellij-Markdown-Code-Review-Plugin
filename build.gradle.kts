import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

dependencies {
    // JUnit 5 for modern testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")

    // JUnit 4 - Required workaround for IJPL-159134 (JUnit5 Test Framework refers to JUnit4)
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")

    // Required for JUnit 5 assertions - fix for IJPL-157292 (Missing opentest4j dependency)
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.3.4")
        testFramework(TestFrameworkType.Platform)
    }
}

tasks {
    test {
        useJUnitPlatform {}

        // Configure test logging
        testLogging {
            events("passed", "skipped", "failed")
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }

        // Generate HTML report
        reports {
            html.required.set(true)
            junitXml.required.set(true)
        }

        // Set JVM arguments for test execution
        jvmArgs = listOf(
            "-Xmx1g",
            "-XX:+HeapDumpOnOutOfMemoryError"
        )
    }
}
