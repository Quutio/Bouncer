import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.protobuf

plugins {
    kotlin("jvm")

    id("com.google.protobuf")

    idea

    `maven-publish`
}

group = "fi.joniaromaa"
version = "1.0-SNAPSHOT"

dependencies {
    protobuf(project(":bouncer-protos"))

    api(kotlin("stdlib"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    api("io.grpc:grpc-protobuf:1.59.0")
    api("io.grpc:grpc-kotlin-stub:1.4.0")
    api("com.google.protobuf:protobuf-kotlin:3.25.0")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.59.0"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}
