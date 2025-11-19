import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage
import com.bmuschko.gradle.docker.tasks.container.*
import com.bmuschko.gradle.docker.tasks.network.DockerCreateNetwork
import com.bmuschko.gradle.docker.tasks.network.DockerRemoveNetwork

plugins {
    java
    id("io.freefair.lombok") version "8.14.2"
    id("com.bmuschko.docker-remote-api") version "9.4.0"
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

    // Docker integration for tests
    integrationImplementation("com.github.docker-java:docker-java-core:3.3.4")
    integrationImplementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.4")
}

java {
    modularity.inferModulePath = true
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

// Create integration test task (embedded tests only, excludes Docker)
val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs embedded integration tests (excludes Docker)"
    group = "verification"

    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath

    useJUnitPlatform {
        excludeTags("docker")
    }

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

// ============================================================================
// Docker Integration Test Tasks
// ============================================================================

// Docker network for integration tests
val createDockerNetwork = tasks.register<DockerCreateNetwork>("createDockerNetwork") {
    networkName.set("cheap-integration-test-network")
}

val removeDockerNetwork = tasks.register<DockerRemoveNetwork>("removeDockerNetwork") {
    networkId.set("cheap-integration-test-network")
}

// Build cheap-rest Docker image
val buildCheapRestImage = tasks.register<DockerBuildImage>("buildCheapRestImage") {
    dependsOn(":cheap-rest:bootJar")
    inputDir.set(file("${project(":cheap-rest").projectDir}"))
    images.add("cheap-rest:latest")
    images.add("cheap-rest:${project.version}")
}

val removeCheapRestImage = tasks.register<DockerRemoveImage>("removeCheapRestImage") {
    targetImageId("cheap-rest:latest")
}

// PostgreSQL container tasks
val createPostgresContainer = tasks.register<DockerCreateContainer>("createPostgresContainer") {
    dependsOn(createDockerNetwork)
    containerName.set("cheap-postgres-test")
    imageId.set("postgres:17")
    envVars.set(mapOf(
        "POSTGRES_DB" to "cheap",
        "POSTGRES_USER" to "cheap_user",
        "POSTGRES_PASSWORD" to "test_password"
    ))
    hostConfig.portBindings.set(listOf("5432"))
    hostConfig.network.set("cheap-integration-test-network")
    healthCheck.cmd.set(listOf("CMD-SHELL", "pg_isready -U cheap_user -d cheap"))
    healthCheck.interval.set(5000000000) // 5 seconds in nanoseconds
    healthCheck.timeout.set(3000000000) // 3 seconds
    healthCheck.retries.set(5)
}

val startPostgresContainer = tasks.register<DockerStartContainer>("startPostgresContainer") {
    dependsOn(createPostgresContainer)
    containerId.set("cheap-postgres-test")
}

val stopPostgresContainer = tasks.register<DockerStopContainer>("stopPostgresContainer") {
    containerId.set("cheap-postgres-test")
}

val removePostgresContainer = tasks.register<DockerRemoveContainer>("removePostgresContainer") {
    dependsOn(stopPostgresContainer)
    containerId.set("cheap-postgres-test")
    force.set(true)
    removeVolumes.set(true)
}

// MariaDB container tasks
val createMariaDbContainer = tasks.register<DockerCreateContainer>("createMariaDbContainer") {
    dependsOn(createDockerNetwork)
    containerName.set("cheap-mariadb-test")
    imageId.set("mariadb:11")
    envVars.set(mapOf(
        "MARIADB_DATABASE" to "cheap",
        "MARIADB_USER" to "cheap_user",
        "MARIADB_PASSWORD" to "test_password",
        "MARIADB_ROOT_PASSWORD" to "root_password"
    ))
    hostConfig.portBindings.set(listOf("3306"))
    hostConfig.network.set("cheap-integration-test-network")
    healthCheck.cmd.set(listOf("CMD-SHELL", "healthcheck.sh --connect --innodb_initialized"))
    healthCheck.interval.set(5000000000)
    healthCheck.timeout.set(3000000000)
    healthCheck.retries.set(5)
}

val startMariaDbContainer = tasks.register<DockerStartContainer>("startMariaDbContainer") {
    dependsOn(createMariaDbContainer)
    containerId.set("cheap-mariadb-test")
}

val stopMariaDbContainer = tasks.register<DockerStopContainer>("stopMariaDbContainer") {
    containerId.set("cheap-mariadb-test")
}

val removeMariaDbContainer = tasks.register<DockerRemoveContainer>("removeMariaDbContainer") {
    dependsOn(stopMariaDbContainer)
    containerId.set("cheap-mariadb-test")
    force.set(true)
    removeVolumes.set(true)
}

// cheap-rest-postgres container tasks
val createCheapRestPostgresContainer = tasks.register<DockerCreateContainer>("createCheapRestPostgresContainer") {
    dependsOn(buildCheapRestImage, createDockerNetwork, startPostgresContainer)
    containerName.set("cheap-rest-postgres-test")
    imageId.set("cheap-rest:latest")
    envVars.set(mapOf(
        "SPRING_PROFILES_ACTIVE" to "postgres",
        "SPRING_DATASOURCE_URL" to "jdbc:postgresql://cheap-postgres-test:5432/cheap",
        "SPRING_DATASOURCE_USERNAME" to "cheap_user",
        "SPRING_DATASOURCE_PASSWORD" to "test_password"
    ))
    hostConfig.portBindings.set(listOf("8080"))
    hostConfig.network.set("cheap-integration-test-network")
}

val startCheapRestPostgresContainer = tasks.register<DockerStartContainer>("startCheapRestPostgresContainer") {
    dependsOn(createCheapRestPostgresContainer)
    containerId.set("cheap-rest-postgres-test")
}

val stopCheapRestPostgresContainer = tasks.register<DockerStopContainer>("stopCheapRestPostgresContainer") {
    containerId.set("cheap-rest-postgres-test")
}

val removeCheapRestPostgresContainer = tasks.register<DockerRemoveContainer>("removeCheapRestPostgresContainer") {
    dependsOn(stopCheapRestPostgresContainer)
    containerId.set("cheap-rest-postgres-test")
    force.set(true)
}

// cheap-rest-mariadb container tasks
val createCheapRestMariaDbContainer = tasks.register<DockerCreateContainer>("createCheapRestMariaDbContainer") {
    dependsOn(buildCheapRestImage, createDockerNetwork, startMariaDbContainer)
    containerName.set("cheap-rest-mariadb-test")
    imageId.set("cheap-rest:latest")
    envVars.set(mapOf(
        "SPRING_PROFILES_ACTIVE" to "mariadb",
        "SPRING_DATASOURCE_URL" to "jdbc:mariadb://cheap-mariadb-test:3306/cheap",
        "SPRING_DATASOURCE_USERNAME" to "cheap_user",
        "SPRING_DATASOURCE_PASSWORD" to "test_password"
    ))
    hostConfig.portBindings.set(listOf("8080"))
    hostConfig.network.set("cheap-integration-test-network")
}

val startCheapRestMariaDbContainer = tasks.register<DockerStartContainer>("startCheapRestMariaDbContainer") {
    dependsOn(createCheapRestMariaDbContainer)
    containerId.set("cheap-rest-mariadb-test")
}

val stopCheapRestMariaDbContainer = tasks.register<DockerStopContainer>("stopCheapRestMariaDbContainer") {
    containerId.set("cheap-rest-mariadb-test")
}

val removeCheapRestMariaDbContainer = tasks.register<DockerRemoveContainer>("removeCheapRestMariaDbContainer") {
    dependsOn(stopCheapRestMariaDbContainer)
    containerId.set("cheap-rest-mariadb-test")
    force.set(true)
}

// cheap-rest-sqlite container tasks
val createCheapRestSqliteContainer = tasks.register<DockerCreateContainer>("createCheapRestSqliteContainer") {
    dependsOn(buildCheapRestImage, createDockerNetwork)
    containerName.set("cheap-rest-sqlite-test")
    imageId.set("cheap-rest:latest")
    envVars.set(mapOf(
        "SPRING_PROFILES_ACTIVE" to "sqlite",
        "CHEAP_DB_PATH" to "/data/cheap.db"
    ))
    hostConfig.portBindings.set(listOf("8080"))
    hostConfig.network.set("cheap-integration-test-network")
    // Mount volume for SQLite database
    hostConfig.binds.set(mapOf("/tmp/cheap-sqlite-test" to "/data"))
}

val startCheapRestSqliteContainer = tasks.register<DockerStartContainer>("startCheapRestSqliteContainer") {
    dependsOn(createCheapRestSqliteContainer)
    containerId.set("cheap-rest-sqlite-test")
}

val stopCheapRestSqliteContainer = tasks.register<DockerStopContainer>("stopCheapRestSqliteContainer") {
    containerId.set("cheap-rest-sqlite-test")
}

val removeCheapRestSqliteContainer = tasks.register<DockerRemoveContainer>("removeCheapRestSqliteContainer") {
    dependsOn(stopCheapRestSqliteContainer)
    containerId.set("cheap-rest-sqlite-test")
    force.set(true)
}

// Orchestration tasks
val startDockerTestEnvironment = tasks.register("startDockerTestEnvironment") {
    description = "Start all Docker containers for integration testing"
    group = "verification"
    dependsOn(
        startPostgresContainer,
        startMariaDbContainer,
        startCheapRestPostgresContainer,
        startCheapRestMariaDbContainer,
        startCheapRestSqliteContainer
    )
}

val stopDockerTestEnvironment = tasks.register("stopDockerTestEnvironment") {
    description = "Stop all Docker containers for integration testing"
    group = "verification"
    dependsOn(
        removeCheapRestPostgresContainer,
        removeCheapRestMariaDbContainer,
        removeCheapRestSqliteContainer,
        removePostgresContainer,
        removeMariaDbContainer
    )
    finalizedBy(removeDockerNetwork)
}

// Docker integration test task
val dockerIntegrationTest = tasks.register<Test>("dockerIntegrationTest") {
    description = "Runs Docker-based integration tests"
    group = "verification"

    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath

    useJUnitPlatform {
        includeTags("docker")
    }

    // Force tests to run every time
    outputs.upToDateWhen { false }

    shouldRunAfter(integrationTest)
}

// All integration tests task
val allIntegrationTests = tasks.register("allIntegrationTests") {
    description = "Runs all integration tests (embedded and Docker)"
    group = "verification"
    dependsOn(integrationTest, dockerIntegrationTest)
}
