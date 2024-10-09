plugins {
    kotlin("jvm") version "2.0.20"
    `java-library`
}

group = "example"

repositories {
    mavenCentral()
}

dependencies {
    dependencies {
        implementation("com.squareup.okhttp3:okhttp:4.12.0")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
