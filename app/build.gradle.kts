plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.iptvcpruebadesdecero"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.iptvcpruebadesdecero"
        minSdk = 29  // Android 10+ (dispositivos desde 2019) - Máxima compatibilidad
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "version"

    // NOTA: Cuando hay productFlavors, debes especificar el flavor al ejecutar tareas:
    // - En Android Studio: Build Variants → selecciona el flavor deseado
    // - En línea de comandos: usa tareas específicas como assembleFullDebug, assembleNewDevicesDebug, etc.
    productFlavors {
        create("full") {
            dimension = "version"
            applicationIdSuffix = ".full"
            versionNameSuffix = "-full"
            minSdk = 21
            targetSdk = 34
        }

        create("newDevices") {
            dimension = "version"
            applicationIdSuffix = ".new"
            versionNameSuffix = "-new"
            minSdk = 29  // Android 10+
            targetSdk = 34
            ndk {
                abiFilters += "arm64-v8a"
            }
        }

        create("oldDevices") {
            dimension = "version"
            applicationIdSuffix = ".old"
            versionNameSuffix = "-old"
            minSdk = 21
            targetSdk = 28  // Mejor compatibilidad
            ndk {
                abiFilters += "armeabi-v7a"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Optimizaciones adicionales para reducir tamaño
            isDebuggable = false
            isJniDebuggable = false
            isPseudoLocalesEnabled = false
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
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
        viewBinding = true
    }
    
    // Optimizaciones para reducir tamaño del APK
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
        }
    }
    
    // Splits por ABI deshabilitado - los flavors manejan los filtros ABI
    // splits {
    //     abi {
    //         isEnable = true
    //         reset()
    //         include("armeabi-v7a", "arm64-v8a")  // Solo dispositivos reales
    //         isUniversalApk = false
    //     }
    // }
}

// Solución para resolver ambigüedad: crear tareas alias que apunten al flavor "full"
// Esto permite ejecutar tareas genéricas sin especificar el flavor
afterEvaluate {
    val defaultFlavor = "full"
    
    // Mapeo de tareas genéricas a tareas específicas del flavor "full"
    val taskMappings = mapOf(
        "assembleDebug" to "assemble${defaultFlavor.capitalize()}Debug",
        "assembleRelease" to "assemble${defaultFlavor.capitalize()}Release",
        "assembleDebugUnitTest" to "assemble${defaultFlavor.capitalize()}DebugUnitTest",
        "assembleDebugAndroidTest" to "assemble${defaultFlavor.capitalize()}DebugAndroidTest",
        "testDebugUnitTest" to "test${defaultFlavor.capitalize()}DebugUnitTest",
        "connectedDebugAndroidTest" to "connected${defaultFlavor.capitalize()}DebugAndroidTest",
        "installDebug" to "install${defaultFlavor.capitalize()}Debug",
        "installRelease" to "install${defaultFlavor.capitalize()}Release",
        "uninstallDebug" to "uninstall${defaultFlavor.capitalize()}Debug",
        "uninstallRelease" to "uninstall${defaultFlavor.capitalize()}Release"
    )
    
    taskMappings.forEach { (genericName, specificName) ->
        val specificTask = tasks.findByName(specificName)
        if (specificTask != null) {
            // Buscar si existe una tarea con este nombre (puede ser ambigua)
            val existingTasks = tasks.matching { it.name == genericName }
            
            if (existingTasks.isEmpty()) {
                // Si no existe, crear una nueva tarea alias
                tasks.register(genericName) {
                    group = "build"
                    description = "Ejecuta $specificName (flavor '$defaultFlavor' por defecto)"
                    dependsOn(specificTask)
                }
            } else {
                // Si existe (puede ser ambigua), intentar configurarla
                existingTasks.forEach { task ->
                    if (task != specificTask) {
                        task.dependsOn(specificTask)
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.androidx.constraintlayout)

    // VLC para reproducción de video (versión optimizada)
    implementation("org.videolan.android:libvlc-all:3.5.1")

    // Glide para cargar imágenes (solo lo necesario)
    implementation("com.github.bumptech.glide:glide:4.16.0") {
        exclude(group = "com.android.support")
    }

    // Coroutines (solo una dependencia)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel y LiveData (combinado)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Android TV (solo lo necesario)
    implementation("androidx.tv:tv-foundation:1.0.0-alpha10")

    // Seguridad - Encriptación de SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing (solo en debug)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Splash screen y Lottie
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.airbnb.android:lottie:6.1.0")
}