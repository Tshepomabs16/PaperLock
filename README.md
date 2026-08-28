# Toolbox

An Android document utility that works entirely on your phone.

**No account. No upload. No watermark. No ads.**

## The Hard Rule

**Nothing leaves the device.**

Every free scanner uploads your ID documents to somebody's server. Toolbox declares **no `INTERNET` permission at all**, and CI fails the build if the merged manifest ever contains one.

- We physically cannot see your files
- No analytics, no crash reporting, no remote config
- Your documents stay on your device

## Features

### v1 (MVP)
- **Scan to B&W PDF** — camera capture, edge crop, threshold filter, multi-page export
- **Split & merge PDFs** — page ranges, extract, reorder, rotate, delete
- **Compress to target size** — "under 2 MB", the thing every upload portal demands
- **OCR** — copy text from images, create searchable PDFs (on-device)
- **Share-sheet entry** — start from any other app

### Round 2
- Video → audio extraction
- Documents → PDF conversion
- Sign and protect PDFs
- QR scan and generate
- Batch mode

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- ML Kit Document Scanner
- ML Kit Text Recognition v2 (bundled)
- PdfBox-Android
- WorkManager

## Development

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Check for INTERNET permission
grep -r "android.permission.INTERNET" app/src/
```

## License

Private project.
