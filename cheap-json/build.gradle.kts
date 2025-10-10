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
 * Build configuration for cheap-json module.
 * This module contains JSON schemas and utilities for the Cheap data model.
 */

plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    idea
    id("io.freefair.lombok") version "8.14.2"
}

group = "net.netbeing"
version = "0.1"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api(project(":cheap-core"))

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation(libs.guava)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)

    compileOnly(libs.jetbrains.annotations)

    testImplementation(libs.junit.jupiter)
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
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked"))
    }
}