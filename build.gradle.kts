plugins {
    kotlin("jvm") version "2.1.10" apply false
    id("org.sonarqube") version "5.1.0.4882"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    jacoco
    `maven-publish`
}

val sonarToken: String = System.getenv("SONAR_TOKEN") ?: findProperty("sonar.token")?.toString() ?: ""

sonar {
    properties {
        property("sonar.projectKey", "harbor-master")
        property("sonar.projectName", "Harbor Master")
        property("sonar.host.url", "https://sonar.worksbyworrell.com")
        property("sonar.token", sonarToken)
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            subprojects.map { "${it.layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml" },
        )
    }
}

allprojects {
    group = "com.worksbyworrell.harbormaster"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    if (childProjects.isEmpty()) {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "io.gitlab.arturbosch.detekt")
        apply(plugin = "jacoco")
        apply(plugin = "maven-publish")

        configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
            compilerOptions {
                freeCompilerArgs.addAll("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
            }
        }

        configure<JacocoPluginExtension> {
            toolVersion = "0.8.12"
        }

        tasks.withType<Test> {
            useJUnitPlatform()
            finalizedBy(tasks.named("jacocoTestReport"))
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.withType<Test>())
            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)
            }
        }

        tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.named("jacocoTestReport"))
            violationRules {
                rule {
                    limit {
                        counter = "LINE"
                        value = "COVEREDRATIO"
                        minimum = "0.80".toBigDecimal()
                    }
                    limit {
                        counter = "BRANCH"
                        value = "COVEREDRATIO"
                        minimum = "0.80".toBigDecimal()
                    }
                }
            }
        }

        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(files("$rootDir/detekt.yml"))
        }

        configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            android.set(false)
            outputToConsole.set(true)
            ignoreFailures.set(false)
        }

        tasks.named("check") {
            dependsOn(tasks.named("jacocoTestCoverageVerification"))
            dependsOn(tasks.named("ktlintCheck"))
            dependsOn(tasks.named("detekt"))
        }

        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "Forge"
                    url = uri("https://packages.worksbyworrell.com/api/packages/worksbyworrell/maven")
                    credentials {
                        username = "warlock"
                        password = System.getenv("FORGE_MAVEN_TOKEN") ?: System.getenv("GITEA_TOKEN") ?: ""
                    }
                }
            }
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                }
            }
        }
    }
}
