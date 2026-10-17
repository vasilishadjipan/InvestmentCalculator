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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# WorkManager and Room ProGuard Rules
-keep class androidx.work.impl.WorkDatabase { *; }
-keep class androidx.room.** { *; }
-keep class androidx.work.Worker { *; }
-keep class androidx.work.ListenableWorker { *; }
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy* { *; }
-keep class androidx.work.impl.background.systemjob.SystemJobService { *; }
-keep class androidx.work.impl.workers.ConstraintTrackingWorker { *; }
-keep class androidx.work.impl.foreground.SystemForegroundService { *; }
-keep public class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod