import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"

    id("com.google.protobuf") version "0.9.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "fi.joniaromaa"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

subprojects {
    group = parent!!.group
    version = parent!!.version

    pluginManager.withPlugin("maven-publish") {
        extensions.getByType(PublishingExtension::class).apply {
            publications {
                create<MavenPublication>(project.name) {
                    groupId = "$group"
                    artifactId = project.name
                    version = version

                    from(components["java"])
                }
            }

            val mavenUser: String by project
            val mavenPassword: String by project

            repositories {
                maven {
                    credentials {
                        username = mavenUser
                        password = mavenPassword
                    }

                    name = "equelix-snapshots"
                    url = uri("https://maven.quut.io/repository/maven-snapshots/")
                }
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}
