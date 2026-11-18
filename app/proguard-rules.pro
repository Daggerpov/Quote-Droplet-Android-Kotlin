# R8 rules for the release build (minify + resource shrinking are on in app/build.gradle.kts).

# Readable crash stack traces in Play Console (the mapping file is uploaded with each release)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Generic signatures + annotations are needed by Retrofit and kotlinx.serialization at runtime
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# --- Retrofit -------------------------------------------------------------------------------------
-keep interface com.danielagapov.quotedroplet.data.QuoteApi { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**

# --- kotlinx.serialization (API models) -----------------------------------------------------------
-keepclassmembers @kotlinx.serialization.Serializable class com.danielagapov.quotedroplet.data.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.danielagapov.quotedroplet.data.**$$serializer { *; }
-keepclasseswithmembers class com.danielagapov.quotedroplet.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Glance widget actions are created by class name ------------------------------------------------
-keep class com.danielagapov.quotedroplet.widget.** implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

# --- OkHttp ----------------------------------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
