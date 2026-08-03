# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep model classes used by Gson/Retrofit
-keep class com.mechanicai.pro.data.model.** { *; }
-keep class com.mechanicai.pro.domain.model.** { *; }

# Keep Hilt generated classes
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* <methods>;
    @dagger.* <methods>;
    <init>(...);
}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
