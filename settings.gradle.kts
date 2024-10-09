plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "compose-plugin-reproducer"
include("my-project")
includeBuild("my-included-build")
