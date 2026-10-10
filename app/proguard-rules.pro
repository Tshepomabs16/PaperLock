# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep PDFBox classes
-keep class org.apache.pdfbox.** { *; }
-keep class com.tom_roush.pdfbox.** { *; }

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }

# PDFBox optionally references a JPEG2000 (JP2) decoder that is not bundled.
# PDFs we produce do not use JPX image encoding, so these are safe to drop.
-dontwarn com.gemalto.jp2.**

# ML Kit references Google's datatransport (telemetry) library. It ships in the
# app (excluding it crashes the scanner), but with no INTERNET permission it
# can never send anything. Silence R8 warnings about its optional paths.
-dontwarn com.google.android.datatransport.**
