# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep model classes used by Gson, Retrofit, and Firestore
-keep class com.mechanicai.pro.data.model.** { *; }
-keep class com.mechanicai.pro.domain.model.** { *; }
-keepclassmembers class com.mechanicai.pro.data.model.** { *; }

# Retrofit / OkHttp / Gson
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }

# Hilt / Dagger / JSR-330
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewComponentBuilderEntryPoint
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* <methods>;
    @dagger.* <methods>;
    <init>(...);
}

# Firebase, Play services, Play Billing
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.android.billingclient.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.android.billingclient.**

# Credential Manager / Google Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Compose / coroutines
-dontwarn org.jetbrains.kotlin.**
-keepclassmembers class **.R$* {
    public static <fields>;
}
