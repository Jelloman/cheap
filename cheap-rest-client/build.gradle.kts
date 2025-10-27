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
 * Build configuration for cheap-rest-client module.
 * This module provides a REST client for the cheap-rest API.
 */

plugins {
    `java-library`
    idea
    id("io.freefair.lombok") version "8.14.2"
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

    // Spring WebClient (reactive, non-blocking HTTP client)
    implementation(libs.spring.boot.starter.webflux)

    // Logging
    implementation(libs.slf4j)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)

    // Guava
    implementation(libs.guava)

    // JetBrains annotations
    compileOnly(libs.jetbrains.annotations)

    // Testing (unit tests only - integration tests will be in separate module)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.boot.starter.test)

    testRuntimeOnly(libs.junit.platform.launcher)
}

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
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked"))
    }
}
