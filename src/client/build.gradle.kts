import com.diffplug.gradle.spotless.BaseKotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	`maven-publish`

	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.shadow)
	alias(libs.plugins.spotless)
}

group = "fi.joniaromaa"
version = "1.0-SNAPSHOT"

allprojects {
	apply(plugin = "com.diffplug.spotless")

	spotless {
		fun BaseKotlinExtension.setupKtlint(allman: Boolean = true): BaseKotlinExtension {
			this.ktlint()
				.setEditorConfigPath(rootProject.file("../../.editorconfig"))
				.editorConfigOverride(
					mapOf(
						"ij_kotlin_allow_trailing_comma" to "false",
						"ij_kotlin_allow_trailing_comma_on_call_site" to "false"
					) + if (allman) {
						mapOf(
							// Messes up with allman
							"ktlint_standard_indent" to "disabled",
							"ktlint_standard_curly-spacing" to "disabled",
							"ktlint_standard_keyword-spacing" to "disabled",
							"ktlint_standard_no-line-break-after-else" to "disabled",
							"ktlint_standard_function-signature" to "disabled",
							"ktlint_standard_function-start-of-body-spacing" to "disabled",
							"ktlint_standard_unnecessary-parentheses-before-trailing-lambda" to "disabled"
						)
					} else {
						mapOf()
					}
				).run {
					if (allman) {
						this.customRuleSets(listOf("fi.joniaromaa:linter:1.0-SNAPSHOT"))
					}
				}

			return this
		}

		kotlin {
			setupKtlint()
			indentWithTabs()
			endWithNewline()
			trimTrailingWhitespace()
			targetExclude("build/generated/**/*")
		}
		kotlinGradle {
			setupKtlint(allman = false)
			indentWithTabs()
			endWithNewline()
			trimTrailingWhitespace()
		}
	}

	repositories {
		mavenLocal()
		mavenCentral()
	}

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			jvmTarget = "17"
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
		languageVersion.set(JavaLanguageVersion.of(11))
	}
}
