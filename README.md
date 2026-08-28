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

Requires **JDK 17–21**. AGP 8.7 rejects JDK 25 with a bare `What went wrong: 25.0.3`,
which is easy to mistake for a corrupt build. Android Studio uses its own bundled JDK,
so a build can succeed in the IDE and fail from the terminal on the same machine.

```bash
# Build (release included — the hard-rule check needs its merged manifest)
./gradlew assembleDebug assembleRelease

# Run tests
./gradlew testDebugUnitTest

# Verify the hard rule (run after a build)
./scripts/check-no-internet.sh
```

### How the hard rule is enforced

ML Kit and Play services pull `INTERNET` in transitively, so the app manifest strips it
with `tools:node="remove"` and the ML Kit dependencies exclude
`com.google.android.datatransport`. The merged release manifest ends up with only
`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE`, all from WorkManager.

[`scripts/check-no-internet.sh`](scripts/check-no-internet.sh) is the guardrail, run by CI
on every push. It fails if the source manifest stops stripping `INTERNET`, if any merged
manifest grants it, **or if no merged manifest is found at all** — an unverifiable build is
treated as a failure rather than a pass, so the check cannot go green without having
actually checked something.

## License

Private project.
