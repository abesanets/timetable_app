# Add project specific ProGuard rules here.
# By default, the rules in this file are only applied to Release builds.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Keep models from obfuscation (crucial for Gson)
-keep class com.example.schedule.data.models.** { *; }
-keepclassmembers class com.example.schedule.data.models.** { *; }
