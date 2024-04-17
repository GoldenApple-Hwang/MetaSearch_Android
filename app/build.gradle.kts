plugins {
    id("com.android.application")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.metasearch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.metasearch"
        minSdk = 33
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    // Retrofit dependency
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson dependency
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp dependency
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    //neo4j dependency
    implementation("org.neo4j.driver:neo4j-java-driver:4.4.0")
    //fragment dependency
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    //CircleImageView dependency
    implementation("de.hdodenhof:circleimageview:3.1.0")
    //PhotoView dependency
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    //SplashScreen dependency
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}