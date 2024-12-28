plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
//    alias(libs.plugins.kotlin.plugin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.poko)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    compilerOptions {
        progressiveMode = true
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.ExperimentalFoundationApi",
        )
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            // Potentially useful for static analysis tools or annotation processors.
            "-Xemit-jvm-type-annotations",
            // Enable new jvm-default behavior
            // https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/
            "-Xjvm-default=all",
            // https://kotlinlang.org/docs/whatsnew1520.html#support-for-jspecify-nullness-annotations
            "-Xjspecify-annotations=strict",
            // Match JVM assertion behavior:
            // https://publicobject.com/2019/11/18/kotlins-assert-is-not-like-javas-assert/
            "-Xassertions=jvm",
            "-Xtype-enhancement-improvements-strict-mode",
            "-Xexpect-actual-classes"
        )
    }
}

val aqVersionCode = providers.gradleProperty("aq_versioncode").map(String::toLong).get()
val aqVersionName = providers.gradleProperty("aq_versionname").get()

buildConfig {
    packageName("dev.jvmname.acquisitive")
    buildConfigField("String", "FS_VERSION_NAME", "\"$aqVersionName - $aqVersionCode\"")
    buildConfigField("Long", "VERSION_CODE", aqVersionCode)
    generateAtSync = true
}

android {
    namespace = "dev.jvmname.acquisitive"
    compileSdk = 35

    defaultConfig {
        minSdk = 33
        targetSdk = 35
        versionCode = aqVersionCode.toInt()
        versionName = aqVersionName
    }

    buildTypes {
        maybeCreate("debug").apply {
            versionNameSuffix = "-dev"
            applicationIdSuffix = ".debug"
            matchingFallbacks += listOf("release")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = libs.versions.jvmTarget.map(JavaVersion::toVersion).get()
        targetCompatibility = libs.versions.jvmTarget.map(JavaVersion::toVersion).get()
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    modules {
        module("com.google.guava:listenablefuture") { replacedBy("com.google.guava:guava") }
    }
    "coreLibraryDesugaring"(libs.desugarJdkLibs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.circuit.foundation)
    implementation(libs.circuit.overlay)
    implementation(libs.circuitx.overlays)
    implementation(libs.circuitx.gestureNav)

    implementation(libs.kotlinInject.runtime)
    implementation(libs.kotlinInject.anvil.runtime)
    implementation(libs.kotlinInject.anvil.runtime.optional)
    ksp(libs.kotlinInject.compiler)
    ksp(libs.kotlinInject.anvil.compiler)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines)

    implementation(libs.sqldelight.async)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.sqldelight.driver.android)

    implementation(platform(libs.square.retrofit.bom))
    implementation(libs.square.retrofit)
    implementation(libs.square.retrofit.moshi)
    implementation(libs.square.moshi)
    ksp(libs.square.moshiKotlin)
    implementation(libs.square.moshiAdapters)
    implementation(libs.square.moshiSealed)
    ksp(libs.square.moshiSealedCodegen)

    implementation(libs.sqkon)
}