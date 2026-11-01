# Toolbox app map

Cached map for the app-guide-illustrator skill. Reuse it for new guides; re-scan
only when the UI changes. Drawn from the code (Jetpack Compose), not a running app.

- **Frame:** `phone` (Android, Compose, Material 3)
- **Style:** `sketch` (hand-drawn, app colours). **Mascot:** `xiaohei` (credit in guide.json)
- **Font:** `ui_font` = `../../../app/src/main/res/font/plus_jakarta_sans_regular.ttf`
  (relative to `specs/`; bold, semibold, medium and extrabold sit beside it and are picked up automatically)
- **Sources:** `app/src/main/kotlin/com/mabsSD/toolbox/ui/` (ToolboxApp.kt = nav shell,
  screens/*.kt, components/Chrome.kt, theme/Color.kt, Shape.kt)

## Theme (paste into specs)

```json
"theme": {"primary": "#3B5BDB", "on_primary": "#FFFFFF", "background": "#F7F5F2", "surface": "#FFFFFF",
          "text": "#111827", "muted": "#6B7280", "border": "#E5E7EB", "accent": "#F59E0B", "danger": "#DC2626",
          "success": "#16A34A", "info": "#0D9488", "radius": 18, "button_radius": 14,
          "selected": "#CCFBF1", "on_selected": "#1E3A8A"}
```

Accent (amber, tertiary) is the camera FAB. Selected chips, segmented buttons and the
active nav pill use the teal container.

## Navigation

Bottom nav: **Home** (home) · **Files** (folder) · **Tools** (build) · **Settings** (settings).
Amber camera FAB on Home and Files opens Scan. Back arrows on every tool screen.

## Screens (top to bottom, exact labels)

- **Onboarding** (first launch, 3 pages, "Skip" top right, dots, "Next" / "Get started"):
  1. "Scan anything", "Turn paper into clean, readable PDFs with your camera."
  2. "Edit offline", "Merge, split, compress, and pull text out of documents without an internet connection."
  3. "Privacy first", "Toolbox has no INTERNET permission. Your files never leave your device — it isn't possible."
- **Home:** "Toolbox", pill "Offline • Private", button "Scan document", section "Quick tools"
  (Split "Extract pages", Merge "Combine PDFs", Compress "Hit a size limit", OCR "Copy text out"),
  section "Recent" + "See all" (file cards, or an empty state), camera FAB.
- **Scan:** "Scan to PDF", intro text, "Start scanning". After capture: "Pages (N)", filter chips
  "Original" / "Grayscale" / "B&W", rotate and delete icons, page thumbnails (retake icon),
  "Export PDF (N pages)" ("(1 page)" when single).
- **Merge:** "Choose PDFs" → "N files — output order", rows with up / down / remove,
  "Merge N files", "Add more" (tonal). Output `<stem>_merged_N.pdf`.
- **Split:** "Choose a PDF" → DOCUMENT card, "Pages to extract" (placeholder "e.g. 1-3, 5"),
  helper "Pages 1-3, 5 (4 pages)", "Extract pages", "Choose another PDF". Output `<stem>_p1-3_5.pdf`.
- **Compress:** "Shrink a PDF or image toward a target size. Quality is never pushed past the point
  where text stops being readable." "Choose a PDF" / "Choose an image" → FILE card, TARGET SIZE chips
  1 MB / 2 MB / 5 MB, "Custom size, in MB", "Compress", "Choose another file".
  Result note "X% smaller — A → B". Output `<stem>_compressed`.
- **OCR** (ToolScreen): "Recognise text in a photo and copy or share it", "Step 1",
  "Select a file, then run the tool.", "Pick an image". Output `<stem>_text.txt`.
- **Result:** "Result", "DONE — size", file card, preview, "Save to…", "Share" / "Open";
  OCR results show the text and "Copy text".
- **Files:** "Files", search "Search files", sort chips "Date" / "Name" / "Size", cards
  "Tool · size · date" with ⋮ menu "Share" / "Delete" (red). Dialog "Delete this file?",
  "Cancel" / "Delete" (red text).
- **Incoming** (share / open with): "Open with Toolbox", "1 FILE", "WHAT DO YOU WANT TO DO?",
  PDF choices Split "Pull selected pages out into a new PDF", Compress "Shrink it under a size limit",
  Merge "Join it with other PDFs". Images get Scan, OCR and Compress.
- **Settings:** APPEARANCE segmented "System" / "Light" / "Dark"; PRIVACY text;
  ABOUT "Toolbox", "Version 1.0.0", "Set in Plus Jakarta Sans (SIL Open Font License)."

## Sample data (made up)

Contract-2026.pdf (12 pages), Payslip-Oct.pdf (4.8 MB), Invoice-March.pdf, receipt photos.
No real files or people.

## Guides made (October 2026)

00 cover · 01 getting started · 02 home tour · 03 scan · 04 merge · 05 split · 06 compress ·
07 OCR · 08 files · 09 open with · 10 settings. Specs in `specs/`, generated art in `images/`.
