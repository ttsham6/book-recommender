plugins {
    java
    id("org.springframework.boot") version "3.5.15" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.diffplug.spotless") version "8.7.0" apply false
}

group = "com.ttsham6"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

subprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "checkstyle")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()
        }
    }

    configure<org.gradle.api.plugins.quality.CheckstyleExtension> {
        toolVersion = "10.14.0"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = true
        configProperties = mapOf(
            "org.checkstyle.google.severity" to "warning",
            "org.checkstyle.google.suppressionfilter.config" to rootProject.file("config/checkstyle/checkstyle-suppressions.xml").absolutePath,
            "org.checkstyle.google.suppressionxpathfilter.config" to rootProject.file("config/checkstyle/checkstyle-xpath-suppressions.xml").absolutePath
        )
    }
}
