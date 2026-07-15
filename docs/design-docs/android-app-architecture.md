# Android App Architecture

## Purpose

Define the first Android architecture for the SafeClip submission MVP.

## Current Decision

Use a simple Kotlin + Jetpack Compose app with clearly separated feature boundaries:

```text
ui/
  onboarding/
  picker/
  videoList/
  videoPreview/
  submission/
  history/

data/
  file/
  media/
  submission/
  upload/

domain/
  model/
  usecase/
```

The exact package split can be smaller at first, but the code should keep these responsibilities separate.

## Main Screens

- Onboarding screen: explains blackbox SD card workflow.
- Folder picker entry screen: starts Android's system folder picker.
- Video list screen: shows recent candidate files.
- Video preview screen: plays selected video.
- Submission form screen: collects incident info and consent.
- Upload progress screen or state: shows upload progress and retry.
- Submission history screen: shows current processing status.

## Data Flow

1. UI starts folder picker.
2. Android returns a tree URI.
3. App persists read permission for that tree URI.
4. File scanner reads document children through `DocumentFile` or equivalent SAF APIs.
5. Media metadata service extracts safe metadata where possible.
6. User selects one video.
7. Submission form creates a local draft.
8. Upload repository uploads the video stream.
9. Submission repository writes metadata and status.
10. History screen observes submission records.

## Important Constraints

- Do not assume normal filesystem paths for USB reader files.
- Do not load full videos into memory.
- Do not upload until the user explicitly confirms consent.
- Persisted URI permission can disappear if user revokes access, so the app must handle missing access cleanly.
- Some videos may not preview because of codec limitations; the app should show a clear unsupported-format message.

## Testing Direction

- Unit test file filtering and sorting rules with fake video records.
- Unit test submission status mapping.
- Instrumented test the main Compose navigation once UI exists.
- Manual test with real USB-C microSD reader and at least a few blackbox file samples.

## Open Questions

- Whether to use a single-activity Compose navigation setup immediately.
- Whether to add Room for local draft/submission cache in MVP 1 or rely on Firebase records first.
- Which media playback library should be used first: platform `VideoView` wrapper, Media3, or a lighter preview approach.
