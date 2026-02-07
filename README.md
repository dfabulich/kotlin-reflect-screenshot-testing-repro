# KotlinReflectRepro

Minimal project to reproduce the Compose Preview Screenshot Testing issue where `compose-preview-renderer-0.0.1-alpha13.jar` bundles `kotlin/reflect/KClasses.class`, which shadows the real `kotlin-reflect` and causes `KotlinReflectionNotSupportedError` when a composable uses Kotlin reflection.

## Steps to reproduce this code

1. In Android Studio, create a new project with an Empty Activity (initial commit [ffdf41e3](https://github.com/dfabulich/kotlin-reflect-screenshot-testing-repro/commit/ffdf41e3f2ea98799f5f8b227e80b249c5863485))
2. Add screenshot testing to the project, following the steps documented in the [Compose Preview Screenshot Testing docs](https://developer.android.com/studio/preview/compose-screenshot-testing) [a8feade](https://github.com/dfabulich/kotlin-reflect-screenshot-testing-repro/commit/a8feade59041feb1dc1a779db6de308adfbc72a7)
3. Add `kotlin-reflect` to the project and do some Kotlin reflection in the test composable. [c55902d](https://github.com/dfabulich/kotlin-reflect-screenshot-testing-repro/commit/c55902dad64a26446d5b8d8ad8c281ff43ac19ce)

## Reproducing the bug

Run the screenshot test:

```bash
./gradlew --rerun-tasks updateDebugScreenshotTest
```

The preview render step fails with:

```
kotlin.jvm.KotlinReflectionNotSupportedError: Kotlin reflection implementation is not found at runtime. Make sure you have kotlin-reflect.jar in the classpath
    at kotlin.reflect.full.KClasses.getFunctions(KClasses.kt:89)
    at com.example.kotlinreflectrepro.MainActivityKt.Greeting(MainActivity.kt:36)
```

## Cause

This is happening because the renderer runs in an isolated `layoutLib` classloader https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:preview/screenshot/screenshot-validation-junit-engine/src/main/java/com/android/tools/screenshot/renderer/Renderer.kt;l=59?q=renderer.kt which includes `compose-preview-renderer-0.0.1-alpha13.jar`; that jar bundles `kotlin/reflect/KClasses.class`, a stub that throws the exception.

## Workaround

Prepend the real `kotlin-reflect` JAR to the layoutlib classpath so it is loaded before the compose-preview-renderer stub. The `app/build.gradle.kts` in this repo includes an `afterEvaluate` block that:

1. Resolves `org.jetbrains.kotlin:kotlin-reflect` (using the project’s Kotlin version).
2. For `updateDebugScreenshotTest` and `validateDebugScreenshotTest`, prepends those JARs to the task’s layoutlib classpath via reflection.

With this in place, `./gradlew --rerun-tasks updateDebugScreenshotTest` and `./gradlew validateDebugScreenshotTest` succeed. If you add screenshot tests for other variants (e.g. release), add corresponding `tasks.findByName("updateReleaseScreenshotTest")?.let { ... }` (and validate) in the same block.
