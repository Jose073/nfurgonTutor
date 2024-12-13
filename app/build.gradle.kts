plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.nfurgontutor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.nfurgontutor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    implementation(libs.firebase.auth)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("io.reactivex.rxjava3:rxjava:3.1.9")
    implementation("androidx.core:core-splashscreen:1.0.1")

    //databaseUI
    implementation("com.firebaseui:firebase-ui-auth:7.2.0")

    //firebase database
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")

    //karumi
    implementation("com.karumi:dexter:6.1.2")
    implementation("com.google.android.material:material:1.8.0")

    //location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    //circle image
    implementation("de.hdodenhof:circleimageview:3.1.0")

    //Firebase Storage
    implementation("com.google.firebase:firebase-storage:21.0.1")

    //glide
    implementation("com.github.bumptech.glide:glide:4.11.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.11.0")

    //Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging:24.0.3")

    //Geofire
    implementation("com.firebase:geofire-android:3.2.0")

    //Retrofit
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    //Sliding
    implementation("com.sothree.slidinguppanel:library:3.3.1")

    //Google Place
    implementation("com.google.android.libraries.places:places:4.1.0")

    //EventBus
    implementation("org.greenrobot:eventbus:3.2.0")

    //Maps Utils
    implementation("com.google.maps.android:android-maps-utils-v3:2.2.6")

    //osmdroid
    implementation("org.osmdroid:osmdroid-android:6.1.14")

}