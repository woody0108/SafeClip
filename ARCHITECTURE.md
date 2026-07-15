# ARCHITECTURE.md

This document explains the top-level shape of the SafeClip repository.
Keep it factual and update it when folders or ownership boundaries change.

## Project Type

- Android application project.
- Current stack: Kotlin, Jetpack Compose, Gradle Kotlin DSL.
- First product target: Android submission MVP for blackbox dashcam video import and upload.

## Top-Level Structure

```text
SafeClip/
  app/          Android app module
  gradle/       Gradle wrapper and version catalog
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

- This folder is not currently a Git repository.
- `local.properties` is local machine configuration and should not be treated as product source.
- External SD card and USB reader access must go through Android's user-approved document access flow.
- Video upload behavior must be designed with storage cost, privacy, consent, and retry failure cases in mind.
