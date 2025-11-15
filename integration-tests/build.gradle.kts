plugins {
    java
    id("io.freefair.lombok") version "8.14.2"
}

group = "net.netbeing"
version = "0.1"

repositories {
    mavenCentral()
}

sourceSets {
    create("integration") {
        java.srcDir("src/integration/java")
        resources.srcDir("src/integration/resources")
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

// Configure dependencies for integration source set
val integrationImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val integrationRuntimeOnly by configurations.getting

dependencies {
    // Depend on all other modules for integration tests
    integrationImplementation(project(":cheap-core"))
    integrationImplementation(project(":cheap-json"))
    integrationImplementation(project(":cheap-db-postgres"))
    integrationImplementation(project(":cheap-db-sqlite"))
    integrationImplementation(project(":cheap-db-mariadb"))
    integrationImplementation(project(":cheap-rest"))
    integrationImplementation(project(":cheap-rest-client"))

    // Spring Boot dependencies
    integrationImplementation(libs.spring.boot.starter.test)
    integrationImplementation(libs.spring.boot.starter.webflux)

    // Jackson for JSON processing
    integrationImplementation(libs.jackson.databind)

    // Database drivers and embedded databases
    integrationImplementation(libs.postgresql)
    integrationImplementation(libs.sqlite.jdbc)
    integrationImplementation(libs.mariaDB)
    integrationImplementation(libs.embedded.postgres)
    integrationImplementation(libs.mariaDB4j)

    // Test dependencies for integration tests
    integrationImplementation(libs.junit.jupiter)
    integrationRuntimeOnly(libs.junit.platform.launcher)
    integrationRuntimeOnly(libs.junit.jupiter)
}

java {
    modularity.inferModulePath = true
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

// Create integration test task
val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests"
    group = "verification"

    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath

    useJUnitPlatform()

    shouldRunAfter(tasks.test)

    // Force integration tests to run every time (disable up-to-date checking)
    outputs.upToDateWhen { false }
}

// Make check depend on integration tests
tasks.named("check") {
    dependsOn(integrationTest)
}

gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked"))
    }
}

// Configure duplicate handling for resource processing
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
