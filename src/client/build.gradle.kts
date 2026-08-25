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
			leadingSpacesToTabs()
			endWithNewline()
			trimTrailingWhitespace()
			targetExclude("build/generated/**/*")
		}
		kotlinGradle {
			ktlint().setEditorConfigPath(rootProject.file("../../.editorconfig"))
			leadingSpacesToTabs()
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
			name = "github-fusion"
			url = uri("https://maven.pkg.github.com/Quutio/Fusion")
			credentials {
				username = gprUser ?: System.getenv("GITHUB_ACTOR")
				password = gprPassword ?: System.getenv("GITHUB_TOKEN")
			}
		}
	}

	kotlin {
		jvmToolchain(21)
	}
}

subprojects {
	apply(plugin = "maven-publish")

	publishing {
		publications {
			register("bouncer", MavenPublication::class) {
				from(components["java"])

				this.artifactId = project.name.lowercase()

				pom {
					this.name.set(project.name)
					this.description.set(project.description)
				}
			}
		}
	}
}
