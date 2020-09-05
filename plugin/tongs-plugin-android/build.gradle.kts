plugins {
    `java-library`
    kotlin("jvm")
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
    implementation(extra.deps.getValue("kotlinxCoroutinesCore"))
    implementation(project(":tongs-common"))

    implementation("com.shazam:axmlparser:1.0")
    implementation(extra.deps.getValue("commonsIo"))
    implementation(group = "org.apache.commons", name = "commons-text", version = "1.4")
    implementation(extra.deps.getValue("ddmlib"))
    implementation(extra.deps.getValue("sdklib"))
    implementation("org.smali:dexlib2:2.4.0") {
        exclude(group = "com.google.guava")
    }
    implementation(extra.deps.getValue("gson"))
    implementation(extra.deps.getValue("guava"))
    implementation(extra.deps.getValue("jsr305"))
    implementation(extra.deps.getValue("slf4j"))
    implementation("com.madgag:animated-gif-lib:1.2") // TODO: move GIF creation back to plugin api or runner

    testImplementation(project(":tongs-common-test"))
    testImplementation(extra.deps.getValue("kotlinTest"))
    testImplementation(extra.deps.getValue("junit"))
    testImplementation(extra.deps.getValue("hamcrest"))
    testImplementation(extra.deps.getValue("junitParams"))
}
