plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    id("io.freefair.lombok") version "8.14.2"
    idea

}

group = "net.netbeing"
version = "0.1"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(libs.guava)
    implementation(libs.slf4j)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)

    compileOnly(libs.jetbrains.annotations)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.tempus.fugit)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter)
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
        //options.compilerArgs.addAll(listOf("-Xlint:unchecked", "--sun-misc-unsafe-memory-access=allow"))
    }
}
