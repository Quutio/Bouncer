import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.protobuf

plugins {
	idea

	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.protobuf)
}

group = "io.quut"
version = "1.0-SNAPSHOT"

dependencies {
	protobuf(project(":bouncer-protos"))

	api(libs.kotlin.stdlib)
	api(libs.kotlinx.coroutines)

	api(libs.ptotobuf.kotlin)
	api(libs.grpc.protobuf)
	api(libs.grpc.kotlin.stub)

	runtimeOnly(libs.grpc.netty)
}

protobuf {
	protoc {
		artifact = libs.ptotobuf.protoc.get().toString()
	}
	plugins {
		id("grpc") {
			artifact = libs.grpc.plugin.get().toString()
		}
		id("grpckt") {
			artifact = "${libs.grpc.kotlin.plugin.get()}:jdk8@jar"
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
