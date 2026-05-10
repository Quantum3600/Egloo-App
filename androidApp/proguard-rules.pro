# Keep DTO and network layer classes unobfuscated and unshrunk.
# This helps serialization, reflection, and API mapping remain stable.
-keep class com.trishit.egloo.data.api.** { *; }
-keep class com.trishit.egloo.data.repositories.*Ktor* { *; }
-keep class com.trishit.egloo.data.repositories.*Network* { *; }
-keep class com.trishit.egloo.data.api.**Dto { *; }

# Keep kotlinx serialization metadata used by @Serializable classes.
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep Compose-specific classes and annotations
-keepclassmembers class  ** {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Coroutines and internal state
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}

# Suppress missing errorprone annotation warnings reported in missing_rules.txt
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

