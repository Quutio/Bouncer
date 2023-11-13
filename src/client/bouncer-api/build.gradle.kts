plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "fi.joniaromaa"
version = "1.0-SNAPSHOT"

dependencies {
    api(project(":bouncer-grpc-stubs"))
}