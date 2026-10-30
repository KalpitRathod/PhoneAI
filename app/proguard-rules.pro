# MediaPipe Tasks GenAI ProGuard rules
-keep class com.google.mediapipe.tasks.genai.** { *; }
-keep class com.google.mediapipe.framework.** { *; }
-keep class com.google.mediapipe.tasks.core.** { *; }

# Keep Kotlin coroutines and reflection if used
-keepnames class kotlinx.coroutines.** { *; }
