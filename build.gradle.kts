import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    application
}

group = "me.random_number"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.flink:flink-streaming-java:1.16.0")
    implementation("org.apache.flink:flink-clients:1.16.0")
    implementation("org.apache.flink:flink-connector-kafka:1.16.0")
    implementation("org.apache.flink:flink-table-runtime:1.16.0")
    implementation("org.apache.flink:flink-json:1.16.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.slf4j:slf4j-simple:1.7.30")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("flink.LatencyReportingFlinkAppKt")
}

tasks.register<Jar>("flinkJar") {
    archiveBaseName.set("flink-job")
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}