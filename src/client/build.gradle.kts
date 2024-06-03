import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	`maven-publish`
	`java-library`

	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.shadow)
	alias(libs.plugins.spotless)
}

allprojects {
	group = "io.quut"
	version = "1.0-SNAPSHOT"

	apply(plugin = "java-library")
	apply(plugin = "com.diffplug.spotless")

	spotless {
		kotlin {
			ktlint().setEditorConfigPath(rootProject.file("../../.editorconfig"))
			indentWithTabs()
			endWithNewline()
			trimTrailingWhitespace()
			targetExclude("build/generated/**/*")
		}
		kotlinGradle {
			ktlint().setEditorConfigPath(rootProject.file("../../.editorconfig"))
			indentWithTabs()
			endWithNewline()
			trimTrailingWhitespace()
		}
	}

	repositories {
		mavenCentral()

		val gprUser: String? by project
		val gprPassword: String? by project

		maven {
			name = "github"
			url = uri("https://maven.pkg.github.com/quutio/Harmony")
			credentials {
				username = gprUser ?: System.getenv("GITHUB_ACTOR")
				password = gprPassword ?: System.getenv("GITHUB_TOKEN")
			}
		}
	}

	val targetJava = 11
	val targetJavaVersion = JavaVersion.toVersion(targetJava)
	java {
		sourceCompatibility = targetJavaVersion
		targetCompatibility = targetJavaVersion
		if (JavaVersion.current() < targetJavaVersion) {
			toolchain {
				languageVersion.set(JavaLanguageVersion.of(targetJava))
			}
		}
	}

	tasks {
		withType<JavaCompile> {
			options.encoding = "UTF-8"
			options.release.set(targetJava)
		}

		withType<KotlinCompile> {
			kotlinOptions {
				jvmTarget = targetJava.toString()
			}
		}
	}
}

subprojects {
	apply(plugin = "maven-publish")

	publishing {
		publications {
			register("harmony", MavenPublication::class) {
				from(components["java"])

				this.artifactId = project.name.lowercase()

				pom {
					this.name.set(project.name)
					this.description.set(project.description)
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
