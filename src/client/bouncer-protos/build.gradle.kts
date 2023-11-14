plugins {
    `java-library`

    `maven-publish`
}

java {
    sourceSets.getByName("main").resources.srcDir("../../grpc")
}
