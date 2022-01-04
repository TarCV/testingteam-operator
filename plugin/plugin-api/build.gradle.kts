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
    implementation(kotlin("stdlib"))

    implementation(deps.getValue("commonsLang"))
    implementation(deps.getValue("gson"))
    implementation(deps.getValue("guava"))
    implementation(deps.getValue("jsr305"))
    api("org.simpleframework:simple-xml:2.7.1")
    api(deps.getValue("slf4j"))

    testImplementation(group = "junit", name = "junit", version = "4.12")
}
