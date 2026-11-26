# ClassTrack (课轨) ProGuard / R8 保留规则
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class kotlinx.serialization.** { *; }
