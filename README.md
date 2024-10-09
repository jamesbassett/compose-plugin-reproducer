# compose-plugin-reproducer
Reproduces the "Cannot use shouldRunAfter to reference tasks from another build" issue encountered when
using the [Avast Gradle Docker Compose Plugin](https://github.com/avast/gradle-docker-compose-plugin) in a 
Gradle project that uses [composite builds](https://docs.gradle.org/current/userguide/composite_builds.html).

Building this project will run a wiremock server in docker compose and then run a test that hits an endpoint.

## How to Reproduce

To reproduce the issue, run `./gradlew test` from the root of the project. The test will fail with the following error:

```
FAILURE: Build failed with an exception.

* What went wrong:
Cannot use shouldRunAfter to reference tasks from another build.
```

## Root Cause

This issue seems to be caused by a combination of:
* the changes introduced in [#356](https://github.com/avast/gradle-docker-compose-plugin/pull/356) (using `shouldRunAfter task.taskDependencies` in `isRequiredBy()`) 
* Gradle 8 [making it an error](https://docs.gradle.org/8.0/userguide/upgrading_version_7.html#warnings_that_are_now_errors) to reference tasks in an included build with `shouldRunAfter`

## Workaround

To work around the issue, you can set up the task dependencies manually and avoid the use of `shouldRunAfter task.taskDependencies`,
as is done in `my-project/build.gradle.kts`. This is not ideal (presumably this exists for a good reason - probably to stop composing up too early).

You can see this workaround in action by running `./gradlew test -PenableWorkaround=true` from the root of the project.
