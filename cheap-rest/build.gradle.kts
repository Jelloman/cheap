/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Build configuration for cheap-rest module.
 * This module provides a Spring Boot REST API for the Cheap data model.
 */

plugins {
    `java-library`
    idea
    id("io.freefair.lombok") version "8.14.2"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "net.netbeing"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    // Cheap modules
    api(project(":cheap-core"))
    api(project(":cheap-json"))

    // Database modules - all included, selection via Spring profiles at runtime
    implementation(project(":cheap-db-postgres"))
    implementation(project(":cheap-db-sqlite"))
    implementation(project(":cheap-db-mariadb"))

    // Spring Boot starters
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)

    // OpenAPI/Swagger documentation
    implementation(libs.openapi.starter.ui)

    // Logging
    implementation(libs.slf4j)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)

    // Guava (needed by cheap-core)
    implementation(libs.guava)

    // JetBrains annotations
    compileOnly(libs.jetbrains.annotations)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.sqlite.jdbc) // For SQLite-based service tests
    testImplementation(libs.embedded.postgres)
    testImplementation(libs.mariaDB4j)
    testImplementation(libs.junit.platform.reporting)

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.platform.engine)

    constraints {
        implementation(libs.json.smart) {
            because("CVE-2024-57699")
        }
        implementation(libs.commons.lang3) {
            because("CVE-2025-48924")
        }
        implementation(libs.tomcat.embed.core) {
            because("CVE-2025-41242")
        }
        testImplementation(libs.junit.platform.commons) {
            because("JUnit Platform version alignment")
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    modularity.inferModulePath = true
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Disable module path for tests to avoid JUnit Platform classpath issues
    //modularity.inferModulePath.set(false)

    // Enable native access for SQLite JDBC driver in Java 24+
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked"))
    }
}
