# SafeClip Admin Web

Company-side admin web for reviewing SafeClip submissions.

This project is intentionally separate from the Android app module:

- Android app: `../app`
- Admin web: `./admin-web`

## First MVP

- Admin sign-in
- Firestore `submissions` list
- Submission detail view
- Submitted metadata review
- Status update flow
- Later: original photo/video access through Firebase Storage or a company upload server

## Firebase Data Used First

- `users/{uid}`
- `submissions/{submissionId}`

The Android app currently writes submission metadata to Firestore. Direct photo/video viewing on the web will need a real upload destination later, because local Android document URIs cannot be opened from a company web browser.

## Local Setup

Create `.env.local` from `.env.example`, then run:

```bash
npm install
npm run dev
```

