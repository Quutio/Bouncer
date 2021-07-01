import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    kotlin("jvm")

    id("com.google.protobuf")

    idea
}

group = "fi.joniaromaa"
version = "1.0-SNAPSHOT"

dependencies {
    protobuf(project(":bouncer-protos"))

    api(kotlin("stdlib"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

    api("io.grpc:grpc-protobuf:1.39.0")
    api("com.google.protobuf:protobuf-java-util:3.17.3")
    api("io.grpc:grpc-kotlin-stub:1.1.0")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.17.3"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.39.0"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.1.0:jdk7@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}