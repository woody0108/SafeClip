# SafeClip Admin Web Architecture

## Purpose

Keep the company review web app separate from the Android app while sharing the same Firebase project and repository documentation.

## Current Decision

Use a sibling folder at the repository root:

```text
SafeClip/
  app/        Android app module opened by Android Studio
  admin-web/ Company admin web project
  docs/       Shared product and technical docs
```

Do not move the Android `app/` module for now. Android Studio and Gradle already expect it at `SafeClip/app`, so moving it would add risk without helping the first admin-web MVP.

## Admin Web MVP

1. Admin sign-in.
2. Read Firestore `submissions`.
3. Show submission list and detail.
4. Update review status.
5. Later, add original photo/video access through Firebase Storage or a company upload server.

## Data Boundary

The Android app writes submission metadata to Firestore. Android local document URIs cannot be opened directly by a remote web browser, so file viewing requires a real upload destination later.

