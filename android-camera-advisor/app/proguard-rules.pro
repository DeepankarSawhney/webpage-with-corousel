# TensorFlow Lite loads its interpreter/delegate classes via reflection.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
