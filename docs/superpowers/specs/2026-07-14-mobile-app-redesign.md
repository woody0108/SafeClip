# SafeClip Mobile App Redesign Spec

## Purpose

Redesign the Android MVP screens so SafeClip feels like one coherent product instead of a technical prototype.

The redesign uses the provided AI UI concepts as visual direction:

- dark navy base
- cyan/blue highlights
- orange primary call-to-action
- glass-like panels with thin borders
- evidence/security/blackbox tone

The first implementation stays focused on the Android user app. The company server/admin dashboard concept is recorded as future scope and is not built in the Android MVP slice.

## Product Scope

### In Scope

- Start/onboarding screen.
- Main waiting screen for SD card / reader readiness.
- Video list and selected video summary screen.
- Video preview/edit screen with playback, capture, trim, and clip save.
- Submission form screen.
- Submission status/history screen with local in-memory sample records first.
- Shared visual system for cards, buttons, status chips, and dark background.

### Out Of Scope For This Redesign Slice

- Company server/admin web implementation.
- Real AI number-plate recognition.
- Real legal judgment or automatic reporting.
- Real Firebase upload. Firebase upload belongs to a separate submission-flow task after this redesign.
- Heavy custom animation or generated visual assets as a blocking requirement.

## Screen Flow

```text
StartScreen
  -> MainHomeScreen
      -> Android folder picker
      -> VideoBrowserScreen
          -> VideoPreviewEditScreen
              -> SubmissionFormScreen
                  -> SubmissionStatusScreen
```

Navigation remains simple for this redesign. `MainActivity` keeps a small sealed UI state and switches screens directly.

## Screens

### 1. Start Screen

Goal: introduce SafeClip and explain the SD card based workflow in one glance.

Content:

- SafeClip brand name.
- Short tagline: `블랙박스 영상을 쉽고 빠르게 제출`.
- Three compact process indicators:
  - `USB-C 연결`
  - `microSD 인식`
  - `이벤트 영상 가져오기`
- Main CTA: `시작하기`.
- Trust note: `원본 영상은 사용자가 선택할 때만 처리됩니다.`

Behavior:

- `시작하기` opens `MainHomeScreen`.
- Do not request storage access on this screen.

### 2. Main Home Screen

Goal: be the user's main dashboard before selecting a folder.

Content:

- Header with SafeClip logo text and subtitle: `블랙박스 이벤트 영상 관리`.
- Four status tiles:
  - `리더기 연결`
  - `microSD 인식`
  - `이벤트 폴더`
  - `저장 용량`
- Hero panel explaining that videos can be loaded from microSD.
- Primary CTA: `영상 불러오기`.
- Secondary CTA: `최근 이벤트 보기`.
- Shortcut cards:
  - `전방 영상`
  - `후방 영상`
  - `제출 내역`
  - `설정`

Behavior:

- `영상 불러오기` launches Android `OpenDocumentTree`.
- If a saved folder is available, show it as already connected.
- If no folder is selected, status tiles use neutral/pending states.
- `최근 이벤트 보기` opens `VideoBrowserScreen` only when videos are loaded; otherwise show a short message.

### 3. Video Browser Screen

Goal: let the user inspect videos found in the selected SD card folder and choose one for review.

Content:

- Header: `이벤트 영상 확인`.
- Storage summary:
  - selected folder name
  - video count
  - last scan state
- Front/rear segmented tabs are displayed visually, but this redesign classifies all videos as `전체` until a separate pairing/front-rear detection feature exists.
- Left or top list of video candidates:
  - static thumbnail box for now
  - file name
  - modified date/time
  - file size
  - duration only when metadata extraction already provides it
- Selected video preview summary:
  - file metadata
  - folder path
  - simple evidence note
- Bottom actions:
  - `영상 재생`
  - `제출하기`

Behavior:

- Selecting a row updates the selected video.
- `영상 재생` opens `VideoPreviewEditScreen`.
- `제출하기` can either open the preview first if no clip exists, or open `SubmissionFormScreen` with the selected original video.

### 4. Video Preview/Edit Screen

Goal: support the core evidence preparation workflow already implemented.

Content:

- Full-width video player area remains the largest element.
- Compact top header:
  - back button
  - file name
- Playback controls:
  - speed chips: `0.25x`, `0.5x`, `1.0x`, `1.5x`, `2.0x`
- Evidence tools in one compact row:
  - `현재 장면 캡쳐`
  - `시작 지정`
  - `끝 지정`
  - `클립 저장`
- Clip range text.
- Status message text.
- Primary CTA after clip/original selection:
  - `제출하기`

Behavior:

- Keep current playback, capture, trim, and save logic.
- `제출하기` opens `SubmissionFormScreen`.
- If clip export fails, keep the original video selected and show a clear message.

### 5. Submission Form Screen

Goal: collect only the minimum useful report information before upload.

Content:

- Selected video summary:
  - original file name
  - saved clip path if a clip was created
  - original folder path
  - size
- Required fields:
  - incident date/time
  - location text
  - incident type candidate
  - memo
- Consent section:
  - company review consent
  - video storage consent
  - traffic-risk data-use consent
- Main CTA: `제출하기`.

Behavior:

- Submission button is disabled until required fields and consent items are complete.
- This redesign saves a local in-memory sample submission record rather than uploading.
- Upload and Firebase wiring are separate work after this redesign.

### 6. Submission Status Screen

Goal: show that submissions are tracked after the user sends them.

Content:

- Summary counters:
  - `제출`
  - `검토중`
  - `자료생성`
  - `결과회신`
- Submission list:
  - static thumbnail box
  - title or memo summary
  - incident time
  - location
  - status chip
- Empty state if no submissions exist.

Behavior:

- Status values use the existing MVP labels:
  - `업로드중`
  - `검토대기`
  - `검토중`
  - `자료보완필요`
  - `신고자료준비완료`
  - `반려`
  - `완료`
- First implementation can use in-memory or local fake data until real submission repository exists.

## Visual System

### Palette

- App background: very dark navy, near `#03101F`.
- Elevated panels: `#071A33` to `#0B2747`.
- Primary blue: `#159BFF`.
- Cyan accent: `#32D4FF`.
- Orange CTA: `#FF5A1F`.
- Success green: `#2FE58F`.
- Warning amber: `#F6A540`.
- Error red: `#FF6B6B`.
- Text primary: near white.
- Text secondary: blue-gray.

Avoid making every element blue. Use orange only for the primary next action, green only for completed states, amber only for review/waiting states, and red only for problems.

### Components

- `SafeClipScaffold`: shared dark background and safe drawing padding.
- `SafeClipTopBar`: compact brand/header row.
- `GlassPanel`: dark panel with subtle border, radius 8dp.
- `StatusTile`: icon-like label, state text, optional check indicator.
- `PrimaryActionButton`: orange, large enough for mobile thumb use.
- `SecondaryActionButton`: blue/outlined.
- `StatusChip`: small colored state label.
- `VideoCandidateCard`: list item for videos.
- `MetricStrip`: compact counters for status/history.

### Layout Rules

- Keep cards at 8dp radius unless a specific component needs a circular icon.
- Avoid nested cards inside cards.
- Do not use large decorative blobs/orbs.
- The video player must remain larger than surrounding controls.
- Text does not scale with viewport width.
- Controls must not overlap Android status/navigation bars.

## Architecture Changes

Suggested package additions:

```text
ui/
  components/
  home/
  onboarding/
  submission/
  status/
  video/

data/
  submission/

domain/
  model/
```

Current video file/media logic stays in `data/file` and `data/media`.

Navigation state can start as:

```kotlin
sealed interface SafeClipScreen {
    data object Start : SafeClipScreen
    data object Home : SafeClipScreen
    data object VideoBrowser : SafeClipScreen
    data class VideoPreview(val video: VideoCandidate) : SafeClipScreen
    data class SubmissionForm(val video: VideoCandidate, val clip: VideoClipExportResult?) : SafeClipScreen
    data object SubmissionStatus : SafeClipScreen
}
```

Navigation Compose is not part of this redesign. It can be evaluated in a future navigation-specific task if screen count or back stack behavior grows.

## Data Flow

1. User starts on `StartScreen`.
2. User enters `MainHomeScreen`.
3. User taps `영상 불러오기`.
4. Android folder picker returns a tree URI.
5. App persists the URI and scans videos.
6. `VideoBrowserScreen` shows candidates.
7. User opens `VideoPreviewEditScreen`.
8. User may capture frames and save a trimmed clip.
9. User opens `SubmissionFormScreen`.
10. User fills details and consent.
11. This redesign creates a local in-memory sample submission.
12. `SubmissionStatusScreen` shows the new record.

## Error Handling

- Missing saved folder permission: return to `MainHomeScreen` and show `폴더 접근 권한이 필요합니다.`
- No videos found: keep the folder selected and show an empty state with a rescan action.
- Preview unsupported: show a clear message and allow returning to the video list.
- Capture failure: keep current detailed failure message.
- Clip export failure: keep original video available for submission and explain that this device/format may not support trimming.
- Submission incomplete: show inline field messages, not a generic dialog.

## Testing Plan

- Unit test screen-state transitions where possible.
- Unit test submission field validation.
- Unit test status label/color mapping.
- Keep existing file scanning, capture naming, clip naming, and media helper tests.
- Manual test the full happy path:
  1. start app
  2. select folder
  3. open video
  4. capture frame
  5. trim clip
  6. open submission form
  7. create local in-memory sample submission
  8. view status list

## Implementation Order

1. Add shared visual components and theme colors.
2. Add screen state model and simple navigation in `MainActivity`.
3. Add `StartScreen` and `MainHomeScreen`.
4. Restyle video list into `VideoBrowserScreen`.
5. Restyle current preview into `VideoPreviewEditScreen`.
6. Add `SubmissionFormScreen` with local in-memory submit behavior.
7. Add `SubmissionStatusScreen` with local in-memory records.
8. Update docs and run tests/build.

## Future Admin Server Direction

The company server/admin dashboard concept becomes a separate web/admin spec after the Android redesign.

That future spec covers:

- submission queue
- video review
- AI analysis result display
- report package generation
- result transmission
- user history

It does not block the Android MVP redesign.
