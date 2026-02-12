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
 * Build configuration for cheap-rag module.
 * This module provides Java utilities for cheap-rag, including a comprehensive Java extractor.
 */

plugins {
    `java-library`
    application
    idea
    id("io.freefair.lombok") version "8.14.2"
}

group = "net.netbeing"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    // JavaParser for Java code parsing
    implementation("com.github.javaparser:javaparser-core:3.26.2")

    // Picocli for CLI
    implementation("info.picocli:picocli:4.7.6")

    // Jackson for JSON output
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)

    // Logging
    implementation(libs.slf4j)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)

    // Guava
    implementation(libs.guava)

    // JetBrains annotations
    compileOnly(libs.jetbrains.annotations)

    // Testing
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    modularity.inferModulePath = true
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

application {
    mainClass = "net.netbeing.cheap.rag.extractor.ExtractorCli"
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

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "net.netbeing.cheap.rag.extractor.ExtractorCli"
    }
    // Create fat JAR
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked"))
    }
}
