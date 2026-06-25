plugins {
    java
    `java-library`
    id("org.springframework.boot") version "3.5.15"
    id("io.spring.dependency-management") version "1.1.7"
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
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.ai:spring-ai-transformers-spring-boot-starter:1.0.0-M6")

    api(platform("software.amazon.awssdk:bom:2.46.17"))
    api("software.amazon.awssdk:dynamodb-enhanced:2.46.17")
    api("software.amazon.awssdk:s3")
    api("software.amazon.awssdk:s3-transfer-manager")

    api("io.pinecone:pinecone-client:5.1.0:all") {
        isTransitive = false
    }
    api("io.grpc:grpc-api:1.82.1")

    // Local Inference (LangChain4j)
    api("dev.langchain4j:langchain4j-core:0.36.2")
    api("dev.langchain4j:langchain4j-embeddings:0.36.2")
    // For ONNX support
    api("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:0.36.2")
    api("com.microsoft.onnxruntime:onnxruntime-extensions:0.13.0")

    api("org.junit.platform:junit-platform-launcher")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group.startsWith("ai.djl")) {
            useVersion("0.29.0")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "shared-local")
}

tasks.named("bootJar") {
    enabled = false
}

tasks.named("jar") {
    enabled = true
}
