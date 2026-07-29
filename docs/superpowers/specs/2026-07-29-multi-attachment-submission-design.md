# Multi-Attachment Submission Design

## Goal

SafeClip Android submission should allow one submission to include up to 2 video files and up to 5 JPEG photos. The app should keep the current one-file entry flow, then let the user add or remove eligible files before submitting.

## Scope

- Change Android app submission behavior under `app/`.
- Keep company web and NAS upload API unchanged for this slice.
- Preserve existing Firestore representative file fields for compatibility.
- Add structured attachment metadata so later NAS upload work can attach upload paths per file.

## User Flow

1. User selects a video or JPEG file from the current folder or SafeClip saved folder.
2. App opens the submission form with that file already attached.
3. Submission form shows current attachment counts: videos `0..2`, photos `0..5`.
4. User can add eligible files from the same available file list.
5. User can remove secondary attachments.
6. App blocks additional selection when video or photo limits are reached.
7. User submits the incident form and consent items.

## Attachment Rules

- Video files count toward `MAX_VIDEO_ATTACHMENTS = 2`.
- JPEG files count toward `MAX_PHOTO_ATTACHMENTS = 5`.
- Other files are not submittable.
- Duplicate URI attachments are ignored.
- The first selected file remains the representative file for legacy fields:
  - `sourceUri`
  - `originalFileName`
  - `fileSizeBytes`
  - `originalFolderPath`
  - `originalLastModifiedMillis`

## Firestore Shape

Keep existing fields and add:

```text
attachments: [
  {
    uriString,
    displayName,
    mimeType,
    sizeBytes,
    folderPath,
    kind
  }
]
videoCount
photoCount
```

`kind` is either `video` or `photo`.

## Error Handling

- If the user tries to exceed a limit, keep the previous selection and show a Korean message.
- If a duplicate is selected, keep the previous selection and show a Korean message.
- If no valid attachment exists, submission is disabled.

## Testing

- Unit-test attachment limit and duplicate behavior with real Kotlin models.
- Unit-test Firestore attachment field creation.
- Keep tests focused on app-owned behavior, not Firebase internals.
