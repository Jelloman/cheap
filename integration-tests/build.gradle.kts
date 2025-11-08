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
