plugins {
    `java-library`
}

apply(plugin = "kotlin")

java {
    sourceCompatibility = rootProject.extra["javaSourceCompatibility"] as JavaVersion
    targetCompatibility = rootProject.extra["javaTargetCompatibility"] as JavaVersion
}

repositories {
    mavenCentral()
}

val deps: Map<String, String>
    get() = rootProject.extra["deps"] as Map<String, String>
dependencies {
    // This module should have minimal dependencies as it is "imported" by the gradle plugin

    implementation(deps.getValue("jsr305"))
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")

    api(deps.getValue("slf4j"))

    testImplementation(group = "junit", name = "junit", version = "4.12")
}
