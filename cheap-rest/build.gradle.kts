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
    id("org.springframework.boot") version "3.4.1"
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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Logging
    implementation(libs.slf4j)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)

    // Guava (needed by cheap-core)
    implementation(libs.guava)

    // JetBrains annotations
    compileOnly(libs.jetbrains.annotations)

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.embedded.postgres)
    testImplementation(libs.mariaDB4j)
    testRuntimeOnly(libs.junit.platform.launcher)
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
}

gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked"))
    }
}
