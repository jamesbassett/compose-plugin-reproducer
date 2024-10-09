import org.jetbrains.kotlin.cli.common.toBooleanLenient

plugins {
    kotlin("jvm") version "2.0.20"
    `java-library`
    id("com.avast.gradle.docker-compose") version "0.17.8"
}

group = "example"

repositories {
    mavenCentral()
}

dependencies {
    implementation("example:my-included-build")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

val enableWorkaround: String? by project

dockerCompose {
    val testTask = tasks.named<Test>("test")
    if (!enableWorkaround.toBoolean()) {
        // the normal syntax (this breaks because the compose plugin adds a "shouldRunAfter task.taskDependencies"
        // which includes the dependencies from the included build (which is not allowed)
        isRequiredBy(testTask)
    } else {
        // simulate isRequiredBy but exclude the shouldRunAfter logic (this works)
        val upTask = tasksConfigurator.upTask
        val downTask = tasksConfigurator.downTask
        testTask.configure {
            dependsOn(upTask)
            finalizedBy(downTask)
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
