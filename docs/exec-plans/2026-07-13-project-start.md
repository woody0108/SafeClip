# 2026-07-13 Project Start

## Goal

Start SafeClip as an Android-first blackbox video submission MVP.

## Current Input

User-provided direction:

- Android Studio project has already been created.
- Use a documentation style similar to `C:\Users\win11\GitHub\BlendMaple\REPO ROOT`.
- Build the app based on the SafeClip business proposal.
- MVP target selected by user: submission MVP, meaning video selection, consent, upload, and submission status.

Proposal direction:

- Prioritize blackbox SD card / USB-C reader integration.
- Do not start with phone-mounted continuous recording.
- Start with Android.
- Core flow: choose SD card folder, find recent videos, preview, submit, and check status.

## Initial Plan

1. Create BlendMaple-style documentation structure.
2. Record SafeClip core service concept.
3. Record the submission MVP product spec.
4. Record Android architecture direction.
5. Record SD card file access direction.
6. Record backend/upload direction.
7. Use these docs as the base before implementation planning.

## Current MVP Decision

Use the submission MVP:

1. SD card / USB reader folder selection.
2. Recent blackbox video candidate list.
3. Video preview.
4. Incident information entry.
5. Separated consent items.
6. Video upload.
7. Submission history and processing status.

## Recommended Technical Direction

- Android: Kotlin + Jetpack Compose.
- File access: Android Storage Access Framework.
- Backend for MVP: Firebase Authentication, Firebase Storage, Firestore.
- Later backend option: custom API/server if storage cost, review workflow, AI pipeline, or legal control requires it.

## Out Of Scope For First Implementation

- Phone camera recording.
- Background camera service.
- iOS.
- Payment and ads.
- AI analysis.
- Automatic legal reporting.
- Full admin review site.

## Files Added On 2026-07-13

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/product-specs/README.md`
- `docs/product-specs/core-service-concept.md`
- `docs/product-specs/submission-mvp.md`
- `docs/design-docs/README.md`
- `docs/design-docs/android-app-architecture.md`
- `docs/design-docs/sd-card-file-access.md`
- `docs/design-docs/backend-and-upload-direction.md`
- `docs/references/README.md`
- `docs/references/proposal-summary.md`
- `docs/exec-plans/README.md`
- `docs/exec-plans/2026-07-13-project-start.md`

## Next Question

Decide whether implementation should start with:

1. local-only UI and fake submission repository,
2. real Firebase setup first,
3. SD card folder picker and video listing first.

## Changed on 2026-07-13

- Previous plan: decide the first implementation starting point.
- New plan: implement SD card / USB reader folder selection and supported video candidate listing first.
- Reason: this validates the most important blackbox-specific risk before Firebase upload or consent screens.
- Implemented:
  - Android system folder picker entry from `MainActivity`.
  - SAF `DocumentFile` adapter from selected tree URI into lightweight file documents.
  - supported extension filtering for `.mp4`, `.mov`, `.avi`, `.ts`.
  - recursive folder scanning.
  - recent-first candidate sorting.
  - Compose screen showing selected folder, empty state, errors, and video candidates.
  - unit tests for filtering, sorting, scanning, adapter naming, and UI text formatting.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/domain/model/VideoCandidate.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/VideoCandidateRules.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/VideoDocument.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/VideoScanner.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/AndroidDocumentTreeVideoSource.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoListState.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoListScreen.kt`
  - `app/src/test/java/com/glass/safeclip/data/file/VideoCandidateRulesTest.kt`
  - `app/src/test/java/com/glass/safeclip/data/file/VideoScannerTest.kt`
  - `app/src/test/java/com/glass/safeclip/data/file/VideoDocumentAdapterRulesTest.kt`
  - `app/src/test/java/com/glass/safeclip/ui/video/VideoListStateTest.kt`
  - `app/src/test/java/com/glass/safeclip/ui/video/FolderPickerResultTextTest.kt`
  - `gradle/libs.versions.toml`
  - `app/build.gradle.kts`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-15

- User feedback:
  - pressing detail in submission history should show the fields entered on the submission form;
  - detail should include `사고일시`, `위치`, `사고유형`, and `메모`;
  - user also asked whether photo/video file location and time metadata can be imported.
- New behavior:
  - local submission records now store incident type and memo in addition to date/time and location;
  - submission history detail button now opens a detail panel on the status screen;
  - detail panel shows submitted incident date/time, location, incident type, memo, and file name.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/ui/status/SubmissionStatusModels.kt`
  - `app/src/main/java/com/glass/safeclip/ui/status/SubmissionStatusScreen.kt`
  - `app/src/test/java/com/glass/safeclip/ui/status/LocalSubmissionRecordTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.ui.status.LocalSubmissionRecordTest` failed before implementation and passed after implementation.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-15

- User feedback:
  - pressing `시작하기` often began showing Android's app-not-responding warning;
  - consider adding a `연결중...` loading screen so the app does not feel like it has to jump immediately.
- Root cause:
  - startup and folder-related work included SAF folder creation, saved media counting, restored folder scanning, saved folder listing, delete/copy/move operations, and some refresh calls on the UI thread;
  - large SD card folders or slow document providers can block the main thread long enough to trigger ANR.
- New behavior:
  - added a `Connecting` navigation state and `연결중...` screen after pressing start;
  - event folder setup and saved media counting now run on `Dispatchers.IO`;
  - folder restore and video scanning now run on `Dispatchers.IO`;
  - saved-media folder listing for the folder manager now loads asynchronously instead of during Compose rendering;
  - delete/copy/move file operations now run on `Dispatchers.IO`;
  - removed the unused pre-redesign `VideoListScreen`.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipBackNavigation.kt`
  - `app/src/main/java/com/glass/safeclip/ui/onboarding/StartScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoListScreen.kt`
  - `app/src/test/java/com/glass/safeclip/ui/navigation/SafeClipBackNavigationTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.ui.navigation.SafeClipBackNavigationTest` failed before implementation and passed after implementation.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - when pressing start for the first time, create/connect one event folder used for clips and captures;
  - restore the camera permission prompt to the normal camera-only permission flow.
- New behavior:
  - removed media read permissions from the Android manifest;
  - the home camera permission action now requests only `android.permission.CAMERA`;
  - pressing `시작하기` opens Android's folder picker when no event folder permission is connected;
  - the app stores the selected event-folder parent permission and creates or reuses `SafeClip Captures` inside it;
  - if the user directly selects `SafeClip Captures`, the app uses that folder without creating a nested duplicate;
  - screenshots and trimmed clips now save to the connected SAF event folder first, with the old MediaStore/app-visible save paths kept as fallback;
  - the event folder list/count now reads from the connected SAF event folder first.
- Files affected:
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidFrameCaptureStore.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidSafeClipSavedMediaRepository.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidVideoClipExporter.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/SafeClipEventFolder.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/SafeClipMediaSaveLocation.kt`
  - `app/src/test/java/com/glass/safeclip/data/media/SafeClipMediaSaveLocationTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.data.media.SafeClipMediaSaveLocationTest` failed before implementation and passed after implementation.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-15

- User feedback:
  - remove front/rear video tabs from event video checking;
  - remove front/rear shortcuts from the home screen for now;
  - remove folder reselect actions from the event preview area;
  - in folder view, after selecting a file, show delete/copy/move and then submit/play actions.
- New behavior:
  - home now shows only submission history and settings shortcut cards under the folder status section;
  - event video checking now shows only the `전체` chip and no front/rear tabs;
  - selected event preview now offers only `영상 재생` and `제출하기`;
  - the empty event screen no longer offers folder reselect from inside that screen;
  - folder manager selected-file actions now show `삭제`, `복사`, `이동`, then `영상재생하기` and `제출하기`;
  - folder manager play/submit actions are enabled only for video-like files and route into the existing preview/submission flow.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/ManagedFolderVideoCandidate.kt`
  - `app/src/main/java/com/glass/safeclip/ui/folder/FolderManagerScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoBrowserScreen.kt`
  - `app/src/test/java/com/glass/safeclip/data/file/ManagedFolderVideoCandidateTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.data.file.ManagedFolderVideoCandidateTest` failed before implementation and passed after implementation.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-15

- User feedback:
  - current-folder folder view did have selected-file actions, but they were pushed to the bottom of a long scroll list;
  - selected-file actions should appear as an overlapping fixed UI instead of being part of the scroll content.
- New behavior:
  - folder manager now keeps the file list scrollable;
  - selected-file action panel is fixed at the bottom of the screen;
  - selecting another file updates the fixed bottom panel immediately;
  - bottom padding was added to the scroll list so the last file is not hidden behind the action panel.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/ui/folder/FolderManagerScreen.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-15

- User feedback:
  - keep `영상 재생` and `제출하기` in the same row with symmetric sizes in recent event preview;
  - current-folder folder view should expose the same selected-file actions as event-folder view;
  - JPG files should be submittable while video playback remains disabled for JPG.
- New behavior:
  - selected event preview now places `영상 재생` and `제출하기` side by side with equal width;
  - folder manager keeps the same selected-file action panel for both event folder and current folder;
  - folder manager separates playback eligibility from submission eligibility;
  - video-like files can play and submit;
  - JPG/JPEG files can submit but cannot use video playback.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/ManagedFolderVideoCandidate.kt`
  - `app/src/main/java/com/glass/safeclip/ui/folder/FolderManagerScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoBrowserScreen.kt`
  - `app/src/test/java/com/glass/safeclip/data/file/ManagedFolderVideoCandidateTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.data.file.ManagedFolderVideoCandidateTest` failed before implementation and passed after implementation.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - after reinstalling the app, existing files in the event folder still exist but the app shows `0개`.
- Root cause:
  - SafeClip saved screenshots/clips remain in public MediaStore folders after reinstall;
  - after reinstall, Android no longer treats them as this app's newly-created files;
  - Android 13+ requires image/video read permissions to query existing public MediaStore items reliably.
- New behavior:
  - added `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` for Android 13+;
  - added `READ_EXTERNAL_STORAGE` for Android 12 and below;
  - the home screen's camera permission action now requests camera plus media read permissions together;
  - the camera permission tile is considered ON only when camera and required media read permissions are granted;
  - after permission response, the app refreshes the SafeClip saved media count so existing `SafeClip Captures` files can appear again;
  - cleaned `MainActivity` Korean strings while preserving existing navigation and folder management behavior.
- Files affected:
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.ui.home.HomeImportActionsTest` passed.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - keep the start button simple and move permission state handling into the second/home screen;
  - replace the reader/microSD tiles with `폴더 권한 ON/OFF` and `카메라 권한 ON/OFF`;
  - show a red dot when permission is off and a green dot when permission is on;
  - enable `최근 이벤트 보기` and `이벤트 폴더` only when both folder and camera permissions are on;
  - after reinstall, attempt to rediscover existing SafeClip saved items from the likely event folder location.
- New behavior:
  - added `android.permission.CAMERA` to the manifest;
  - home screen now shows folder permission and camera permission tiles with red/green status dots;
  - camera permission can be requested from the home screen;
  - folder permission is based on persisted SAF read/write permission for the selected folder;
  - `최근 이벤트 보기` is enabled only when both permissions are on;
  - `이벤트 폴더` folder view is enabled only when both permissions are on;
  - `현재 폴더` folder view is enabled when folder permission is on;
  - SafeClip saved media count is still refreshed on app start through the existing MediaStore scan of `SafeClip Captures`;
  - cleaned several corrupted Korean strings in home state models and video list text.
- Files affected:
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/HomeImportActions.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/HomeStatusSummary.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoListState.kt`
  - `app/src/test/java/com/glass/safeclip/ui/home/HomeImportActionsTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.ui.home.HomeImportActionsTest` passed.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - deleting files from the folder manager shows `삭제하지 못했습니다. 파일 권한을 확인해주세요.`;
  - user asked whether requesting camera/folder permissions at app start can solve it.
- Root cause:
  - Android folder access is controlled by Storage Access Framework tree permissions, not camera permission;
  - the app previously restored a saved folder when read permission existed, even if write permission was missing;
  - older saved folder selections may therefore list files but fail delete/move;
  - file deletion also stopped after `DocumentFile.delete()` returned `false` instead of trying MediaStore deletion as fallback.
- New behavior:
  - saved folder restore now requires both persisted read and write permission;
  - if an older saved folder lacks write permission, the app prompts folder selection again on startup;
  - folder selection persists read/write permission when Android grants it;
  - delete now retries with `ContentResolver.delete()` if `DocumentFile.delete()` does not succeed;
  - delete failure message now tells the user to reselect the current folder with write permission;
  - camera permission was not added because the MVP does not use camera capture and it does not affect folder deletion.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/AndroidManagedFileOperator.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/SavedFolderAccess.kt`
  - `app/src/test/java/com/glass/safeclip/data/file/SavedFolderAccessTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.data.file.SavedFolderAccessTest` failed before implementation and passed after implementation.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - replace the text under `이벤트 폴더` and `현재 폴더` with `폴더 보기` buttons;
  - pressing `폴더 보기` should allow deleting, moving, or copying files inside that folder view.
- New behavior:
  - `이벤트 폴더` and `현재 폴더` tiles now show a `폴더 보기` button;
  - added a folder manager screen for `SafeClip 저장함` and `현재 폴더`;
  - the folder manager lists files, supports file selection, and exposes `삭제`, `복사`, and `이동` actions;
  - copy/move opens Android's folder picker so the user explicitly chooses the destination folder;
  - folder selection now requests persisted read/write access so current-folder file operations can work when Android grants write permission;
  - after delete/move/copy attempts, the app refreshes saved media count and rescans the current folder when possible.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/AndroidManagedFileOperator.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/ManagedFileOperation.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/ManagedFolderFile.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidSafeClipSavedMediaRepository.kt`
  - `app/src/main/java/com/glass/safeclip/ui/folder/FolderManagerScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/folder/FolderManagerText.kt`
  - `app/src/main/java/com/glass/safeclip/ui/folder/FolderViewKind.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/HomeImportActions.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipBackNavigation.kt`
  - `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoListState.kt`
  - `app/src/test/java/com/glass/safeclip/ui/folder/FolderManagerTextTest.kt`
  - `app/src/test/java/com/glass/safeclip/ui/home/HomeImportActionsTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - on the second/home screen, `이벤트 폴더` should represent the folder where SafeClip saves screenshots and clips, and show that saved item count;
  - the `저장 용량` tile should become `현재 폴더` and show the number of videos in the currently selected blackbox folder;
  - keep the SafeClip title size, but make it white and add the same icon style used on the first screen.
- New behavior:
  - added a SafeClip saved media counter for `Pictures/SafeClip Captures` and `Movies/SafeClip Captures`;
  - home state now keeps `savedMediaItemCount`;
  - saved media count refreshes on app start and after successful frame capture or clip export;
  - `이벤트 폴더` tile now shows `SafeClip Captures` and the saved capture/clip count;
  - `현재 폴더` tile now shows the current selected folder video count;
  - `SafeClipTopBar` now renders a white title with a shared SafeClip logo mark.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidSafeClipSavedMediaCounter.kt`
  - `app/src/main/java/com/glass/safeclip/ui/components/SafeClipLogoMark.kt`
  - `app/src/main/java/com/glass/safeclip/ui/components/SafeClipTopBar.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/HomeStatusSummary.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoListState.kt`
  - `app/src/test/java/com/glass/safeclip/ui/home/HomeStatusSummaryTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - on the second/home screen, move the video import area to the top;
  - rename `영상 불러오기` to `폴더 선택하기`;
  - when no folder is selected, `폴더 선택하기` should be the orange primary action;
  - when a folder is selected, show the selected folder location and make `최근 이벤트 보기` the orange primary action.
- New behavior:
  - the folder/event action panel now appears directly below the top bar;
  - folder selection action text is now `폴더 선택하기`;
  - no selected folder: `폴더 선택하기` is primary and `최근 이벤트 보기` is disabled secondary;
  - selected folder: selected folder text is shown and `최근 이벤트 보기` becomes primary;
  - pressing `최근 이벤트 보기` after selecting a folder opens the event list even when the list is empty, so the empty-state screen can explain the result.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/HomeImportActions.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt`
  - `app/src/test/java/com/glass/safeclip/ui/home/HomeImportActionsTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.ui.home.HomeImportActionsTest` passed.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - the app launcher icon's outer white bracket/border is clipped by the launcher mask;
  - move the outer border and inner icon content slightly inward.
- New behavior:
  - launcher foreground bracket corners now start at safer inset coordinates;
  - shield, lens, road mark, and recording dot are slightly smaller and more centered;
  - pre-Android 8 fallback icons use the same safer coordinates.
- Files affected:
  - `app/src/main/res/drawable/ic_safeclip_launcher_foreground.xml`
  - `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
  - `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`
  - `app/src/test/java/com/glass/safeclip/ui/icon/SafeClipLauncherIconResourceTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.ui.icon.SafeClipLauncherIconResourceTest` failed before implementation and passed after implementation.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - use the same icon style from the first screen title area as the app launcher icon.
- New behavior:
  - adaptive launcher icons now use SafeClip-specific background and foreground vector resources;
  - pre-Android 8 launcher icons also have matching `mipmap-anydpi` vector resources;
  - the icon keeps the same visual language as the title icon: dark blue/cyan base, scan brackets, shield, camera lens, road mark, and orange recording dot.
- Files affected:
  - `app/src/main/res/drawable/ic_safeclip_launcher_background.xml`
  - `app/src/main/res/drawable/ic_safeclip_launcher_foreground.xml`
  - `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
  - `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
  - `app/src/test/java/com/glass/safeclip/ui/icon/SafeClipLauncherIconResourceTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.glass.safeclip.ui.icon.SafeClipLauncherIconResourceTest` failed before implementation and passed after implementation.
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - center-align the first screen `SafeClip` title and subtitle;
  - check whether a GPT-generated icon can be used safely;
  - add an icon with a similar shield/camera/recording-dot feeling before the title.
- Copyright note:
  - OpenAI Terms say the user owns Output as between the user and OpenAI, to the extent permitted by law;
  - AI output can still be similar to other output and should be reviewed before final commercial branding;
  - to reduce risk, the app does not embed the provided PNG directly and instead draws a new SafeClip-specific icon in Compose.
- New behavior:
  - first screen title block is centered;
  - subtitle is centered;
  - title row now includes a custom Canvas icon inspired by the requested shield, lens, and recording-dot style.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/ui/onboarding/StartScreen.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - the first screen `SafeClip` title is too dark and hard to read;
  - USB-C and microSD connection states should show clearer ON/OFF difference;
  - remove the event video import status item from the first screen;
  - keep the two connection buttons the same size and symmetrical;
  - make the start button larger and centered with balanced left/right spacing.
- New behavior:
  - first screen title now renders in white;
  - first screen shows only two equal-width connection status boxes: USB-C and microSD;
  - each status box has explicit `OFF` and `대기` state text, with styling prepared for a future `ON` state;
  - removed the old third event-video status item;
  - enlarged the start button and made it full-width inside the same horizontal margins.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/ui/onboarding/StartScreen.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Next Recommended Step

Run the app from Android Studio on a real Android phone, connect a USB-C microSD reader, select a blackbox folder, and confirm real files appear. After that, implement video preview for selected candidates.

## Changed on 2026-07-14

- Previous behavior: after selecting an SD card / USB reader folder, the app showed videos only for the current app session.
- Problem: after closing and reopening the app, the user had to select the same folder again.
- Root cause: the app requested persistable read permission from Android, but did not save the selected tree URI in app storage.
- New behavior:
  - save the last selected folder URI in app `SharedPreferences`;
  - on app startup, check whether Android still has persisted read permission for that URI;
  - if permission is still available, automatically reload and scan the previous folder;
  - if permission is missing or revoked, do not attempt to restore.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/LastSelectedFolderStore.kt`
  - `app/src/main/java/com/glass/safeclip/data/file/SavedFolderAccess.kt`
  - `app/src/test/java/com/glass/safeclip/data/file/SavedFolderAccessTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- Previous behavior: video candidates could be listed, but tapping a video did not open a preview.
- New behavior:
  - tapping a video candidate opens an in-app preview screen;
  - preview uses AndroidX Media3 ExoPlayer and PlayerView;
  - user can switch playback speed between `0.25x`, `0.5x`, and `1.0x`;
  - user can capture the current playback position as a JPEG frame;
  - captures are saved under the app-specific Pictures `captures` folder;
  - user can return from preview to the video list.
- Reason: reviewing blackbox footage, slowing it down, and capturing key frames are core to report preparation.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoListScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/PlaybackSpeedOption.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/CaptureFileName.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidFrameCaptureStore.kt`
  - `app/src/test/java/com/glass/safeclip/data/media/PlaybackSpeedOptionTest.kt`
  - `app/src/test/java/com/glass/safeclip/data/media/CaptureFileNameTest.kt`
  - `app/src/test/java/com/glass/safeclip/ui/video/VideoPreviewTextTest.kt`
  - `gradle/libs.versions.toml`
  - `app/build.gradle.kts`
- Manual test steps:
  - select or restore a blackbox folder;
  - tap a listed video;
  - confirm playback starts or is ready in the preview screen;
  - change speed to `0.25x`, `0.5x`, and `1.0x`;
  - tap "현재 장면 캡쳐";
  - confirm a saved path message appears.
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- Previous behavior:
  - capture could fail or save to an app-specific path that was hard for the user to find;
  - preview content could overlap the phone status bar / battery area;
  - video preview area felt too small;
  - playback speeds only supported `0.25x`, `0.5x`, and `1.0x`;
  - preview control labels were too large for the available screen space.
- New behavior:
  - frame capture opens the selected video URI through a file descriptor before using `MediaMetadataRetriever`;
  - on Android 10 and above, captures save through MediaStore to `Pictures/SafeClip Captures`;
  - preview screen uses safe drawing insets to avoid the top system status area;
  - preview padding and spacing are reduced so the video area is larger;
  - control text is smaller;
  - playback speeds now support `0.25x`, `0.5x`, `1.0x`, `1.5x`, and `2.0x`.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidFrameCaptureStore.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/PlaybackSpeedOption.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
  - `app/src/test/java/com/glass/safeclip/data/media/PlaybackSpeedOptionTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- Previous feedback:
  - capture still did not appear to save during simple video testing;
  - speed buttons were still too large and `2.0x` could be clipped;
  - capture button was still too large.
- Root cause / evidence:
  - `adb` was not available in PATH on this machine, so direct device testing could not be run from this session;
  - capture failure message previously hid the real exception reason;
  - Compose Material buttons kept too much default visual size for the small preview control row.
- New behavior:
  - capture failure messages now include the underlying failure reason when available;
  - frame extraction now uses `openAssetFileDescriptor` with file descriptor, start offset, and length;
  - speed controls and capture action use compact custom chips instead of full Material buttons;
  - speed chip text is 10sp with smaller padding;
  - capture action text is 10sp with smaller padding.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidFrameCaptureStore.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
  - `app/src/test/java/com/glass/safeclip/ui/video/VideoPreviewTextTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- Previous need:
  - before submitting, the user should be able to trim the selected video and save a shorter submission clip;
  - the submission flow should still know where the original video came from and what time range was cut.
- New behavior:
  - preview now has compact controls beside `현재 장면 캡쳐`: `시작 지정`, `끝 지정`, and `클립 저장`;
  - the app saves a new MP4 clip and never overwrites the original blackbox video;
  - clip saving uses AndroidX Media3 Transformer with the selected SAF video URI;
  - saved clip results preserve original URI, file name, size, last modified time, folder path, clip start time, and clip end time for the later submission flow;
  - clip output is stored in the app-owned Movies `safeclip-clips` folder for now.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidVideoClipExporter.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/VideoClipExportResult.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/VideoClipSelection.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/ClipFileName.kt`
  - `app/src/test/java/com/glass/safeclip/data/media/VideoClipSelectionTest.kt`
  - `app/src/test/java/com/glass/safeclip/data/media/ClipFileNameTest.kt`
  - `app/src/test/java/com/glass/safeclip/ui/video/VideoPreviewTextTest.kt`
  - `gradle/libs.versions.toml`
  - `app/build.gradle.kts`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - clip saving should use the same visible SafeClip location as frame capture;
  - pressing `클립 저장` did not appear to save a visible file.
- Root cause:
  - clip export wrote to an app-owned Movies folder, which is not the same user-visible MediaStore location used by capture on Android 10+;
  - because the exported file was not registered in MediaStore, it could look like nothing was saved from the user's file/gallery view.
- New behavior:
  - the app still exports to a temporary MP4 first because Media3 Transformer requires a file path;
  - after export completes, Android 10+ copies the clip into MediaStore under `Movies/SafeClip Captures`;
  - capture continues to use `Pictures/SafeClip Captures`, so both use the same `SafeClip Captures` album name while staying in Android's correct media collections;
  - success messages now show the visible clip path, not the temporary/app-owned path;
  - clip export is launched from an IO dispatcher so the preview UI remains responsive.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidVideoClipExporter.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidFrameCaptureStore.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/SafeClipMediaSaveLocation.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/VideoClipExportResult.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
  - `app/src/test/java/com/glass/safeclip/data/media/SafeClipMediaSaveLocationTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- New design input:
  - user provided AI-generated UI concepts for start screen, mobile home/waiting screen, video review/edit screen, submission/status screens, and company server dashboard.
- Design decision:
  - redesign the whole Android mobile app flow first;
  - keep the company server/admin dashboard as future web/admin scope;
  - use the concept images for visual direction, not as exact one-to-one layouts.
- Spec added:
  - `docs/superpowers/specs/2026-07-14-mobile-app-redesign.md`
- Notes:
  - this folder is not a Git repository, so the design spec could not be committed from this workspace.

## Changed on 2026-07-14

- New planning work:
  - created an implementation plan for the full Android mobile redesign.
- Plan added:
  - `docs/superpowers/plans/2026-07-14-mobile-app-redesign-implementation.md`
- Plan scope:
  - shared SafeClip visual system;
  - simple direct navigation state;
  - start screen, home screen, video browser, preview/edit submit entry, submission form, and submission status;
  - local in-memory submission records only;
  - no company server/admin dashboard implementation in this Android slice.

## Changed on 2026-07-14

- Implemented Android mobile app redesign based on `docs/superpowers/specs/2026-07-14-mobile-app-redesign.md`.
- Added:
  - SafeClip branded dark theme colors;
  - shared UI components for scaffold, top bar, glass panels, status chips, metric strips, and primary/secondary actions;
  - start screen;
  - main home/waiting screen;
  - video browser screen;
  - preview/edit screen submit entry;
  - local submission form;
  - local submission status screen.
- Kept:
  - existing SAF folder selection and persisted folder restore;
  - existing video scanning;
  - existing playback, capture, trim, and clip save logic;
  - company server/admin dashboard out of Android scope.
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - each screen should have an obvious top-right back/list/home action;
  - pressing the phone back button currently exits the app instead of moving to the previous app screen.
- Root cause:
  - the app used direct Compose screen state, but did not intercept Android system back events;
  - each screen handled its own top action manually, so behavior was not consistent.
- New behavior:
  - added `SafeClipBackNavigation` as a single source of truth for previous-screen rules;
  - Android `BackHandler` now moves through app screens instead of closing the app when a previous screen exists;
  - Home has a top-right `뒤로` action;
  - Video browser has a top-right `홈` action;
  - Preview keeps top-right `목록`;
  - Submission form keeps top-right `뒤로`;
  - Submission status keeps top-right `홈`;
  - Start screen has no back action because it has no previous screen.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/MainActivity.kt`
  - `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipBackNavigation.kt`
  - `app/src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoBrowserScreen.kt`
  - `app/src/test/java/com/glass/safeclip/ui/navigation/SafeClipBackNavigationTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - frame capture is now working;
  - clip saving fails with `Transformer is accessed on the wrong thread`.
- Root cause:
  - preview UI wrapped the entire clip export call in `Dispatchers.IO`;
  - Media3 Transformer requires creation, start, cancellation, and listener access to stay on the same thread;
  - moving the whole export to IO made Transformer run on a worker thread while callbacks/cancellation could happen elsewhere.
- New behavior:
  - original SAF video copy and MediaStore publishing run on `Dispatchers.IO`;
  - Media3 Transformer creation/start now runs on `Dispatchers.Main.immediate`;
  - preview UI no longer wraps the whole `onExportClip` call in IO;
  - temporary files are still cleaned up after success/failure.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidVideoClipExporter.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.

## Changed on 2026-07-14

- User feedback:
  - frame capture sometimes succeeds and sometimes fails with "현재 위치에서 캡쳐할 프레임을 찾지 못했습니다.";
  - clip saving still does not produce a saved video in real testing.
- Root cause analysis:
  - frame capture tried only one exact playback timestamp, but blackbox files can have sparse keyframes or timestamp gaps;
  - clip export passed the SAF `content://` URI directly into Media3 Transformer, while playback support and transform support can differ by provider/container.
- New behavior:
  - frame capture now tries the requested timestamp plus nearby positions at 100ms, 500ms, and 1000ms offsets;
  - frame capture tries closest, closest sync, previous sync, and next sync extraction modes before failing;
  - clip export now copies the original SAF video into a temporary app cache file first, then runs Transformer from a file URI;
  - temporary source/output files are deleted on success, failure, and cancellation;
  - failure messages are now readable Korean and mention when a blackbox format may not support device-side trimming.
- Files affected:
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidFrameCaptureStore.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/AndroidVideoClipExporter.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/FrameCaptureSearchPlan.kt`
  - `app/src/main/java/com/glass/safeclip/data/media/ClipTempFileName.kt`
  - `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
  - `app/src/test/java/com/glass/safeclip/data/media/FrameCaptureSearchPlanTest.kt`
  - `app/src/test/java/com/glass/safeclip/data/media/ClipTempFileNameTest.kt`
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.
