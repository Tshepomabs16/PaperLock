# PaperLock Privacy Policy

_Last updated: 2026-10-30_

PaperLock is a document utility that works entirely on your device.

## The short version

- PaperLock has no `INTERNET` permission. It cannot send your files anywhere,
  because it has no way to reach a network at all. This is enforced by the
  Android operating system itself, not by a setting you have to trust.
- Splitting, merging, compressing, and text recognition happen on your
  device. The text-recognition model is bundled inside the app.
- PaperLock does not have an account system, a login, or a cloud service of any
  kind to send data to even if it wanted to.

## What PaperLock does with your files

Every operation you run — scanning a document, splitting a PDF, merging
files, compressing, or recognising text — reads the files you choose, using
Android's Storage Access Framework, and writes its output back to storage you
choose. PaperLock does not scan your device for other files and does not access
anything you have not explicitly selected.

You can also share a PDF or image to PaperLock from another app, or open one
with it. PaperLock copies that file into its own private storage so the tool you
pick can work on it, and deletes the copy automatically within a day.

PaperLock keeps a local, on-device history of files you have processed (name,
size, and when it was created) so you can find them again from the Files tab.
This history never leaves your device. You can delete any entry, and its file,
from the Files tab; everything is removed if you uninstall the app.

## What PaperLock does not do

- PaperLock does not collect analytics, crash reports, or usage data.
- PaperLock does not use advertising or advertising identifiers.
- PaperLock does not require an account, and has nowhere to send one if it did.
- PaperLock does not request camera, contacts, location, microphone, or storage
  permissions. It reads only the files you explicitly select.

## Scanning

PaperLock does not request camera permission itself. The Scan feature uses the
document scanner provided by Google Play services on your device, which shows
its own camera screen and returns only the pages you capture to PaperLock.
PaperLock then processes those pages on your device.

## Changes to this policy

If PaperLock ever changes what it does with your data, this policy will be
updated before that change ships, and the version history will remain
visible in the app's source repository.

## Contact

Questions about this policy can be sent to the contact address listed on
PaperLock's Play Store listing.
