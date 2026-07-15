# Backend And Upload Direction

## Purpose

Define the first backend direction for the SafeClip submission MVP.

## Current Decision

Use Firebase first unless the user later decides to invest in a custom backend before MVP.

MVP services:

- Firebase Authentication
- Firebase Storage
- Firestore

## Submission Record

Initial fields:

```text
id
userId
status
originalFileName
fileSizeBytes
storagePath
incidentDateTime
incidentLocationText
violationTypeCandidate
userMemo
blackboxBrand
reportReviewConsent
videoStorageConsent
trafficRiskDataConsent
createdAt
updatedAt
```

## Storage Path Direction

Use a predictable but non-public path:

```text
submissions/{userId}/{submissionId}/{originalFileName}
```

Security rules must prevent other users from reading the uploaded file.

## Upload UX Requirements

- Show upload progress.
- Warn that large videos may take time.
- Recommend Wi-Fi for large files.
- Allow retry after failure.
- Do not create a final `waiting_review` status until upload succeeds.

## Privacy Requirements

- Consent must be stored as explicit boolean fields.
- Data-use consent must be separate from report-review consent.
- Raw video retention policy must be decided before production launch.
- Admin access must be restricted and audited before real users are accepted.

## Future Custom Backend Reasons

Move away from Firebase if:

- video storage cost becomes too high,
- admin review workflow needs complex server processing,
- signed upload URLs are needed,
- AI analysis pipeline requires queue workers,
- legal/data governance requires stricter backend control.
