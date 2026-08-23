# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Firebase Firestore uses reflection to (de)serialize model POJOs via
# toObject()/toObjects(). Keep all model classes and their members so field
# names are not obfuscated.
# ---------------------------------------------------------------------------
-keep class com.tanim.ccepedia.** { *; }
-keepclassmembers class com.tanim.ccepedia.** {
    *;
}
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Firebase / Google Play services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# AndroidPdfViewer (barteksc) / PdfiumAndroid
-keep class com.shockwave.** { *; }
-dontwarn com.shockwave.**