plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.ecommerceapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ecommerceapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        //enable multiDex
        multiDexEnabled = true

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
    kotlinOptions {
        jvmTarget = "11"
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
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.room.ktx)
    implementation(libs.play.services.identity)
    implementation(libs.androidx.datastore.core.android)
    implementation(libs.play.services.wallet)
    implementation(libs.play.services.analytics.impl)




    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Firebase
    implementation (platform("com.google.firebase:firebase-bom:32.5.0"))
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.firebase:firebase-database-ktx")
    // Add Firebase Storage
    implementation ("com.google.firebase:firebase-storage-ktx:20.3.0")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    //lottie flies
    implementation ("com.airbnb.android:lottie-compose:6.6.2")

    // Coil for image loading
    implementation ("io.coil-kt:coil-compose:2.4.0")

    // Material components
    implementation ("androidx.compose.material:material:1.5.0")
    implementation ("androidx.compose.material:material-icons-extended:1.5.0")


    // ZaloPay SDK - Choose ONE of the following options:


    // Additional dependencies for ZaloPay that might be required
//    implementation("com.squareup.okhttp3:okhttp:4.6.0")
//    implementation("commons-codec:commons-codec:1.14")
    //import ZaloPay SDK
//    implementation(fileTree(mapOf(
//        "dir" to "D:\\ZaloLib\\DemoZPDK_Android",
//        "include" to listOf("*.aar", "*.jar"),
//        "exclude" to listOf("libs/*.jar")
//    )))
//    implementation(files("D:\\ZaloLib2\\DemoZPDK_Flutter\\DemoZPDK_Flutter\\ZPDK-Android\\zpdk-release-28052021.aar"))

    implementation(fileTree(mapOf(
        "dir" to "D:\\ZaloLib2",
        "include" to listOf("*.aar", "*.jar"),
        "exclude" to listOf("libs/*.jar")
    )))
    // Replace with path to local AAR file
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$2.0.0")

    // DataStore Preferences
    implementation ("androidx.datastore:datastore-preferences:1.0.0")

    //Crisp Chat
    implementation ("im.crisp:crisp-sdk:2.0.11")

    // If you're not using AndroidX
    implementation ("com.android.support:multidex:1.0.3")

}