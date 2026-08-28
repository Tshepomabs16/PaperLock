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

# We deliberately exclude Google's datatransport (telemetry) library to
# guarantee no network access. ML Kit's bundled code still references it at
# the bytecode level, so silence the R8 warnings for those unreachable paths.
-dontwarn com.google.android.datatransport.**
