# Releasing Toolbox

## Signing

Release builds are signed with an **upload key** when `keystore.properties`
exists in the project root (it is git-ignored):

```properties
storeFile=/absolute/path/to/toolbox-upload.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

Without that file, `assembleRelease` produces an unsigned APK — which is what CI
builds. Google Play re-signs uploads with its own app signing key (Play App
Signing), so a lost upload key can be reset from the Play Console; it is still
much easier to keep a backup of the `.jks` and its password somewhere safe
(a password manager, not this repository).

```bash
./gradlew bundleRelease      # app/build/outputs/bundle/release/app-release.aab → upload to Play
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk  → install on a test phone
```

## Device test pass (release build)

Run on the **release** APK — R8 shrinks and renames code there, and that is what
users get. Install with `adb install -r app/build/outputs/apk/release/app-release.apk`.
A debug build already installed must be uninstalled first (different signature).

- [ ] Fresh install → onboarding → Home
- [ ] **Scan with the camera** (not just gallery import): 2+ pages, B&W, rotate, delete, export
- [ ] Scan → Add pages → export
- [ ] Split a multi-page PDF by range
- [ ] Merge 2+ PDFs, reorder before merging
- [ ] Compress a large PDF (target met) and a small one (original kept)
- [ ] Compress an image
- [ ] OCR an image → Copy text
- [ ] Result: Open, Share, Save to… (correct file name)
- [ ] Files tab: search, sort, share, delete
- [ ] Settings: Light / Dark / System, persists after restart
- [ ] **Share to Toolbox** from Files / Gallery:
  - [ ] one PDF → chooser offers Split, Compress, Merge
  - [ ] several PDFs → Merge, files pre-filled
  - [ ] one image → Scan, OCR, Compress (OCR runs straight away)
  - [ ] several images → Scan, pages pre-filled
  - [ ] a PDF and an image together → "share them separately" message
- [ ] **Open with → Toolbox** on a PDF from a file manager
- [ ] Password-protected PDF → clear error, no crash
- [ ] Rotate the phone on the chooser and on a tool screen
- [ ] Themed icons on (Android 13+): launcher icon follows the wallpaper colour

## Play Console

- [ ] App content → Privacy policy URL:
      `https://github.com/Tshepomabs16/Toolbox/blob/main/PRIVACY.md`
- [ ] Data safety: no data collected, no data shared
- [ ] Content rating questionnaire, target audience, ads: none
- [ ] Store listing: text and graphics in [`fastlane/metadata/android/en-US`](../fastlane/metadata/android/en-US)
- [ ] Closed test: personal accounts created after Nov 2023 need **12 testers opted
      in for 14 consecutive days** before production access
- [ ] Bump `versionCode` in `app/build.gradle.kts` for every upload after the first
