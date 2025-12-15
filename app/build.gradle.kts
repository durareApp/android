plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    signingConfigs {
        create("config") {
            storeFile = file("changeThis")
            storePassword = "changeThis"
            keyAlias = "changeThis"
            keyPassword = "changeThis"
        }
    }
    namespace = "com.subhajitrajak.durare"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.subhajitrajak.durare"
        minSdk = 26
        targetSdk = 36
        versionCode = 19
        versionName = "0.6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            signingConfig = signingConfigs.getByName("config")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.pose.detection)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ML Kit Face Detection
    implementation(libs.face.detection)
    implementation(libs.pose.detection.common)
    
    // CameraX dependencies
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // fragment navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // flexbox layout
    implementation(libs.flexbox)

    // circle image view
    implementation(libs.circleimageview)

    // dots indicator
    implementation(libs.dotsindicator)

    // Firebase Bom
    implementation(platform(libs.firebase.bom))

    // firebase auth
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    // firestore
    implementation(libs.firebase.firestore.ktx)
    // listenable future support
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.guava.listenablefuture)
    implementation(libs.guava.android)

    // glide
    implementation(libs.glide)

    // swipe to refresh layout
    implementation(libs.androidx.swiperefreshlayout)

    // lottie
    implementation(libs.lottie)

    // splash api
    implementation(libs.androidx.core.splashscreen)

    // vico charts
    implementation(libs.vico.views)

    // android charts
    implementation(libs.androidchart)

    // room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // markdown
    implementation(libs.noties.markwon.core)

    // firebase ai
    implementation(libs.firebase.ai)

    // play integrity
    implementation(libs.integrity)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
}