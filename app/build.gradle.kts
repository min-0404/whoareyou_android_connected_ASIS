plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.whoareyou"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.whoareyou"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "env"

    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_URL", "\"https://isrnd.bccard.com:64443/\"")
            buildConfigField("Boolean", "TRUST_ALL_SSL", "true")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://u2.bccard.com/\"")
            buildConfigField("Boolean", "TRUST_ALL_SSL", "false")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        // BuildConfig 생성 활성화: BuildConfig.DEBUG 등 빌드 상수를 코드에서 참조 가능
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
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
    implementation(libs.androidx.navigation.compose)
    // 이미지 자동 다운로드 + 캐시 라이브러리
    implementation("io.coil-kt:coil-compose:2.6.0")
    // 확장 Material 아이콘 (CallReceived, CallMade, CallMissed 등)
    implementation("androidx.compose.material:material-icons-extended")

    // ── 네트워크 레이어 ──────────────────────────────────────────────────────
    // Retrofit2: HTTP API 클라이언트 (WhoAreYouApi 인터페이스 구현체 생성)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson Converter: ApiResponse<T> 등 Kotlin data class 자동 역직렬화
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp3: Retrofit 기본 HTTP 클라이언트
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // OkHttp Logging Interceptor: 디버그 빌드에서 요청/응답 Logcat 출력
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Gson: JSON 파싱 라이브러리 (@SerializedName 어노테이션 지원)
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}