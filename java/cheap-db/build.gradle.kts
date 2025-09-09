plugins {
    id("java")
    id("io.freefair.lombok") version "8.14.2"
}

group = "net.netbeing"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":cheap-core"))
    implementation(libs.sqlite.jdbc)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
