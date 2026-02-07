plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.example.kotlinreflectrepro"
    compileSdk {
        version = release(36)
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    defaultConfig {
        applicationId = "com.example.kotlinreflectrepro"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}

// Workaround for https://issuetracker.google.com/issues/482433854
// compose-preview-renderer bundles stub kotlin/reflect classes that throw
// KotlinReflectionNotSupportedError. Prepend the real kotlin-reflect to the
// layoutlib classpath so it is loaded first in the isolated renderer classloader.
afterEvaluate {
    val kotlinReflectConfig = configurations.detachedConfiguration(
        dependencies.create("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}")
    )
    kotlinReflectConfig.resolve()
    val kotlinReflectJars = kotlinReflectConfig.files

    fun prependKotlinReflectToLayoutlib(task: Task) {
        val getTestEngineInput = (task as java.lang.Object).javaClass.getMethod("getTestEngineInput")
        val input = getTestEngineInput.invoke(task)!!
        val layoutlibClassPath = input.javaClass.getMethod("getLayoutlibClassPath").invoke(input)!!
        val existingFiles = layoutlibClassPath.javaClass.getMethod("getFiles").invoke(layoutlibClassPath) as Iterable<*>
        val newFiles = kotlinReflectJars + existingFiles.filterIsInstance<java.io.File>()
        // setFrom(Iterable) - pass as Iterable for Gradle's ConfigurableFileCollection
        layoutlibClassPath.javaClass.getMethod("setFrom", Iterable::class.java).invoke(layoutlibClassPath, newFiles)
    }
    tasks.findByName("updateDebugScreenshotTest")?.let { prependKotlinReflectToLayoutlib(it) }
    tasks.findByName("validateDebugScreenshotTest")?.let { prependKotlinReflectToLayoutlib(it) }
}