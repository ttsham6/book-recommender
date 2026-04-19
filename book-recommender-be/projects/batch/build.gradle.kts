plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
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

dependencies {
    implementation(project(":projects:shared"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group.startsWith("ai.djl")) {
            useVersion("0.29.0")
        }
    }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("ai.djl.huggingface.tokenizers.flavor", "")
    systemProperty("ai.djl.repository.skip.check", "true")
}

tasks.named<Jar>("jar") {
    enabled = false
}
