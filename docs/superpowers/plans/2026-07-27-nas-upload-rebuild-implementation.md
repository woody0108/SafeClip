# SafeClip NAS Upload Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the DS214 public upload receiver so authenticated Firebase users, including silent anonymous users, can safely upload one or two videos of at most 500 MiB each while the existing company website remains unchanged.

**Architecture:** `upload.seyoungi.com` is a separate hostname-based Web Station portal on the existing HTTPS port 443. A modular PHP 8.0 API verifies Firebase ID tokens, checks Firestore ownership, stores one `front` or `rear` file per request outside the web root, updates Firestore server-side, and makes retries idempotent. This plan stops after PC-to-NAS verification; Android upload wiring and the LAN administrator API are separate follow-up plans.

**Tech Stack:** Synology DSM 7.1.1, Web Station, PHP 8.0, Nginx/PHP-FPM, Composer, `firebase/php-jwt`, Firestore REST API, PowerShell test tooling

## Global Constraints

- Preserve the existing `www.seyoungi.com` Web Station service, document root, redirects, certificate, and PHP behavior.
- Public router forwarding remains TCP 80 and 443 only.
- Do not forward ports 8090, 5000, 5001, 445, FTP, WebDAV, or any video-read endpoint.
- Public production hostname is exactly `upload.seyoungi.com`.
- Public API exposes only `GET /api/health.php` and `POST /api/upload.php`.
- Remove the app-wide static upload key from the new API contract.
- A valid Firebase ID token and matching Firestore `ownerUid` are required for every upload.
- `guestId` is metadata only and never authorizes an upload.
- A submission accepts one or two roles: `front` and `rear`.
- Each HTTP request carries exactly one video of at most 524,288,000 bytes.
- Allowed extensions are exactly `mp4`, `mov`, `avi`, and `ts`.
- Store videos under `/volume1/SafeClipUpLoads`, never under a web root.
- Store configuration, credentials, cache, counters, locks, logs, and temporary files under `/volume1/SafeClipConfig`.
- Never return or log bearer tokens, credentials, absolute paths, PHP stack traces, or raw Google error bodies.
- Keep the internal administrator portal and Android integration out of this implementation plan.

---

## File Structure

```text
nas-upload-api/
  composer.json
  composer.lock
  config.example.php
  README.md
  docs/
    DSM_SETUP.md
    FIREBASE_SETUP.md
    RECOVERY.md
  public/
    api/
      bootstrap.php
      health.php
      upload.php
  src/
    ApiException.php
    ApiResponse.php
    AuditLogger.php
    Clock.php
    Config.php
    CurlTransport.php
    FileRateLimiter.php
    FirebaseCertificateProvider.php
    FirebaseIdTokenVerifier.php
    FirestoreSubmissionClient.php
    GoogleAccessTokenProvider.php
    HttpResult.php
    HttpTransport.php
    PathGuard.php
    StoredVideo.php
    SubmissionLock.php
    SubmissionRecord.php
    SystemClock.php
    UploadCommand.php
    UploadFileReceiver.php
    UploadPolicy.php
    UploadService.php
    VerifiedUser.php
    autoload.php
  tests/
    Assert.php
    FakeClock.php
    FakeTransport.php
    TestEnvironment.php
    run.php
  tools/
    Test-SafeClipUpload.ps1
tools/
  build-nas-upload-package.ps1
docs/exec-plans/
  2026-07-27-nas-upload-rebuild.md
```

The old `public/test-upload.html` is deleted before production packaging. The build script installs Composer production dependencies into the deployment package; `vendor/` is not committed.

---

### Task 1: Freeze The Public Contract And Test Harness

**Files:**
- Create: `nas-upload-api/composer.json`
- Create: `nas-upload-api/src/autoload.php`
- Create: `nas-upload-api/src/ApiException.php`
- Create: `nas-upload-api/src/ApiResponse.php`
- Create: `nas-upload-api/src/Clock.php`
- Create: `nas-upload-api/src/SystemClock.php`
- Create: `nas-upload-api/tests/Assert.php`
- Create: `nas-upload-api/tests/run.php`
- Replace: `nas-upload-api/config.example.php`
- Modify: `nas-upload-api/tests/UploadApiStaticTests.ps1`

**Interfaces:**
- Produces: `SafeClip\Upload\ApiException`, `ApiResponse`, `Clock`, `SystemClock`, project autoloading, and the executable PHP test harness.
- Consumes: PHP 8.0 with `json`, `openssl`, `curl`, and `fileinfo` extensions.

- [ ] **Step 1: Add the Composer manifest**

Create `composer.json` with PHP 8.0 compatibility, PSR-4 loading, and the established JWT library:

```json
{
  "name": "safeclip/nas-upload-api",
  "type": "project",
  "require": {
    "php": "^8.0",
    "ext-curl": "*",
    "ext-fileinfo": "*",
    "ext-json": "*",
    "ext-openssl": "*",
    "firebase/php-jwt": "^6.11"
  },
  "autoload": {
    "psr-4": {
      "SafeClip\\Upload\\": "src/"
    }
  }
}
```

- [ ] **Step 2: Write the failing API response tests**

Add assertions to `tests/run.php` for stable exception and JSON behavior:

```php
$error = new ApiException(413, 'file_too_large', 'Video exceeds 500 MiB.');
Assert::same(413, $error->statusCode);
Assert::same('file_too_large', $error->errorCode);
Assert::same(
    ['ok' => false, 'error' => ['code' => 'file_too_large', 'message' => 'Video exceeds 500 MiB.']],
    ApiResponse::errorPayload($error)
);
```

- [ ] **Step 3: Run the test to verify RED**

Run: `php nas-upload-api/tests/run.php`

Expected: FAIL because the namespace classes do not exist.

- [ ] **Step 4: Implement the base classes and autoloader**

`ApiException` carries only a status, stable error code, and safe message. `ApiResponse` creates payload arrays and emits JSON only in public endpoint files. `Clock::now(): int` allows deterministic expiry and rate-limit tests.

`src/autoload.php` loads `vendor/autoload.php` and throws a safe startup exception when production dependencies are absent.

- [ ] **Step 5: Replace the example configuration**

The example must contain these exact keys and no `upload_key`:

```php
return [
    'firebase_project_id' => 'safeclip-fd26c',
    'service_account_path' => '/volume1/SafeClipConfig/firebase-service-account.json',
    'storage_dir' => '/volume1/SafeClipUpLoads',
    'work_dir' => '/volume1/SafeClipConfig',
    'max_file_bytes' => 524_288_000,
    'min_free_bytes' => 21_474_836_480,
    'uid_files_per_hour' => 4,
    'ip_files_per_hour' => 30,
    'allowed_extensions' => ['mp4', 'mov', 'avi', 'ts'],
    'allowed_mime_types' => [
        'video/mp4', 'video/quicktime', 'video/x-msvideo',
        'video/mp2t', 'application/octet-stream'
    ],
];
```

`application/octet-stream` is accepted only when the extension is `ts` and the transport-stream signature check succeeds later in `UploadPolicy`.

- [ ] **Step 6: Strengthen static checks**

Update `UploadApiStaticTests.ps1` to require the new endpoint paths, 500 MiB limit, both camera roles, bearer authorization, and absence of `upload_key`, list, read, delete, and test HTML behavior.

- [ ] **Step 7: Run tests and install locked dependencies**

Run:

```powershell
composer install --working-dir nas-upload-api
php nas-upload-api/tests/run.php
powershell -ExecutionPolicy Bypass -File nas-upload-api/tests/UploadApiStaticTests.ps1
```

Expected: PHP base tests and PowerShell static checks pass.

- [ ] **Step 8: Commit**

```bash
git add nas-upload-api/composer.json nas-upload-api/composer.lock nas-upload-api/config.example.php nas-upload-api/src nas-upload-api/tests
git commit -m "refactor(upload): define authenticated upload contract"
```

---

### Task 2: Verify Firebase ID Tokens

**Files:**
- Create: `nas-upload-api/src/HttpResult.php`
- Create: `nas-upload-api/src/HttpTransport.php`
- Create: `nas-upload-api/src/CurlTransport.php`
- Create: `nas-upload-api/src/VerifiedUser.php`
- Create: `nas-upload-api/src/FirebaseCertificateProvider.php`
- Create: `nas-upload-api/src/FirebaseIdTokenVerifier.php`
- Create: `nas-upload-api/tests/FakeClock.php`
- Create: `nas-upload-api/tests/FakeTransport.php`
- Modify: `nas-upload-api/tests/run.php`

**Interfaces:**
- Produces: `FirebaseIdTokenVerifier::verify(string $token): VerifiedUser` and reusable HTTP transport primitives.
- Consumes: `Clock`, project ID, cache directory, Google's Secure Token x509 endpoint, and `firebase/php-jwt`.

- [ ] **Step 1: Write failing certificate-cache tests**

Cover a fresh fetch, cached reuse, expiry refresh, missing `kid`, and network failure with an expired cache. Inject `FakeTransport` and `FakeClock`; do not call Google in unit tests.

```php
$provider = new FirebaseCertificateProvider($transport, $clock, $cacheFile);
$keys = $provider->getCertificates();
Assert::same('TEST CERT', $keys['test-key']);
Assert::same(1, $transport->requestCount());
$provider->getCertificates();
Assert::same(1, $transport->requestCount());
```

- [ ] **Step 2: Run tests to verify RED**

Run: `php nas-upload-api/tests/run.php`

Expected: FAIL because certificate and transport classes are absent.

- [ ] **Step 3: Implement bounded cURL transport and certificate caching**

`HttpTransport::request()` returns:

```php
final class HttpResult {
    public function __construct(
        public int $status,
        public array $headers,
        public string $body
    ) {}
}
```

Set explicit connect and total timeouts, TLS peer verification, a response-size cap for metadata calls, and no automatic forwarding of authorization headers across redirects. Cache Google's x509 JSON using the response `Cache-Control: max-age` and an exclusive file lock.

- [ ] **Step 4: Write failing token-claim tests**

Generate a test RSA key pair in the test environment and sign tokens that separately violate `alg`, `kid`, `exp`, `iat`, `aud`, `iss`, `sub`, and `auth_time`. The valid case must return:

```php
Assert::same('firebase-uid-001', $verifier->verify($token)->uid);
```

- [ ] **Step 5: Implement strict Firebase token verification**

Accept only `RS256`. Select only the certificate matching the untrusted header `kid`, then verify the signature with a `Key($pem, 'RS256')`. After signature verification require:

```text
aud == configured Firebase project ID
iss == https://securetoken.google.com/{projectId}
sub is non-empty and at most 128 characters
exp > now
iat <= now
auth_time <= now
```

Apply at most 60 seconds of clock skew. Map every token failure to the same public `401 invalid_token` response; log only the request ID and internal failure category.

- [ ] **Step 6: Run focused and full tests**

Run: `php nas-upload-api/tests/run.php --group auth`

Expected: all auth tests pass without network access.

Run: `php nas-upload-api/tests/run.php`

Expected: full suite passes.

- [ ] **Step 7: Commit**

```bash
git add nas-upload-api/src nas-upload-api/tests
git commit -m "feat(upload): verify Firebase ID tokens"
```

---

### Task 3: Add Service Account OAuth And Firestore Submission Access

**Files:**
- Create: `nas-upload-api/src/GoogleAccessTokenProvider.php`
- Create: `nas-upload-api/src/SubmissionRecord.php`
- Create: `nas-upload-api/src/FirestoreSubmissionClient.php`
- Modify: `nas-upload-api/tests/run.php`

**Interfaces:**
- Produces: `FirestoreSubmissionClient::find(string $submissionId): SubmissionRecord` and `recordVideo(SubmissionRecord $submission, StoredVideo $video): SubmissionRecord`.
- Consumes: service account JSON outside the repo, `HttpTransport`, `Clock`, and Firestore REST documents under `projects/{projectId}/databases/(default)/documents/submissions`.

- [ ] **Step 1: Write failing OAuth token tests**

Use a temporary test service-account JSON containing a generated test RSA key. Assert the JWT assertion has `iss`, datastore scope, token endpoint audience, `iat`, and `exp = iat + 3600`. Assert a cached access token is reused until 5 minutes before expiry and cache refresh uses a file lock.

- [ ] **Step 2: Run tests to verify RED**

Run: `php nas-upload-api/tests/run.php --group firestore`

Expected: FAIL because OAuth and Firestore classes do not exist.

- [ ] **Step 3: Implement `GoogleAccessTokenProvider`**

Read and validate `client_email`, `private_key`, `project_id`, and `token_uri` from the configured file. Sign the OAuth assertion with RS256, POST form-encoded data to the token endpoint, and cache only the access token and expiry. Do not copy the private key into cache or logs.

- [ ] **Step 4: Write failing Firestore mapping tests**

Feed Firestore REST fixtures through `FakeTransport`. Cover:

- valid `uploading` submission with expected count one and empty videos;
- valid expected count two with existing front video;
- missing document;
- malformed document ID;
- absent or empty `ownerUid`;
- unknown status;
- malformed expected count;
- update-time precondition conflict.

Use this ID rule:

```php
Assert::throws(
    fn() => $client->find('../other-document'),
    ApiException::class,
    'invalid_submission_id'
);
```

- [ ] **Step 5: Implement Firestore value mapping and updates**

Allow submission IDs matching `^[A-Za-z0-9_-]{8,128}$`. Read only the one `submissions/{id}` document. Map `ownerUid`, `status`, `expectedVideoCount`, `uploadedVideoCount`, and front/rear metadata.

`recordVideo()` patches only:

```text
videos.{role}
uploadedVideoCount
status
uploadCompletedAt
updatedAt
nasRelativePath/nasStoredName/nasSizeBytes for front compatibility
```

Use the document `updateTime` as a Firestore precondition. Set `waiting_review` only when both counts match; otherwise keep `uploading`. On a precondition conflict, reload once and classify an already matching role as idempotent or a different role value as `409 upload_conflict`.

- [ ] **Step 6: Run tests**

Run: `php nas-upload-api/tests/run.php --group firestore`

Expected: all Firestore tests pass with no live credentials.

Run: `php nas-upload-api/tests/run.php`

Expected: full suite passes.

- [ ] **Step 7: Commit**

```bash
git add nas-upload-api/src nas-upload-api/tests
git commit -m "feat(upload): connect submissions to Firestore"
```

---

### Task 4: Build File Policy, Rate Limits, Locking, And Storage

**Files:**
- Create: `nas-upload-api/src/UploadCommand.php`
- Create: `nas-upload-api/src/UploadPolicy.php`
- Create: `nas-upload-api/src/PathGuard.php`
- Create: `nas-upload-api/src/FileRateLimiter.php`
- Create: `nas-upload-api/src/SubmissionLock.php`
- Create: `nas-upload-api/src/UploadFileReceiver.php`
- Create: `nas-upload-api/src/StoredVideo.php`
- Modify: `nas-upload-api/tests/run.php`

**Interfaces:**
- Produces: validated `UploadCommand`, idempotency lock, rate-limit decision, and `StoredVideo` metadata.
- Consumes: verified UID, source IP, PHP uploaded-file metadata, submission ID, camera role, configured storage/work roots, and `Clock`.

- [ ] **Step 1: Write failing upload-policy tests**

Cover exactly:

```text
front and rear accepted
unknown or blank role rejected
zero-byte and over-500-MiB files rejected
mp4/mov/avi/ts accepted with matching MIME
double extension and executable extension rejected
application/octet-stream accepted only for a TS signature
PHP partial/no-file/INI-size errors mapped safely
extra uploaded file fields rejected
```

Expected public errors are `400 invalid_request`, `413 file_too_large`, and `415 unsupported_video`.

- [ ] **Step 2: Run policy tests to verify RED**

Run: `php nas-upload-api/tests/run.php --group storage`

Expected: FAIL because policy and storage classes are absent.

- [ ] **Step 3: Implement validation without trusting client names**

Use `finfo(FILEINFO_MIME_TYPE)` on the received temporary file. Derive the extension from the original filename only after matching the strict allowlist. Store the original filename as sanitized metadata, never as a path component.

For `ts` with `application/octet-stream`, read a bounded prefix and require MPEG-TS sync bytes at expected packet offsets. Do not read an entire video into memory.

- [ ] **Step 4: Write failing path and storage tests**

Use a temporary test root. Assert final paths have this shape:

```text
YYYY/MM/DD/{submissionId}/front.mp4
YYYY/MM/DD/{submissionId}/rear.ts
```

Reject traversal, symlink escape, malformed IDs, and any canonical path outside the configured root. Assert the receiver writes first to the work directory, then atomically renames only a validated file. Assert free space below 20 GiB returns `507 insufficient_storage`.

- [ ] **Step 5: Implement path guard and storage receiver**

`UploadFileReceiver` first requires `is_uploaded_file()` in production, then uses `move_uploaded_file()` into a random work path. Unit tests inject a test mover. The final move occurs under a per-submission lock and uses `rename()` on the same volume.

Write `submission.json` through a temporary sidecar plus atomic rename. Sidecar fields are limited to submission ID, owner UID, role, relative path, stored name, original filename, byte size, received time, and Firestore reconciliation state.

- [ ] **Step 6: Write failing rate-limit and lock tests**

Use `FakeClock` to assert four UID files per hour pass, the fifth returns 429, thirty IP files pass, and the thirty-first returns 429. Assert expired buckets are removed and concurrent writes cannot lose increments. Assert a second lock for the same submission times out safely while another submission can proceed.

- [ ] **Step 7: Implement file-backed counters and locks**

Hash UID and IP before using them in counter filenames. Store counters outside the web root, update with `flock(LOCK_EX)`, and never log raw counter file contents. Use a configurable short lock timeout and return `429 upload_busy` when the DS214 is already processing the configured number of uploads.

- [ ] **Step 8: Run tests and commit**

Run: `php nas-upload-api/tests/run.php --group storage`

Expected: all storage tests pass.

Run: `php nas-upload-api/tests/run.php`

Expected: full suite passes.

```bash
git add nas-upload-api/src nas-upload-api/tests
git commit -m "feat(upload): secure video storage and rate limits"
```

---

### Task 5: Orchestrate The Upload Endpoint And Recovery Behavior

**Files:**
- Create: `nas-upload-api/src/Config.php`
- Create: `nas-upload-api/src/AuditLogger.php`
- Create: `nas-upload-api/src/UploadService.php`
- Create: `nas-upload-api/public/api/bootstrap.php`
- Create: `nas-upload-api/public/api/health.php`
- Replace: `nas-upload-api/public/upload.php` with `nas-upload-api/public/api/upload.php`
- Delete: `nas-upload-api/public/test-upload.html`
- Modify: `nas-upload-api/tests/run.php`

**Interfaces:**
- Produces: production `GET /api/health.php` and `POST /api/upload.php` behavior.
- Consumes: all services from Tasks 1-4 and a NAS-only `config.php` outside the public root.

- [ ] **Step 1: Write failing service orchestration tests**

Test the call order and outcomes with fakes:

```text
method check before body work
bearer token required
token verified before Firestore lookup
owner UID equality required
status uploading required
expected count 1 or 2 required
role not declared/already conflicting rejected
rate and disk checks before receiving bytes
successful storage followed by Firestore update
same completed role returns the same result
Firestore failure leaves a recoverable sidecar
temporary file removed on every pre-final failure
```

- [ ] **Step 2: Run endpoint tests to verify RED**

Run: `php nas-upload-api/tests/run.php --group service`

Expected: FAIL because `UploadService` and public bootstrap are absent.

- [ ] **Step 3: Implement `Config` validation and bootstrap**

Fail closed when required paths, project ID, limits, or service-account file are missing. Configure production error handling so PHP notices and exceptions never reach JSON. Create a random request ID for logs and responses.

Do not include `config.php` in the repository. The deployed public endpoint finds the real configuration through an environment value or a fixed path outside the document root, never `../config.php` under the public deployment tree.

- [ ] **Step 4: Implement upload orchestration**

Return this success shape for both first success and idempotent replay:

```json
{
  "ok": true,
  "submissionId": "SUB-2026-0727-0001",
  "cameraRole": "front",
  "relativePath": "2026/07/27/SUB-2026-0727-0001/front.mp4",
  "storedName": "front.mp4",
  "sizeBytes": 123456,
  "uploadedVideoCount": 1,
  "expectedVideoCount": 2,
  "status": "uploading",
  "idempotent": false,
  "requestId": "safe-public-id"
}
```

The second role returns `status = waiting_review`. A replay returns `idempotent = true`.

- [ ] **Step 5: Implement minimal health response**

`GET /api/health.php` returns only:

```json
{"ok":true,"service":"safeclip-upload","version":"1"}
```

It must not inspect or reveal storage capacity, DSM/PHP versions, paths, Firebase state, or credentials.

- [ ] **Step 6: Implement safe audit logging**

Log newline-delimited JSON outside the web root with request ID, time, source IP, submission ID, UID hash, role, result code, and byte count. Strip control characters and omit authorization headers, tokens, credentials, original paths, and exception traces.

- [ ] **Step 7: Run tests, static checks, and commit**

Run:

```powershell
php nas-upload-api/tests/run.php
powershell -ExecutionPolicy Bypass -File nas-upload-api/tests/UploadApiStaticTests.ps1
```

Expected: all tests pass and static checks confirm there is no public read/list/delete/test form or static upload key.

```bash
git add nas-upload-api
git commit -m "feat(upload): expose authenticated upload endpoint"
```

---

### Task 6: Create The Anonymous PC End-To-End Test Client

**Files:**
- Create: `nas-upload-api/tools/Test-SafeClipUpload.ps1`
- Create: `nas-upload-api/tools/README.md`
- Modify: `nas-upload-api/README.md`

**Interfaces:**
- Produces: a PC command that creates a temporary Firebase anonymous user, creates its own Firestore submission, and uploads front/rear files to the NAS.
- Consumes: Firebase Web API key, project ID, upload base URL, one required front file, and one optional rear file.

- [ ] **Step 1: Write parameter and dry-run tests**

The script declares:

```powershell
param(
    [Parameter(Mandatory)] [string] $FirebaseApiKey,
    [Parameter(Mandatory)] [string] $ProjectId,
    [Parameter(Mandatory)] [uri] $UploadBaseUrl,
    [Parameter(Mandatory)] [string] $FrontVideo,
    [string] $RearVideo,
    [switch] $DryRun
)
```

Dry-run validates paths, extensions, and 500 MiB limits without printing the API key or token. Add a PowerShell test mode that asserts missing files, unsupported extensions, and oversized fixtures fail before network access.

- [ ] **Step 2: Implement Firebase anonymous sign-in**

POST to the official Identity Toolkit `accounts:signUp` REST endpoint with `returnSecureToken = true`. Keep the returned ID token only in memory and never write or echo it.

- [ ] **Step 3: Create the Firestore submission as the anonymous user**

Create a unique document ID and write `ownerUid`, a generated PC guest ID, expected count, zero uploaded count, timestamps, and `status = uploading` through Firestore REST using the Firebase ID token. This step verifies that deployed Firestore Security Rules allow an authenticated user to create only their own submission.

- [ ] **Step 4: Upload one or two files sequentially**

Call `/api/upload.php` with bearer authorization, `submission_id`, `camera_role`, and multipart `video`. Print only status, role, bytes, request ID, and final submission status. Do not print the token, API key, service account data, or NAS absolute path.

- [ ] **Step 5: Verify final Firestore state**

Read the submission with the same Firebase token and assert uploaded count, expected count, both requested roles, and `waiting_review`. Fail if a one-file test remains uploading or a two-file test reaches waiting review after only the first file.

- [ ] **Step 6: Document PC commands and commit**

Example:

```powershell
.\nas-upload-api\tools\Test-SafeClipUpload.ps1 `
  -FirebaseApiKey $env:SAFECLIP_FIREBASE_API_KEY `
  -ProjectId safeclip-fd26c `
  -UploadBaseUrl https://upload.seyoungi.com `
  -FrontVideo C:\SafeClipTest\front.mp4 `
  -RearVideo C:\SafeClipTest\rear.mp4
```

```bash
git add nas-upload-api/tools nas-upload-api/README.md
git commit -m "test(upload): add anonymous end-to-end client"
```

---

### Task 7: Build The Deployment Package And DSM Runbook

**Files:**
- Create: `tools/build-nas-upload-package.ps1`
- Create: `nas-upload-api/docs/DSM_SETUP.md`
- Create: `nas-upload-api/docs/FIREBASE_SETUP.md`
- Create: `nas-upload-api/docs/RECOVERY.md`
- Create: `docs/exec-plans/2026-07-27-nas-upload-rebuild.md`
- Modify: `.gitignore`
- Modify: `ARCHITECTURE.md`
- Modify: `nas-upload-api/README.md`

**Interfaces:**
- Produces: a versioned ZIP deployable to Web Station and a beginner-readable, ordered setup checklist.
- Consumes: built/tested PHP source, `composer.lock`, and the confirmed DSM/domain/router environment.

- [ ] **Step 1: Write packaging assertions**

The PowerShell builder must fail unless tests pass and Composer production dependencies install. Its post-build checks require public API files and `vendor/`, and reject:

```text
config.php
firebase-service-account.json
test-upload.html
tests/
.git/
*.log
any file containing the configured private key header
```

- [ ] **Step 2: Implement the package builder**

Build into a temporary workspace, run `composer install --no-dev --classmap-authoritative`, copy only runtime files, write a SHA-256 manifest, and emit `dist/safeclip-nas-upload-{git-short-sha}.zip`. Do not modify or delete NAS files.

- [ ] **Step 3: Write the DSM setup runbook**

Document exact checkpoints in this order:

1. Record screenshots of the current `www.seyoungi.com` portal, PHP profile, certificate assignment, firewall, and router forwarding.
2. Back up the current website document root.
3. Create `SafeClipUpLoads` and `SafeClipConfig`; deny guest access.
4. Verify Web Station and PHP 8.0; enable `curl`, `openssl`, and `fileinfo`; keep `display_errors` off.
5. Set upload limits above 500 MiB and timeouts for slow connections while keeping `memory_limit` low enough not to encourage whole-file buffering.
6. Set the SafeClip PHP `open_basedir` to only required SafeClip roots.
7. Obtain a certificate for `upload.seyoungi.com` and verify `www.seyoungi.com` still presents its original certificate.
8. Create the hostname-based HTTPS portal on 443 with a separate document root.
9. Deploy the ZIP runtime and create the real config outside the web root.
10. Grant the Web Station service identity minimal read/write permissions.
11. Test health internally, then externally, then reconfirm the company website.
12. Remove any staging or browser upload test files.

Each checkpoint contains a stop/rollback condition. Any change that breaks the company website is reverted before continuing.

- [ ] **Step 4: Write Firebase setup and recovery runbooks**

`FIREBASE_SETUP.md` covers enabling Anonymous Authentication, creating a dedicated service account, minimum Firestore IAM, downloading the JSON directly to protected administration storage, and Firestore rules for authenticated users creating only documents whose `ownerUid == request.auth.uid`.

`RECOVERY.md` covers stale temporary files, sidecars whose Firestore update failed, idempotent replay, token-cache deletion, certificate-cache refresh, log rotation, and low-disk response. It never recommends manually changing a submission to `waiting_review` before all expected files exist.

- [ ] **Step 5: Update architecture and execution log**

Mark the old static-key, single-file MVP as superseded. Record that this plan ends at PC validation and does not modify Android or deploy the administrator API.

- [ ] **Step 6: Build, inspect, and commit**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/build-nas-upload-package.ps1
Expand-Archive dist/safeclip-nas-upload-*.zip -DestinationPath $env:TEMP\safeclip-package-check -Force
Get-ChildItem $env:TEMP\safeclip-package-check -Recurse
```

Expected: runtime PHP and Composer vendor files are present; credentials, tests, logs, and test forms are absent.

```bash
git add .gitignore ARCHITECTURE.md nas-upload-api tools/build-nas-upload-package.ps1 docs/exec-plans/2026-07-27-nas-upload-rebuild.md
git commit -m "docs(upload): add DSM deployment runbook"
```

---

### Task 8: Perform Local Verification And Prepare The User NAS Checklist

**Files:**
- Modify: `docs/exec-plans/2026-07-27-nas-upload-rebuild.md`

**Interfaces:**
- Produces: verified source/package evidence and a concise set of NAS actions the user can execute safely.
- Consumes: all deliverables from Tasks 1-7.

- [ ] **Step 1: Run every local automated check fresh**

Run:

```powershell
composer validate --working-dir nas-upload-api --strict
composer audit --working-dir nas-upload-api
php nas-upload-api/tests/run.php
powershell -ExecutionPolicy Bypass -File nas-upload-api/tests/UploadApiStaticTests.ps1
powershell -ExecutionPolicy Bypass -File tools/build-nas-upload-package.ps1
git diff --check
git status --short
```

Expected: zero test failures, no known Composer vulnerability, successful package build, no whitespace errors, and only intentional changes.

- [ ] **Step 2: Inspect the package for secret and public-surface regressions**

Search the extracted package for `PRIVATE KEY`, `upload_key`, `firebase-service-account`, `test-upload`, list/delete/read routes, absolute developer paths, and stack-trace settings. Expected: no secret, static key, test form, or public read behavior.

- [ ] **Step 3: Record checks that require the user's NAS**

The handoff checklist requires the user to verify:

```text
www.seyoungi.com still works before and after every change
upload.seyoungi.com resolves and presents the correct certificate
health endpoint works on company Wi-Fi and mobile data
valid anonymous PC upload succeeds
forged/expired/wrong-owner token fails
front-only submission reaches waiting_review
two-video submission remains uploading after front and reaches waiting_review after rear
replay does not create duplicate files
501 MiB file and unsupported extensions fail
public list/read/delete attempts return 404 or 405
DSM/SMB/admin ports are unreachable from mobile data
```

- [ ] **Step 4: Update execution evidence and commit**

Add commands, dates, pass/fail counts, package filename, and residual NAS-only checks to the execution log. Never paste tokens, API keys, public certificates' private keys, service account JSON, or absolute credential paths beyond the approved generic NAS path.

```bash
git add docs/exec-plans/2026-07-27-nas-upload-rebuild.md
git commit -m "test(upload): record rebuild verification"
```

---

## Follow-Up Plans

After the user completes PC-to-NAS verification, write separate implementation plans in this order:

1. Android anonymous-auth ownership, sequential front/rear upload, progress, retry, and account-link behavior.
2. LAN-only administrator PHP API with front/rear Range streaming and Firestore status updates.
3. Connect the existing administrator React UI to the real LAN API and perform device/network acceptance testing.
