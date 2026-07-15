# AGENTS.md

This is the entry guide for Codex work on SafeClip.
Keep this file short. Put details in the linked docs instead.

## Start Here

1. Read `ARCHITECTURE.md` for the top-level project shape.
2. Check `docs/product-specs/` for service intent and MVP behavior.
3. Check `docs/design-docs/` for Android, backend, data flow, and UI decisions.
4. Check the latest file in `docs/exec-plans/` before changing code.
5. Use `docs/references/` before repeating research from the proposal or external sources.

## Project

- SafeClip is an Android-first service for importing blackbox dashcam videos from a USB-C microSD reader, previewing them, and submitting them for company review.
- The first MVP is the submission flow, not phone-based continuous recording.
- The user decides business direction, operating policy, and final UX.
- Codex designs systems, writes code, fixes bugs, and explains changes.
- Codex does the coding work; the user performs final review.

## Working Rules

- Make small, reviewable changes.
- Check existing files before editing.
- Discuss plans before large structure or behavior changes.
- If a requested change is likely to tangle the code, grow scope too much, or make future work harder, tell the user before coding and recommend a simpler alternative.
- Do not overwrite user changes without permission.
- Keep unfinished work easy to hand off through `docs/exec-plans/`.

## Code Rules

- Keep code simple and readable for a junior developer.
- Add short Korean comments where Android or backend behavior would be hard to follow.
- Prefer clear names over clever code.
- Avoid deep abstraction until real duplication or complexity appears.
- Separate UI, app state, file access, upload, submission data, and backend integration.

## Android Rules

- Build the app with Kotlin and Jetpack Compose unless the project direction changes.
- Use Android Storage Access Framework for SD card and USB reader folder/file access.
- Do not assume direct filesystem paths for external storage.
- Treat large video files carefully: show upload progress, support retry, and avoid loading entire files into memory.
- Keep dangerous or privacy-sensitive actions explicit and consent-based.

## Product Rules

- Initial MVP focuses on blackbox video import and submission.
- Phone-mounted continuous recording is a later experiment, not part of MVP 1.
- Legal, privacy, location, evidence handling, and reporting flow decisions must be documented before implementation.
- Early reporting flow should prefer "company prepares report package, user performs final submission" until legal review says otherwise.

## Avoid

- Do not treat `AGENTS.md` as a full spec.
- Do not implement phone camera recording as part of MVP 1.
- Do not silently add AI judgment or automatic legal reporting behavior.
- Do not store or upload videos without explicit user action and consent.
- Do not make broad refactors while solving a narrow task.

## After Coding

- Summarize only the core changes.
- Explain important logic in beginner-friendly Korean.
- Mention changed files and the next useful step.
