# SafeClip Admin Review Web Design

## Goal

Build the first real company review web application for SafeClip. The Android app uploads video files directly to the Synology NAS and stores submission metadata in Firestore. Company staff use an internal-only web portal to find submissions, stream the matching NAS video, and change the Firestore `status` field that the Android app already reads.

This is an operational MVP, not a visual prototype.

## Confirmed Decisions

- Use the existing `admin-web/` React, Vite, and TypeScript project.
- Follow the supplied dark SafeClip dashboard image as visual direction, not as a one-to-one feature list.
- Host the administrator portal on the company NAS and make it reachable only from the company LAN.
- Do not require administrator sign-in in this first internal MVP.
- Keep the public upload API and internal administrator API on separate Web Station portals and ports.
- Keep DSM, SMB, the administrator portal, and all video read endpoints unavailable from the public internet.
- Use Firestore as the source of truth for submission metadata and review status.
- Store video bytes only on the NAS.
- Do not implement AI analysis, result transmission, user management, report generation, or public video access in this slice.

## Security Boundary

The system has two deliberately separate entrances:

```text
Public internet
  -> HTTPS public upload portal
  -> nas-upload-api
  -> write-only video storage

Company LAN
  -> internal admin portal
  -> admin-web + nas-admin-api
  -> Firestore metadata/status + NAS video streaming
```

The router forwards only the public HTTPS upload port. It must not forward the internal administrator portal port, DSM ports `5000/5001`, SMB `445`, FTP, WebDAV, or the internal video endpoint.

The internal PHP API also checks `REMOTE_ADDR` against configured LAN CIDRs, initially `192.168.0.0/24` and loopback. It does not trust `X-Forwarded-For`. This check is defense in depth; the primary boundary remains the absence of router port forwarding for the administrator portal.

Without administrator sign-in, anyone connected to the permitted company LAN can view videos and change statuses. The MVP therefore cannot attribute a status change to a named administrator. Add administrator authentication before expanding access beyond a small trusted internal network.

## Project Boundaries

```text
admin-web/
  React administrator UI source

nas-upload-api/
  public write-only upload receiver

nas-admin-api/
  internal-only PHP endpoints
  Firestore REST connector
  NAS video lookup and streaming

/volume1/SafeClipUpLoads/
  stored video files and recovery sidecars

/volume1/SafeClipConfig/
  administrator API config
  dedicated Firebase service account credential
  cached Google access token
  audit log
```

`/volume1/SafeClipConfig` is outside every Web Station document root. It is not exposed as an SMB share to ordinary users. Only administrators and the Web Station `http` group receive the minimum required access.

## Submission Identity And Data Contract

The Firestore submission document ID is the single identifier connecting the Android app, Firestore, NAS filename, and administrator web.

The Android upload sequence is:

1. Create `submissions/{submissionId}` in Firestore with `status = uploading`.
2. Upload the video to the NAS with `submission_id = submissionId`.
3. Receive `relative_path`, `stored_name`, and `size_bytes` from the NAS.
4. Update the same Firestore document with:

```text
nasRelativePath
nasStoredName
nasSizeBytes
uploadCompletedAt
status = waiting_review
updatedAt
```

5. If the NAS upload fails, keep the document in `uploading` or mark it with a separate upload error field during the Android integration slice. Never move it to `waiting_review` before the file is stored.

The upload API writes a recovery sidecar beside the video after a successful move:

```text
2026/07/27/video-name.mp4
2026/07/27/video-name.mp4.json
```

The sidecar contains only recovery metadata: submission ID, original filename, stored filename, relative path, byte size, received time, and optional device label. Firestore remains the normal metadata source.

## Internal API

Use simple PHP file endpoints because Synology Web Station does not need URL rewriting for this MVP.

### `GET /api/submissions.php`

- Returns newest Firestore submissions first.
- Supports `status`, `limit`, and cursor parameters.
- Default page size is 50; maximum is 100.
- Returns only fields required by the dashboard and list.

### `GET /api/submission.php?id={documentId}`

- Returns the selected submission metadata.
- Indicates whether its NAS video currently exists.
- Does not return an absolute NAS filesystem path.

### `GET /api/video.php?id={documentId}`

- Loads the Firestore document server-side.
- Reads only the document's `nasRelativePath`.
- Resolves and verifies the canonical file path remains under `/volume1/SafeClipUpLoads`.
- Rejects missing, malformed, or traversal paths.
- Supports HTTP `Range` requests so seeking does not load the entire video into PHP memory.
- Allows only configured video extensions and emits `Content-Disposition: inline`.

### `POST /api/status.php`

Request:

```json
{
  "submissionId": "firestore-document-id",
  "status": "reviewing"
}
```

Allowed administrator statuses:

```text
waiting_review
reviewing
needs_more_info
report_package_ready
rejected
completed
```

The endpoint rejects every other field and value. It updates only `status` and `updatedAt` in the specified Firestore document. `uploading` is controlled by the Android upload flow and is not an administrator action.

## Firestore Access

The browser never receives Firebase write credentials or a service account key. The internal PHP API uses a dedicated Google Cloud service account to call the Firestore REST API.

The service account receives only the Firestore entity permissions required to list, read, and update submission documents. Its JSON credential stays under `/volume1/SafeClipConfig`, outside the web root and repository.

PHP obtains a short-lived OAuth access token using the service account, caches it with an expiry time, and protects cache refresh with a file lock. Synology PHP must enable `curl` and `openssl`.

The API code limits reads to the `submissions` collection and limits administrator writes to the allowlisted status update operation even though the service account bypasses Firestore Security Rules.

## Administrator Interface

The first screen is a dense desktop review workspace based on the supplied SafeClip concept.

### Navigation

- Dashboard
- Video review
- Submissions

These routes use the same real data with different initial filters. AI analysis, result transmission, and user management are omitted rather than shown as fake controls.

### Dashboard

- Total submissions
- Waiting review
- Reviewing
- Completed
- Recent submission table
- Selecting a row opens the real review workspace

### Review Workspace

- Native HTML video player with seek support
- Submission ID and status
- Incident date/time
- Location text
- Violation type candidate
- User memo
- Original filename and file size
- Submitter display name, email, or guest label when present
- Status action group using the existing Android status vocabulary

Status buttons remain disabled while a request is running. The UI changes state only after the server confirms the Firestore update.

### Visual Direction

- Quiet operational dashboard rather than a marketing page.
- Dark neutral/navy background with cyan, green, amber, red, and violet used by function and status.
- Compact typography and stable table/control dimensions.
- Lucide icons for navigation and actions.
- Maximum card radius of 8px.
- No nested decorative cards, gradients, or non-functional dashboard statistics.
- Desktop is primary; narrower screens collapse the sidebar and stack the review metadata without overlapping text or controls.

## Loading, Empty, And Error Handling

- Initial list load: stable skeleton rows that do not shift the layout.
- No submissions: a concise empty state with a refresh action.
- Firestore unavailable: keep the current screen, show a clear error banner, and offer retry.
- Video metadata missing: show submission details and a `Video not linked` state instead of failing the whole page.
- Video file missing: return `404`, show the relative filename only, and keep status controls available.
- Status conflict or validation failure: preserve the old status and display the server message.
- Network interruption during playback: the native player can retry range requests; the page provides reload.
- Firestore access token refresh failure: return `503` without exposing credential or Google response details.

PHP errors are logged outside the web root. JSON responses never include stack traces, credentials, absolute paths, or raw exception messages.

## Audit Trail

Because there is no administrator login, the MVP writes a protected local audit entry for each successful status change:

```text
timestamp, source IP, submission ID, previous status, new status
```

The log is not exposed through an API or displayed in the first UI. It supports basic incident diagnosis but does not identify a person.

## Testing And Verification

### React

- Status label and tone mapping tests.
- Dashboard count tests.
- Submission filtering and ordering tests.
- Loading, empty, missing-video, and API-error component tests.
- Status controls update only after a successful response.
- Production build with `npm run build`.

### PHP

- LAN CIDR allow/deny tests.
- Submission ID and status allowlist tests.
- Canonical path containment and path traversal tests.
- HTTP Range parsing tests, including invalid and suffix ranges.
- Firestore response mapping tests with fixtures.
- Static checks that credentials and absolute paths are not returned.

### NAS Integration

1. Load a real Firestore submission through the internal portal.
2. Stream a real MP4 from `/volume1/SafeClipUpLoads` and seek to multiple positions.
3. Change `waiting_review` to `reviewing` in the administrator web.
4. Confirm the Firestore `status` field changed.
5. Refresh the Android submission history and confirm it displays `검토중`.
6. Confirm the administrator portal works on the company LAN.
7. Confirm the administrator port is unreachable from mobile data.
8. Confirm DSM, SMB, and video endpoints remain unavailable externally.

Before completion, capture and inspect desktop and narrow-viewport screenshots to verify layout, text fitting, video framing, and control overlap.

## Deployment Shape

Build `admin-web` on the development PC. Deploy its `dist` contents together with the public files from `nas-admin-api` under one internal Web Station document root so the React app and `/api/*.php` share an origin and require no CORS configuration.

Example NAS document root:

```text
/volume1/web/safeclip-admin/
  index.html
  assets/
  api/
    submissions.php
    submission.php
    video.php
    status.php
```

Use a dedicated internal port such as `8090`. Do not add that port to ipTIME port forwarding. The public upload portal remains separate.

## Explicitly Deferred

- Administrator login and named audit history
- Public administrator access or VPN access
- AI analysis and number-plate recognition
- Report package generation
- Result messaging
- User management
- Video deletion and retention automation
- Multiple administrator roles
- Large-scale reverse proxy, WAF, or dedicated gateway migration
