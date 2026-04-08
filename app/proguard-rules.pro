# Add project specific ProGuard rules here.

# ── Gson / DataStore serialized models ───────────────────────────────────────
# Profile and PlannerEntry are serialized to JSON with Gson.
# R8 must NOT rename their fields, otherwise deserialization silently fails.
-keep class com.mathieu.blocker.data.Profile { *; }
-keep class com.mathieu.blocker.data.PlannerEntry { *; }

# Gson internals
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ── Room ─────────────────────────────────────────────────────────────────────
-keep class com.mathieu.blocker.data.db.** { *; }

# ── Stack traces (optional but useful for crash reports) ─────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile