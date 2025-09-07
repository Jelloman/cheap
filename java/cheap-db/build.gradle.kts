plugins {
    id("java")
}

group = "net.netbeing"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":cheap-core"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}