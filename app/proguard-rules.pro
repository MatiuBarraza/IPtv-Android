# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Reglas específicas para optimizar el tamaño del APK

# Mantener clases de VLC necesarias
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.libvlc.util.** { *; }

# Mantener clases de Glide necesarias
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Mantener clases de Lottie
-keep class com.airbnb.lottie.** { *; }

# Mantener clases de ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static * inflate(android.view.LayoutInflater);
    public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}

# Optimizaciones generales
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Remover logs en release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Mantener solo las clases necesarias de Android TV
-keep class androidx.tv.** { *; }

# Mantener clases de seguridad
-keep class androidx.security.** { *; }