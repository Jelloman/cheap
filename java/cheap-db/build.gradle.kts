import org.gradle.internal.impldep.org.apache.maven.model.Build

plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    id("io.freefair.lombok") version "8.14.2"
}

group = "net.netbeing"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api(project(":cheap-core"))

    implementation(libs.sqlite.jdbc)
    implementation(libs.postgresql)

    compileOnly(libs.jetbrains.annotations)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.embedded.postgres)
    testImplementation(libs.flyway)
    testImplementation(libs.flyway.pg)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    modularity.inferModulePath = true
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}
tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED"
    )
}
gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked"))
        //options.compilerArgs.addAll(listOf("-Xlint:unchecked", "--sun-misc-unsafe-memory-access=allow"))
    }
}
