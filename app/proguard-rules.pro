# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Room
-keep class androidx.room.** { *; }

# NewPipeExtractor + Rhino/JSoup transitive deps
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn org.jsoup.**
-keep class org.jsoup.** { *; }

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses

-keepclassmembers class * {
    @androidx.room.* <methods>;
}
