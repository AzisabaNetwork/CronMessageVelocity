plugins {
    java
}

group = "net.azisaba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    // velocity repo
    maven { url = uri("https://nexus.velocitypowered.com/repository/maven-public/") }
}

dependencies {
    // velocity-api
    compileOnly("com.velocitypowered:velocity-api:3.0.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.0.1")

    // netty
    compileOnly("io.netty:netty-all:4.1.86.Final")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
}
