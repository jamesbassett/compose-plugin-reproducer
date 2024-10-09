import com.avast.gradle.dockercompose.ComposeExtension
import com.avast.gradle.dockercompose.TasksConfigurator

plugins {
    kotlin("jvm")
    `java-library`
    id("com.avast.gradle.docker-compose") version "0.17.8"
}

group = "example"

repositories {
    mavenCentral()
}

dependencies {
    implementation("example:my-included-build")

    // replace the above with the following to use a normal (not composite) build
//    implementation(project(":my-included-build"))
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()

            targets.all {
                testTask.configure {
                    testLogging {
                        events("passed", "skipped", "failed")
                    }
                }
            }
        }
    }
}

val enableWorkaround: String? by project

dockerCompose {
    // for testing eager task evaluation still works
//    val testTask = tasks.getByName("test")

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
 * A variant of isRequiredBy that can apply the workaround or delegate to the original implementation.
 */
fun ComposeExtension.isRequiredBy(task: Task, includedBuildWorkaround: Boolean) {
    if (includedBuildWorkaround) {
        tasksConfigurator.isRequiredByCoreWithIncludedBuildWorkaround(task, false)
    } else {
        isRequiredBy(task)
    }
}

/**
 * A reimplementation of TaskConfigurator.isRequiredByCore() that filters out task dependencies from included builds.
 * For the original see: https://github.com/avast/gradle-docker-compose-plugin/blob/main/src/main/groovy/com/avast/gradle/dockercompose/TasksConfigurator.groovy#L122-L132
 */
private fun TasksConfigurator.isRequiredByCoreWithIncludedBuildWorkaround(task: Task, fromConfigure: Boolean) {
    task.dependsOn(upTask)
    task.finalizedBy(downTask)

    val filteredTaskDependencies = if (gradle.includedBuilds.isEmpty()) {
        task.taskDependencies
    } else {
        // Ignore any task dependencies from a composite/included build by delegating to a lazily filtered TaskDependency implementation
        // to avoid the "Cannot use shouldRunAfter to reference tasks from another build" error introduced in Gradle 8
        val includedBuildProjectNames = gradle.includedBuilds.map { it.name }
        TaskDependency { t ->
            task.taskDependencies.getDependencies(t).filter { dependency ->
                val includeTask = dependency.project.name !in includedBuildProjectNames
                println("${ if (includeTask) "including" else "excluding"} task: ${dependency.path} from ${dependency.project.name}")
                includeTask
            }.toMutableSet()
        }
    }

    // delay execution of the up task until all the tasks dependencies have run
    // (we don't want to wait for compose up if there's a compilation issue)
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
