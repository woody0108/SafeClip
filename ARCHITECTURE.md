# ARCHITECTURE.md

This document explains the top-level shape of the SafeClip repository.
Keep it factual and update it when folders or ownership boundaries change.

## Project Type

- Android application project plus separate admin web and NAS upload API projects.
- Android stack: Kotlin, Jetpack Compose, Gradle Kotlin DSL.
- Admin web stack: React, Vite, TypeScript, Firebase web SDK.
- NAS upload API stack: PHP 8.0 on Synology Web Station.
- First product target: Android submission MVP for blackbox dashcam video import and company review.

## Top-Level Structure

```text
SafeClip/
  app/          Android app module opened by Android Studio
  admin-web/    company admin web project
  nas-upload-api/ PHP upload receiver for Synology NAS Web Station
  gradle/       Gradle wrapper and version catalog for Android
  docs/         product specs, design docs, references, and execution plans
  AGENTS.md     Codex working guide
  ARCHITECTURE.md
```

## app

`app/` contains the Android application.

Current important areas:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/glass/safeclip/MainActivity.kt`
- `app/src/main/java/com/glass/safeclip/ui/theme/`
- `app/build.gradle.kts`

The app currently starts from the Android Studio Compose template.

Planned boundaries:

- UI screens: onboarding, folder picker entry, video list, video preview, submission form, consent, upload progress, submission history.
- File access: Android Storage Access Framework document tree and file URI handling.
- Media: video metadata, preview playback, thumbnail or key-frame support later.
- Submission data: user-entered incident information, consent flags, upload metadata, processing status.
- Upload/backend: Firebase first for MVP unless replaced by a custom backend.

## admin-web

`admin-web/` contains the company-side review web app.

Initial scope:

- Admin sign-in.
- Firestore `submissions` list.
- Submission detail view.
- Review status updates.
- Later photo/video viewing through Firebase Storage or a company upload server.

The admin web is not an Android Studio project. It is a separate web project kept beside the Android `app/` module so the two surfaces do not get mixed together.

## nas-upload-api

`nas-upload-api/` contains the internal company NAS upload receiver.

Initial scope:

- PHP 8.0 upload endpoint for Synology Web Station.
- Single video upload API with an upload key.
- Uploaded files stored in a NAS share outside the public web folder.
- No read, list, download, or delete API in the first MVP.

Planned boundary:

- Android upload integration can call this API later.
- Company admins retrieve uploaded files from the NAS shared folder, not from a public web URL.

## docs

`docs/` is the first reading point before significant work.

```text
docs/
  product-specs/  service intent, MVP behavior, acceptance criteria
  design-docs/    Android, backend, data flow, UI, privacy, and system decisions
  references/     proposal summaries, external docs notes, legal or API references
  exec-plans/     dated plans and work history
```

## Before Editing

1. Read `AGENTS.md`.
2. Read this file.
3. Check the relevant spec under `docs/product-specs/` or `docs/design-docs/`.
4. Check the latest execution plan under `docs/exec-plans/`.
5. Inspect target project files directly before editing.

## Known Caution

- This folder is a Git repository.
- `local.properties` is local machine configuration and should not be treated as product source.
- External SD card and USB reader access must go through Android's user-approved document access flow.
- Video upload behavior must be designed with storage cost, privacy, consent, and retry failure cases in mind.
