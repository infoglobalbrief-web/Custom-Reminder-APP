# Personal Reminder App — Remindly
# Keep entities for Room reflection
-keep class com.remindly.app.core.data.entity.** { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
# Coroutines debug metadata
-dontwarn kotlinx.coroutines.debug.**
