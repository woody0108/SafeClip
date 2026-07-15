# Submission MVP

## Goal

Build the first SafeClip app flow that lets a user select a blackbox video from an SD card or USB reader, preview it, submit it with incident information and consent, and later check processing status.

## Recommended First Backend

Use Firebase for MVP speed:

- Firebase Authentication for basic user identity.
- Firebase Storage for video upload.
- Firestore for submission metadata and status.

This can be replaced later by a custom backend if storage cost, review workflow, or data-control needs require it.

## User Flow

1. Open app.
2. Read short onboarding message explaining that MVP uses existing blackbox videos.
3. Tap "SD card folder select".
4. Android folder picker opens.
5. User grants access to the SD card or blackbox video folder.
6. App scans likely video files and sorts recent files first.
7. User opens a video preview.
8. User selects one primary video for submission.
9. User enters incident date/time, location note, violation type candidate, and optional memo.
10. User reviews consent items.
11. User uploads the submission.
12. App shows upload progress.
13. App shows submission status after upload completes.

## Video Candidate Rules

MVP search should be simple and conservative.

Candidate extensions:

- `.mp4`
- `.mov`
- `.avi`
- `.ts`

Preferred folders when discoverable:

- `EVENT`
- `EMERGENCY`
- `RO`
- `MOVIE`
- `PARKING`
- `NORMAL`

Sort order:

1. Last modified time descending.
2. Created time if available.
3. File name descending as fallback.

## Submission Fields

Required:

- selected video URI
- original file name
- file size
- incident date/time
- incident location text
- violation type candidate
- user memo
- report-review consent
- video-storage consent
- traffic-risk data-use consent

Optional for MVP:

- blackbox brand
- vehicle plate of reported vehicle
- front/rear video relationship
- GPS location

## Submission Status

Use these statuses first:

- `uploading`
- `waiting_review`
- `reviewing`
- `needs_more_info`
- `report_package_ready`
- `rejected`
- `completed`

User-facing Korean labels:

- 업로드중
- 검토대기
- 검토중
- 자료보완필요
- 신고자료준비완료
- 반려
- 완료

## Acceptance Criteria

- A user can select a folder through Android's system picker.
- The app can list recent video candidates from the selected folder.
- The app can preview at least common MP4 files.
- The app can upload a selected video with metadata.
- The app shows upload progress or a clear uploading state.
- A completed upload creates a submission record.
- The user can see the submission and its status after returning to the app.

## Out Of Scope

- Automatic front/rear pairing.
- Automatic number-plate OCR.
- Automatic violation judgment.
- Payment, ads, subscription, and premium report.
- Admin web UI implementation in the first Android slice.
