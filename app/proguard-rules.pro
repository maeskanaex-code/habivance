# ============================================================
# Habivance ProGuard / R8 rules
# ============================================================

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.habivance.**$$serializer { *; }
-keepclassmembers class app.habivance.** {
    *** Companion;
}
-keepclasseswithmembers class app.habivance.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep WorkManager workers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keepclassmembers class app.habivance.notification.ReminderWorker { *; }

# Keep our data classes referenced by name
-keep class app.habivance.data.backup.** { *; }
-keep class app.habivance.domain.model.** { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
