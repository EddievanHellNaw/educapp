plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")

    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.example.educapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.educapp"

        minSdk = 27
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {

    /*
     * ---------------------------------------------------------
     * COMPOSE
     * ---------------------------------------------------------
     *
     * One BOM controls the versions of all Compose libraries.
     */

    val composeBom = platform(libs.androidx.compose.bom)

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.compose.material3:material3")

    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.compose.ui:ui-text-google-fonts")

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    /*
     * IMPORTANT:
     *
     * Use the actual Maven coordinates here instead of the
     * version-catalog aliases that were resolving to:
     *
     * androidx.compose.ui:ui-test-junit4:
     *
     * The Compose BOM supplies the version.
     */

    androidTestImplementation(
        "androidx.compose.ui:ui-test-junit4"
    )

    debugImplementation(
        "androidx.compose.ui:ui-test-manifest"
    )


    /*
     * ---------------------------------------------------------
     * ANDROIDX / ANDROID
     * ---------------------------------------------------------
     */

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.navigation.runtime.ktx)

    implementation(
        "androidx.navigation:navigation-compose:2.7.7"
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-ktx:2.8.4"
    )

    implementation(
        "androidx.constraintlayout:constraintlayout-compose:1.0.1"
    )

    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.tv.material)

    implementation(
        "androidx.work:work-runtime-ktx:2.8.1"
    )

    implementation(
        "androidx.exifinterface:exifinterface:1.3.6"
    )


    /*
     * ---------------------------------------------------------
     * DATASTORE
     * ---------------------------------------------------------
     *
     * datastore-preferences already brings in the appropriate
     * core dependencies, so we don't need three different
     * DataStore versions/artifacts.
     */

    implementation(
        "androidx.datastore:datastore-preferences:1.1.3"
    )


    /*
     * ---------------------------------------------------------
     * ROOM
     * ---------------------------------------------------------
     */

    implementation(
        "androidx.room:room-runtime:2.6.1"
    )

    implementation(
        "androidx.room:room-ktx:2.6.1"
    )


    /*
     * ---------------------------------------------------------
     * FIREBASE
     * ---------------------------------------------------------
     */

    implementation(
        platform("com.google.firebase:firebase-bom:33.2.0")
    )

    implementation(
        "com.google.firebase:firebase-firestore-ktx"
    )

    implementation(
        "com.google.firebase:firebase-analytics"
    )

    implementation(
        "com.google.firebase:firebase-auth-ktx"
    )

    /*
     * Keep this for now if the existing application still
     * references Firebase Dynamic Links.
     */
    implementation(libs.firebase.dynamic.links.ktx)


    /*
     * ---------------------------------------------------------
     * COROUTINES
     * ---------------------------------------------------------
     */

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1"
    )


    /*
     * ---------------------------------------------------------
     * NETWORKING
     * ---------------------------------------------------------
     */

    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-scalars:2.11.0"
    )

    implementation(
        "com.squareup.okhttp3:okhttp:5.0.0-alpha.14"
    )

    implementation(
        "com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14"
    )

    implementation(
        "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0"
    )


    /*
     * ---------------------------------------------------------
     * KOTLIN SERIALIZATION
     * ---------------------------------------------------------
     */

    implementation(
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0"
    )


    /*
     * ---------------------------------------------------------
     * FIREBASE / GOOGLE
     * ---------------------------------------------------------
     */

    implementation(libs.places)


    /*
     * ---------------------------------------------------------
     * DEPENDENCY INJECTION
     * ---------------------------------------------------------
     */

    implementation(
        "io.insert-koin:koin-android:3.4.3"
    )

    implementation(
        "io.insert-koin:koin-androidx-compose:3.4.3"
    )


    /*
     * ---------------------------------------------------------
     * PDF / OCR
     * ---------------------------------------------------------
     */

    implementation(
        "com.tom-roush:pdfbox-android:2.0.27.0"
    )

    implementation(
        "com.rmtheis:tess-two:9.1.0"
    )


    /*
     * ---------------------------------------------------------
     * TEXT / HTML
     * ---------------------------------------------------------
     */

    implementation(
        "org.jsoup:jsoup:1.18.1"
    )

    implementation(
        "org.apache.commons:commons-text:1.10.0"
    )

    implementation(
        "org.apache.commons:commons-io:1.3.2"
    )

    implementation(
        "jp.wasabeef:richeditor-android:2.0.0"
    )


    /*
     * ---------------------------------------------------------
     * RICH TEXT
     * ---------------------------------------------------------
     */

    implementation(
        "com.halilibo.compose-richtext:richtext-ui-material3:1.0.0-alpha02"
    )

    implementation(
        "com.halilibo.compose-richtext:richtext-markdown:1.0.0-alpha02"
    )

    implementation(
        "com.halilibo.compose-richtext:richtext-commonmark:1.0.0-alpha02"
    )


    /*
     * ---------------------------------------------------------
     * NAVIGATION ANIMATION
     * ---------------------------------------------------------
     *
     * This is deprecated, but your MainActivity still uses it.
     * Leave it installed until we migrate navigation.
     */

    implementation(
        "com.google.accompanist:accompanist-navigation-animation:0.36.0"
    )


    /*
     * ---------------------------------------------------------
     * REORDERABLE
     * ---------------------------------------------------------
     *
     * Your current attendance screen uses custom drag handling,
     * but keep this if another screen uses it.
     */

    implementation(
        "sh.calvin.reorderable:reorderable:3.0.0"
    )


    /*
     * ---------------------------------------------------------
     * OTHER UTILITIES
     * ---------------------------------------------------------
     */

    implementation(
        "com.jakewharton.timber:timber:5.0.1"
    )

    implementation(
        "com.squareup:javapoet:1.11.0"
    )


    /*
     * ---------------------------------------------------------
     * JAVA 8+ API DESUGARING
     * ---------------------------------------------------------
     */

    coreLibraryDesugaring(
        "com.android.tools:desugar_jdk_libs:2.1.1"
    )


    /*
     * ---------------------------------------------------------
     * TESTS
     * ---------------------------------------------------------
     */

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)

    androidTestImplementation(
        libs.androidx.espresso.core
    )
}