import org.commonmark.parser.Parser
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

buildscript {
    dependencies {
        classpath("org.commonmark:commonmark:0.30.0")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    // JUnit 5 for modern testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.14.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")

    // JUnit 4 - Required workaround for IJPL-159134 (JUnit5 Test Framework refers to JUnit4)
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.14.4")

    // Required for JUnit 5 assertions - fix for IJPL-157292 (Missing opentest4j dependency)
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.3.4")
        testFramework(TestFrameworkType.Platform)
    }
}

kover {
    // Configure coverage reports - generates both HTML and XML reports by default
    // Run: ./gradlew koverHtmlReport for HTML report
    // Run: ./gradlew koverXmlReport for XML report (for CI integration)
    // Run: ./gradlew koverVerify to verify coverage thresholds
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

    patchPluginXml {
        sinceBuild = providers.gradleProperty("pluginSinceBuild")
        untilBuild = null

        pluginDescription = provider {
            val descriptionFile = layout.projectDirectory.file("DESCRIPTION.md").asFile
            val markdown = descriptionFile.readText()

            val parser = Parser.builder().build()
            val document = parser.parse(markdown)
            val renderer = org.commonmark.renderer.html.HtmlRenderer.builder().build()

            renderer.render(document)
        }
    }
}
