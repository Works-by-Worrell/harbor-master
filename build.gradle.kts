plugins {
    kotlin("jvm") version "2.1.10" apply false
}

allprojects {
    group = "com.worksbyworrell.harbormaster"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // Only configure actual leaf modules, skipping intermediate containers like ':modules'
    if (childProjects.isEmpty()) {
        apply(plugin = "org.jetbrains.kotlin.jvm")

        configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }
    }
}
