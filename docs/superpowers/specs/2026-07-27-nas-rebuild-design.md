# SafeClip NAS Rebuild Design

## Goal

Rebuild the SafeClip Synology upload and review environment from a freshly initialized DS214 without interrupting the existing company website. Anonymous app users must be able to upload one or two dashcam videos from any internet connection, while video reading and administrator functions remain available only on the company LAN.

## Confirmed Environment

- NAS: Synology DiskStation DS214.
- DSM: 7.1.1-42962 Update 9.
- Router: ipTIME A2004MU with administrator access restored.
- Public fixed IP: the existing company fixed IP used by the NAS.
- Domain: `seyoungi.com`, managed through Gabia DNS.
- Existing DNS has A records for the root, `www`, and wildcard `*` pointing to the NAS public IP.
- Existing website: `www.seyoungi.com`, hosted by Web Station on the same NAS using ports 80 and 443.
- The existing website must continue operating throughout the rebuild.
- Docker is unavailable and is not part of this design.

## Architecture

Use hostname-based Web Station portals on the same standard HTTPS port:

```text
Public internet
  -> https://www.seyoungi.com:443
  -> existing Web Station website

Public SafeClip clients
  -> https://upload.seyoungi.com:443
  -> separate Web Station upload portal
  -> PHP 8.0 upload-only API
  -> /volume1/SafeClipUpLoads

Company LAN only
  -> http://192.168.0.3:8090
  -> SafeClip administrator web and internal PHP API
  -> Firestore metadata/status and NAS video streaming
```

The router keeps the existing 80 and 443 forwarding to the NAS. It must not forward port 8090, DSM ports 5000/5001, SMB 445, FTP, WebDAV, or any video-read endpoint.

The wildcard DNS record already routes `upload.seyoungi.com` to the fixed public IP. A dedicated explicit DNS record may be added later for clarity but is not required for routing. DSM must obtain and assign a certificate valid for `upload.seyoungi.com` before production upload testing.

## Identity And Authentication

The existing `guestId` is a device-generated display and lookup identifier. It is not proof of identity and must never authorize an upload.

Every submitting app instance uses Firebase Authentication:

- Existing Google or email users keep their current Firebase UID.
- A signed-out user is silently signed in through Firebase Anonymous Authentication before creating a submission.
- No login screen is shown for anonymous use.
- The device `guestId` remains stored for display, recovery, and later account linking.
- New submissions always store a non-empty Firebase `ownerUid`.

The Android app sends its Firebase ID token in the HTTPS `Authorization: Bearer` header. The PHP server verifies the RS256 signature, key ID, expiry, issued-at time, audience, issuer, subject, and authentication time according to the Firebase ID token rules. The verified token UID must equal the Firestore submission `ownerUid`.

The public upload API no longer uses an app-wide static upload key. A key embedded in an APK can be extracted and is unsuitable as the primary credential for an internet-facing endpoint.

References:

- Firebase anonymous authentication: <https://firebase.google.com/docs/auth/android/anonymous-auth>
- Firebase ID token verification: <https://firebase.google.com/docs/auth/admin/verify-id-tokens>

## Submission And Video Data

A submission declares the number of videos before upload:

```text
submissionId
ownerUid
guestId
expectedVideoCount = 1 or 2
uploadedVideoCount = 0
status = uploading
```

Video roles are exactly `front` and `rear`. At least one role is required, and the same role cannot be uploaded twice as a different file.

The server records completed videos under a map or equivalent structured Firestore value:

```text
videos.front.relativePath
videos.front.storedName
videos.front.originalFileName
videos.front.sizeBytes
videos.front.uploadedAt

videos.rear.relativePath
videos.rear.storedName
videos.rear.originalFileName
videos.rear.sizeBytes
videos.rear.uploadedAt
```

For compatibility during migration, `nasRelativePath`, `nasStoredName`, and `nasSizeBytes` may mirror the front video when present. New administrator code must use the structured `videos` value.

## Upload Flow

One submission may contain up to two videos, but each HTTP request carries one video. Sequential requests reduce memory, retry, and connection risk on the DS214 and on mobile networks.

1. Ensure a Firebase user exists; sign in anonymously when signed out.
2. Create the Firestore submission with `ownerUid`, `guestId`, expected count, and `status = uploading`.
3. Obtain a current Firebase ID token.
4. Send one video with `submission_id`, `camera_role`, and the bearer token.
5. Verify the token and load the matching Firestore submission.
6. Require matching ownership, `status = uploading`, a valid expected count, and an unused camera role.
7. Check rate limits, free disk space, PHP upload errors, size, extension, and detected MIME type.
8. Move the fully validated temporary file to the final submission folder.
9. Write or update the protected recovery sidecar.
10. Update the Firestore video metadata and uploaded count server-side.
11. Change the status to `waiting_review` only when the uploaded count equals the expected count.
12. Return a stable success response that Android can safely retry.

The idempotency key is the pair `submissionId + cameraRole`. A retry for a previously completed pair returns the existing result and does not create another video.

## Storage Layout

```text
/volume1/web/safeclip-upload/
  api/
    upload.php
    health.php

/volume1/SafeClipUpLoads/
  YYYY/MM/DD/{submissionId}/
    front.<allowed-extension>
    rear.<allowed-extension>
    submission.json

/volume1/SafeClipConfig/
  config.php
  firebase-service-account.json
  token-cache/
  rate-limit/
  logs/
  temp/
```

Only PHP code is placed under the public document root. Videos, configuration, credentials, temporary files, counters, and logs remain outside every Web Station document root.

Original filenames are metadata only. Final names are generated from the allowlisted camera role and validated extension. Canonical path checks must ensure every final or temporary path stays inside its configured root.

## Limits And Validation

- One or two videos per submission.
- One video per HTTP request.
- Maximum 500 MiB per video.
- Allowed extensions: `mp4`, `mov`, `avi`, `ts`.
- MIME allowlist includes the expected video types and explicitly handled MPEG transport stream values.
- Reject PHP upload errors and partial uploads before file inspection.
- Refuse a new upload when free space is below a configurable reserve.
- Default server limit: four video files per Firebase UID per rolling hour.
- Default server limit: thirty video files per source IP per rolling hour.
- Apply a conservative global concurrent-upload limit suitable for the DS214.
- Rate limits are enforced on the server even when the Android app also applies submission limits.

PHP configuration must support a 500 MiB file without loading it into application memory. Set `upload_max_filesize` above 500 MiB, `post_max_size` above that value, permit at least one uploaded file, and use execution/input timeouts suitable for slow mobile connections. Test the final values on the DS214 rather than assuming desktop PHP behavior.

## Public And Internal APIs

The public portal exposes only:

```text
GET  /api/health.php
POST /api/upload.php
```

The health response reveals no DSM version, paths, credentials, capacity, or configuration. The production public root contains no browser upload form.

The internal administrator API remains a separate LAN-only portal on port 8090. It may list submissions, return details, stream front/rear video with HTTP Range support, and update allowlisted Firestore statuses. It must not share public upload routes or become reachable through the router.

## Web Station And Permissions

- Keep the existing website service and document root unchanged.
- Create a separate SafeClip PHP 8.0 profile with `display_errors` disabled.
- Enable only required extensions, including `curl`, `openssl`, and `fileinfo`.
- Apply an `open_basedir` that includes only the SafeClip public code and required SafeClip storage/configuration paths.
- Use the built-in Nginx/PHP-FPM path first; Apache is not a default requirement.
- Grant the Web Station service identity read/write access only where uploads, temporary data, counters, logs, and token cache require it.
- Keep guest and ordinary NAS users denied from `SafeClipConfig`.
- Give designated company administrators read access to stored videos through the internal workflow or controlled SMB permissions.

The service account credential is a sensitive fallback boundary. It must use the minimum Firestore IAM role available for the required server reads and updates, remain outside web roots, and never be returned or logged. Application code still limits access to the submissions collection and allowlisted field changes because IAM does not provide field-level restrictions.

## Network And TLS

- Keep public router forwarding limited to TCP 80 and 443.
- Use `upload.seyoungi.com` as the Android production base URL.
- Obtain a trusted certificate containing `upload.seyoungi.com` and assign it to the upload Web Station service.
- Redirect the upload host from HTTP to HTTPS after certificate issuance and verification.
- Allow ports 8090, 5001, and 445 only from the company LAN in DSM firewall rules.
- Never trust `X-Forwarded-For` for security decisions unless a controlled reverse proxy is explicitly introduced later.

Synology documents port 443 as the standard Web Station HTTPS port and supports assigning certificates to NAS services:

- DSM network settings: <https://kb.synology.com/en-global/DSM/help/DSM/AdminCenter/connection_network_dsmsetting>
- DSM certificate management: <https://kb.synology.com/es-es/DSM/help/DSM/AdminCenter/connection_certificate?version=7>

## Error Handling

The public API uses JSON and stable HTTP status codes:

```text
400 malformed request or invalid submission fields
401 missing, expired, or invalid Firebase token
403 token UID does not own the submission
404 submission does not exist
409 invalid submission state or conflicting camera role
413 file exceeds 500 MiB
415 unsupported extension or detected media type
429 UID/IP/concurrency limit exceeded
507 insufficient NAS storage
500 internal failure with no sensitive details
```

An interrupted or rejected upload removes its temporary file. If final storage succeeds but Firestore update fails, retain a recovery sidecar and let an idempotent retry complete reconciliation. Logs include a request identifier, timestamp, source IP, submission ID, camera role, result code, and byte count, but never the bearer token or credential contents.

## Administrator Review

The administrator portal stays LAN-only without administrator login for the first MVP. Anyone on the permitted company LAN can therefore view videos and change statuses; authentication must be added before widening access.

The review workspace supports front/rear selection, native video playback with Range seeking, Firestore metadata, and allowlisted status changes. No list, read, delete, or video endpoint is placed on the public upload host.

## Build And Deployment Order

1. Record and back up the current website document root, Web Station portals, PHP profiles, certificates, firewall rules, and router forwarding.
2. Create `SafeClipUpLoads` and `SafeClipConfig` with guest access denied.
3. Install or verify Web Station and PHP 8.0, then create the isolated PHP profile.
4. Obtain and assign the `upload.seyoungi.com` certificate.
5. Create the hostname-based upload portal without altering the existing `www` service.
6. Deploy a minimal health endpoint and reconfirm the company website.
7. Enable Firebase Anonymous Authentication and update Firestore ownership fields/rules.
8. Deploy token verification, Firestore access, validation, rate limiting, idempotency, and storage logic.
9. Test with a small PC upload, then malformed requests, then a near-limit file, then a two-video submission.
10. Connect Android sequential upload with progress and retry.
11. Deploy the LAN-only administrator API and connect the existing administrator UI.

## Verification

### Existing Website

- `https://www.seyoungi.com` remains valid and unchanged after every portal or certificate change.
- Its certificate, redirects, static assets, and PHP behavior remain correct.

### Public Upload

- `upload.seyoungi.com` resolves to the fixed public IP.
- HTTPS presents a certificate valid for the upload hostname.
- Missing, forged, expired, wrong-project, and wrong-owner tokens are rejected.
- Valid Google/email and anonymous users can upload their own submission.
- Guest ID spoofing does not grant access.
- Unsupported, oversized, duplicate-role, interrupted, and over-limit uploads are handled as designed.
- One- and two-video submissions reach `waiting_review` only after all expected files exist.
- Retrying a completed role does not duplicate data.
- No public route can list, read, play, or delete stored videos.

### Internal Administration

- Both front and rear video stream and seek on the LAN.
- Status changes update Firestore and appear in the Android submission history.
- The administrator portal, DSM, SMB, and video endpoints are unreachable from mobile data.

### Recovery

- A Firestore update failure after storage can be reconciled from `submission.json`.
- Temporary files are cleaned after rejected or interrupted requests.
- Low-disk and credential failures do not expose paths, stack traces, tokens, or secrets.

## Explicitly Deferred

- Public administrator access.
- Named administrator login and role management.
- Firebase Storage for video bytes.
- Cloud Functions or Cloud Run upload relay.
- AI analysis, license-plate recognition, and automatic reporting.
- Video deletion and retention automation.
- More than two videos per submission.
- Chunked or resumable multipart protocol beyond retrying one complete 500 MiB role.
