# SD Card File Access

## Purpose

Record how SafeClip should access blackbox SD card or USB reader files on Android.

## Current Decision

Use Android Storage Access Framework.

The user must explicitly select a folder or storage root through Android's system picker. The app should then scan only the user-approved tree URI.

## Why This Matters

Android does not guarantee direct filesystem access to external USB storage. Storage Access Framework is safer, user-approved, and more compatible with modern Android privacy rules.

## MVP Behavior

- Start folder selection from an app button.
- Ask the user to choose the blackbox SD card root or the folder containing videos.
- Persist read permission when possible.
- Scan candidate folders and files through document APIs.
- Show clear help text if no video files are found.

## Candidate File Rules

Extensions:

- `.mp4`
- `.mov`
- `.avi`
- `.ts`

Folder hints:

- `EVENT`
- `EMERGENCY`
- `RO`
- `MOVIE`
- `PARKING`
- `NORMAL`

## Failure Cases

- User cancels folder selection.
- SD card is removed.
- Folder permission is revoked.
- Files are encrypted or app cannot read them.
- Files use unsupported codecs.
- File metadata is missing or inaccurate.

## Related Files

- `docs/product-specs/submission-mvp.md`
- `docs/design-docs/android-app-architecture.md`
