plugins {
    `java-library`
    kotlin("jvm") version "1.3.41"
}

apply(from = "../dependencies.gradle")

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

val org.gradle.api.plugins.ExtraPropertiesExtension.deps: Map<String, String>
    get() = this["deps"] as Map<String, String>
dependencies {
    implementation(kotlin("stdlib"))

    implementation(extra.deps.getValue("commonsLang"))
    implementation(extra.deps.getValue("gson"))
    implementation(extra.deps.getValue("guava"))
    implementation(extra.deps.getValue("jsr305"))
    api("org.simpleframework:simple-xml:2.7.1")
    api(extra.deps.getValue("slf4j"))

    testCompile(group = "junit", name = "junit", version = "4.12")
}
