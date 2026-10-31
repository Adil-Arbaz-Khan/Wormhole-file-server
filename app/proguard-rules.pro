# Proguard / R8 Optimization Rules for Wormhole

# Keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * implements kotlinx.serialization.internal.GeneratedSerializer {
    *;
}
-keepclassmembers class * {
    kotlinx.serialization.descriptors.SerialDescriptor getDescriptor();
}

# Ktor Server & CIO
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn io.netty.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.**
-dontwarn org.slf4j.**

# Application Packages & Serialization DTOs
-keep class com.jarvis.fileserver.model.** { *; }
-keep class com.jarvis.fileserver.clipboard.** { *; }
-keep class com.jarvis.fileserver.server.** { *; }
-keep class com.jarvis.fileserver.service.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ZXing QR Generator
-dontwarn com.google.zxing.**
-keep class com.google.zxing.** { *; }
