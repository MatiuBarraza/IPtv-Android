plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.iptvcpruebadesdecero"
    compileSdk = 35  // Android 15+ (preparado para Android 16)

    defaultConfig {
        applicationId = "com.example.iptvcpruebadesdecero"
<<<<<<< Updated upstream
        minSdk = 29  // Android 10+ (dispositivos desde 2019) - Máxima compatibilidad
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
=======
        minSdk = 21  // Android 5.0+ - Compatibilidad con todas las versiones
        targetSdk = 35  // Android 15+ (preparado para Android 16)
        versionCode = 2
        versionName = "1.1"
>>>>>>> Stashed changes

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

<<<<<<< Updated upstream
=======
    flavorDimensions += "version"

    // NOTA: Cuando hay productFlavors, debes especificar el flavor al ejecutar tareas:
    // - En Android Studio: Build Variants → selecciona el flavor deseado
    // - En línea de comandos: usa tareas específicas como assembleFullDebug, assembleNewDevicesDebug, etc.
    productFlavors {
        create("full") {
            dimension = "version"
            applicationIdSuffix = ".full"
            versionNameSuffix = "-full"
            minSdk = 21  // Android 5.0+ - Máxima compatibilidad
            targetSdk = 35  // Android 15+ (preparado para Android 16)
        }

        create("newDevices") {
            dimension = "version"
            applicationIdSuffix = ".new"
            versionNameSuffix = "-new"
            minSdk = 29  // Android 10+
            targetSdk = 35  // Android 15+ (preparado para Android 16)
            ndk {
                abiFilters += "arm64-v8a"
            }
        }

        create("oldDevices") {
            dimension = "version"
            applicationIdSuffix = ".old"
            versionNameSuffix = "-old"
            minSdk = 21  // Android 5.0+
            targetSdk = 35  // Actualizado para compatibilidad moderna
            ndk {
                abiFilters += "armeabi-v7a"
            }
        }
    }

>>>>>>> Stashed changes
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
            isRenderscriptDebuggable = false
            isPseudoLocalesEnabled = false
            isZipAlignEnabled = true
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
    
<<<<<<< Updated upstream
    // Splits por ABI solo para dispositivos reales
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")  // Solo dispositivos reales
            isUniversalApk = false
=======
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
    val capitalizedFlavor = defaultFlavor.replaceFirstChar { it.uppercaseChar() }
    val taskMappings = mapOf(
        "assembleDebug" to "assemble${capitalizedFlavor}Debug",
        "assembleRelease" to "assemble${capitalizedFlavor}Release",
        "assembleDebugUnitTest" to "assemble${capitalizedFlavor}DebugUnitTest",
        "assembleDebugAndroidTest" to "assemble${capitalizedFlavor}DebugAndroidTest",
        "testDebugUnitTest" to "test${capitalizedFlavor}DebugUnitTest",
        "connectedDebugAndroidTest" to "connected${capitalizedFlavor}DebugAndroidTest",
        "installDebug" to "install${capitalizedFlavor}Debug",
        "installRelease" to "install${capitalizedFlavor}Release",
        "uninstallDebug" to "uninstall${capitalizedFlavor}Debug",
        "uninstallRelease" to "uninstall${capitalizedFlavor}Release"
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
>>>>>>> Stashed changes
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // VLC para reproducción de video (versión estable y compatible con SDK 35)
    implementation("org.videolan.android:libvlc-all:3.5.1")

    // Glide para cargar imágenes (compatible con SDK 35)
    implementation("com.github.bumptech.glide:glide:4.16.0") {
        exclude(group = "com.android.support")
    }

    // Coroutines (actualizada para compatibilidad)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel y LiveData (actualizado)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")

    // Android TV (actualizado)
    implementation("androidx.tv:tv-foundation:1.0.0-alpha12")

    // Seguridad - Encriptación de SharedPreferences (actualizado)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing (actualizado)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // Splash screen y Lottie (actualizado)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.airbnb.android:lottie:6.4.1")
}