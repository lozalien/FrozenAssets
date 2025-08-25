# Keep AdMob classes and their dependencies
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.internal.ads.** { *; }

# Keep the missing classes that are causing the error
-dontwarn android.media.LoudnessCodecController
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener

# Keep necessary Android libraries
-keep class android.** { *; }
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }

# Keep Room database classes
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep your app's model classes
-keep class com.frozenassets.app.models.** { *; }