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
        // simulate isRequiredBy with modified shouldRunAfter logic (this works)
        val upTask = tasksConfigurator.upTask
        val downTask = tasksConfigurator.downTask
        testTask.configure {
            dependsOn(upTask)
            finalizedBy(downTask)

            // ignore dependencies from included builds from the upTask shouldRunAfter dependencies
            // (comparing on projectDir may not be the best - is name reliable?)
            val includedBuildDirectories = gradle.includedBuilds.map { it.projectDir }
            val filteredTaskDependencies = testTask.get().taskDependencies.getDependencies(null).filter {
                val includeTask = it.project.projectDir !in includedBuildDirectories
                println("${ if (includeTask) "including" else "excluding"} task: ${it.path} from ${it.project.name}")
                includeTask
            }

            upTask.get().shouldRunAfter(filteredTaskDependencies)
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
