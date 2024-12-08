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
	apply(plugin = "kotlin")
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
		mavenLocal()

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

	kotlin {
		jvmToolchain(11)
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
