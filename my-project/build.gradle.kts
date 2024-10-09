import com.avast.gradle.dockercompose.ComposeExtension
import com.avast.gradle.dockercompose.TasksConfigurator

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
    isRequiredBy(testTask, includedBuildWorkaround = enableWorkaround.toBoolean())
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

/**
 * A variant of isRequiredBy that can apply the workaround or delegate to the original implementation.
 */
fun <T: Task> ComposeExtension.isRequiredBy(taskProvider: TaskProvider<T>, includedBuildWorkaround: Boolean) {
    if (includedBuildWorkaround) {
        taskProvider.configure { tasksConfigurator.isRequiredByCoreWithIncludedBuildWorkaround(this, true) }
    } else {
        isRequiredBy(taskProvider)
    }
}

/**
 * A reimplementation of TaskConfigurator.isRequiredByCore() that filters out task dependencies from included builds.
 * For the original see: https://github.com/avast/gradle-docker-compose-plugin/blob/main/src/main/groovy/com/avast/gradle/dockercompose/TasksConfigurator.groovy#L122-L132
 */
private fun TasksConfigurator.isRequiredByCoreWithIncludedBuildWorkaround(task: Task, fromConfigure: Boolean) {
    task.dependsOn(upTask)
    task.finalizedBy(downTask)

    // ignore dependencies from included builds from the upTask shouldRunAfter dependencies
    // (comparing on projectDir may not be the best - is name reliable?)
    val includedBuildDirectories = gradle.includedBuilds.map { it.projectDir }
    val filteredTaskDependencies = task.taskDependencies.getDependencies(null).filter {
        val includeTask = it.project.projectDir !in includedBuildDirectories
        println("${ if (includeTask) "including" else "excluding"} task: ${it.path} from ${it.project.name}")
        includeTask
    }

    if (fromConfigure) {
        upTask.get().shouldRunAfter(filteredTaskDependencies)
    } else {
        upTask.configure { shouldRunAfter(filteredTaskDependencies) }
    }

    (task as? ProcessForkOptions)?.let {
        task.doFirst { composeSettings.exposeAsEnvironment(it) }
    }
    (task as? JavaForkOptions)?.let {
        task.doFirst { composeSettings.exposeAsSystemProperties(it) }
    }
}
