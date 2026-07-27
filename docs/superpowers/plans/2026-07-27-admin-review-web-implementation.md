# SafeClip Admin Review Web Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a real internal SafeClip administrator portal that reads Firestore submissions, streams the matching NAS video, and updates the Firestore `status` field consumed by the Android app.

**Architecture:** A React/Vite application calls same-origin PHP 8.0 endpoints hosted on an internal-only Synology Web Station portal. The PHP API validates the company LAN source, uses a dedicated service account to call Firestore REST, and streams files only after resolving a Firestore-owned relative path under `/volume1/SafeClipUpLoads`.

**Tech Stack:** React 19, TypeScript 5.7, Vite 6, Vitest, Testing Library, Lucide React, PHP 8.0, Firestore REST API, Synology Web Station

## Global Constraints

- Do not modify the Android app in this plan.
- Keep `nas-upload-api/` public and write-only; place read/status behavior in new `nas-admin-api/`.
- Host the administrator UI and API only on the company LAN; never configure router forwarding for its port.
- The first LAN allowlist is `192.168.0.0/24`, plus IPv4 and IPv6 loopback for development.
- Do not require administrator sign-in in this MVP; record source IP and status transitions locally.
- Never place a Firebase service account, access token, NAS absolute path, or audit log under a web root or in Git.
- Firestore is the source of truth for submission metadata and `status`; NAS stores only video bytes and recovery sidecars.
- Administrator status values are exactly `waiting_review`, `reviewing`, `needs_more_info`, `report_package_ready`, `rejected`, and `completed`.
- `uploading` is controlled by the Android upload flow and must not appear as an administrator action.
- Video streaming must support HTTP Range and must not load an entire video into PHP memory.
- Omit AI analysis, result transmission, report generation, user management, deletion, and public video access.
- Keep operational controls compact, use Lucide icons, cap radii at 8px, avoid nested cards, and verify desktop and narrow layouts.

---

## File Structure

### React administrator UI

```text
admin-web/src/
  api/adminApi.ts                 typed same-origin HTTP client
  domain/submission.ts            submission/status types and mapping helpers
  domain/submission.test.ts       status, summary, ordering tests
  components/AppShell.tsx         sidebar and top bar
  components/DashboardSummary.tsx real metric strip
  components/SubmissionTable.tsx  filterable submission rows
  components/ReviewWorkspace.tsx  video and metadata workspace
  components/StatusActions.tsx    confirmed status updates
  components/AdminStates.test.tsx loading/empty/error/missing-video tests
  test/setup.ts                    Testing Library setup
  App.tsx                          page state and data orchestration
  styles.css                       responsive SafeClip operational UI
```

### Internal PHP API

```text
nas-admin-api/
  config.example.php
  README.md
  src/AccessGuard.php
  src/AdminConfig.php
  src/AuditLog.php
  src/CurlTransport.php
  src/FirestoreClient.php
  src/FirestoreValue.php
  src/GoogleAccessTokenProvider.php
  src/HttpResult.php
  src/HttpTransport.php
  src/PathGuard.php
  src/RangeParser.php
  src/StatusPolicy.php
  src/autoload.php
  public/api/bootstrap.php
  public/api/submissions.php
  public/api/submission.php
  public/api/status.php
  public/api/video.php
  tests/Assert.php
  tests/FakeTransport.php
  tests/run.php
```

### Deployment

```text
tools/build-nas-admin-package.ps1
docs/exec-plans/2026-07-27-admin-review-web.md
```

---

### Task 1: React Domain Model And API Contract

**Files:**
- Modify: `admin-web/package.json`
- Modify: `admin-web/vite.config.ts`
- Create: `admin-web/src/test/setup.ts`
- Create: `admin-web/src/domain/submission.ts`
- Create: `admin-web/src/domain/submission.test.ts`
- Create: `admin-web/src/api/adminApi.ts`

**Interfaces:**
- Produces: `SubmissionStatus`, `SubmissionSummary`, `SubmissionListItem`, `SubmissionDetail`, `statusLabel`, `statusTone`, `summarizeSubmissions`, and `AdminApi`.
- Consumes: same-origin JSON endpoints under `/api/*.php`.

- [ ] **Step 1: Add the frontend test dependencies and scripts**

Add these dev dependencies and scripts to `admin-web/package.json`:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "test": "vitest run",
    "test:watch": "vitest"
  },
  "devDependencies": {
    "@testing-library/jest-dom": "^6.6.3",
    "@testing-library/react": "^16.1.0",
    "jsdom": "^25.0.1",
    "vitest": "^2.1.8"
  }
}
```

Merge these entries with the existing dependencies rather than replacing them. Configure Vitest in `vite.config.ts` with `environment: "jsdom"` and `setupFiles: "./src/test/setup.ts"`.

- [ ] **Step 2: Write failing status and summary tests**

Create tests that require these exact behaviors:

```ts
expect(statusLabel("waiting_review")).toBe("검토대기");
expect(statusLabel("reviewing")).toBe("검토중");
expect(statusLabel("needs_more_info")).toBe("자료보완필요");
expect(statusLabel("report_package_ready")).toBe("신고자료준비완료");
expect(statusLabel("rejected")).toBe("반려");
expect(statusLabel("completed")).toBe("완료");

expect(summarizeSubmissions(fixtures)).toEqual({
  total: 6,
  waitingReview: 2,
  reviewing: 1,
  completed: 1
});
```

- [ ] **Step 3: Run the tests and verify RED**

Run: `npm install` from `admin-web`, then `npm test -- src/domain/submission.test.ts`

Expected: FAIL because `submission.ts` and its exports do not exist.

- [ ] **Step 4: Implement the domain types and helpers**

Define the status union exactly:

```ts
export type SubmissionStatus =
  | "uploading"
  | "waiting_review"
  | "reviewing"
  | "needs_more_info"
  | "report_package_ready"
  | "rejected"
  | "completed";

export type AdminStatus = Exclude<SubmissionStatus, "uploading">;

export const ADMIN_STATUSES: readonly AdminStatus[] = [
  "waiting_review",
  "reviewing",
  "needs_more_info",
  "report_package_ready",
  "rejected",
  "completed"
];
```

Define list/detail fields matching the current Android Firestore document plus `nasRelativePath`, `nasStoredName`, and `hasVideo`. Unknown Firestore status strings map to `waiting_review`; do not add them to the administrator action list.

- [ ] **Step 5: Implement the typed API client**

Expose this interface:

```ts
export interface AdminApi {
  listSubmissions(filter?: SubmissionStatus): Promise<SubmissionListResponse>;
  getSubmission(id: string): Promise<SubmissionDetail>;
  updateStatus(id: string, status: AdminStatus): Promise<SubmissionDetail>;
  videoUrl(id: string): string;
}
```

Use `fetch` with same-origin paths, `Accept: application/json`, and `Content-Type: application/json` only for status POST. Convert non-2xx JSON `{ ok: false, error }` responses into an `AdminApiError` with a safe user-facing message.

- [ ] **Step 6: Run tests and build**

Run: `npm test -- src/domain/submission.test.ts`

Expected: PASS.

Run: `npm run build`

Expected: PASS with a generated `admin-web/dist`.

- [ ] **Step 7: Commit**

```bash
git add admin-web/package.json admin-web/package-lock.json admin-web/vite.config.ts admin-web/src/test admin-web/src/domain admin-web/src/api
git commit -m "feat(admin): add submission data contract"
```

---

### Task 2: Real Dashboard Shell And Submission Table

**Files:**
- Create: `admin-web/src/components/AppShell.tsx`
- Create: `admin-web/src/components/DashboardSummary.tsx`
- Create: `admin-web/src/components/SubmissionTable.tsx`
- Create: `admin-web/src/components/AdminStates.test.tsx`
- Modify: `admin-web/src/App.tsx`
- Modify: `admin-web/src/styles.css`

**Interfaces:**
- Consumes: `AdminApi.listSubmissions`, `SubmissionListItem`, and `SubmissionSummary` from Task 1.
- Produces: row selection callback `onSelectSubmission(id: string): void` and status filter callback `onFilterChange(status?: SubmissionStatus): void`.

- [ ] **Step 1: Write failing loading, empty, and row-selection tests**

Test all three states with injected fake API data:

```tsx
render(<App api={pendingApi} />);
expect(screen.getByLabelText("제출 목록 불러오는 중")).toBeInTheDocument();

render(<App api={emptyApi} />);
expect(screen.getByText("접수된 제출이 없습니다")).toBeInTheDocument();

render(<App api={successApi} />);
await user.click(await screen.findByText("교통법규 위반"));
expect(successApi.getSubmission).toHaveBeenCalledWith("submission-001");
```

- [ ] **Step 2: Run tests and verify RED**

Run: `npm test -- src/components/AdminStates.test.tsx`

Expected: FAIL because the real components and injected `api` prop do not exist.

- [ ] **Step 3: Implement the operational shell**

Build a fixed desktop sidebar with `LayoutDashboard`, `FileVideo2`, and `ClipboardList` Lucide icons. Use icon buttons with `title`/`aria-label` for sidebar collapse and refresh. Keep only Dashboard, Video review, and Submissions entries.

The top bar contains page title, last refreshed time, and refresh icon. Do not add login, notification, AI, or administrator profile controls.

- [ ] **Step 4: Implement summary and table**

The metric strip displays total, waiting review, reviewing, and completed counts. The table columns are:

```text
Submission ID | Submitted at | Submitter | Type | Status | File
```

Rows are keyboard selectable. The selected row uses a stable highlight without changing row height. Status filters are a compact segmented control, not rounded text pills.

- [ ] **Step 5: Implement loading, empty, and list error states**

Use fixed-height skeleton rows for loading. The error banner keeps the current data visible and exposes a Retry button. Empty results show one refresh command and no explanatory marketing copy.

- [ ] **Step 6: Run tests and visual build checks**

Run: `npm test -- src/components/AdminStates.test.tsx`

Expected: PASS.

Run: `npm run build`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add admin-web/src/App.tsx admin-web/src/styles.css admin-web/src/components
git commit -m "feat(admin): build submission dashboard"
```

---

### Task 3: Review Workspace And Confirmed Status Actions

**Files:**
- Create: `admin-web/src/components/ReviewWorkspace.tsx`
- Create: `admin-web/src/components/StatusActions.tsx`
- Modify: `admin-web/src/components/AdminStates.test.tsx`
- Modify: `admin-web/src/App.tsx`
- Modify: `admin-web/src/styles.css`

**Interfaces:**
- Consumes: `AdminApi.getSubmission`, `AdminApi.videoUrl`, and `AdminApi.updateStatus`.
- Produces: a detail workspace whose confirmed status result replaces the matching item in list state.

- [ ] **Step 1: Write failing review workspace tests**

Cover real behaviors:

```tsx
expect(await screen.findByText("서울특별시 강남구")).toBeInTheDocument();
expect(screen.getByText("급차선 변경")).toBeInTheDocument();
expect(screen.getByLabelText("제출 영상")).toHaveAttribute(
  "src",
  "/api/video.php?id=submission-001"
);
```

For a missing video, assert that submission metadata and status buttons remain visible while the video element is absent.

- [ ] **Step 2: Write the failing status confirmation test**

Click `검토 시작`, confirm the dialog, and assert the UI keeps the old status until `updateStatus` resolves. After resolution, assert `검토중` appears in both detail and table.

- [ ] **Step 3: Run tests and verify RED**

Run: `npm test -- src/components/AdminStates.test.tsx`

Expected: FAIL because the review components are absent.

- [ ] **Step 4: Implement review metadata and video**

Use a native `<video controls preload="metadata">` element. Do not generate thumbnails or keyframes. Render incident date/time, location, type, memo, submitter, original filename, and human-readable file size beside the player.

When `hasVideo` is false, render `연결된 영상이 없습니다` and retain all non-video review controls.

- [ ] **Step 5: Implement status actions**

Expose only these commands:

```text
검토 대기로 이동 -> waiting_review
검토 시작 -> reviewing
자료 보완 요청 -> needs_more_info
신고자료 준비 완료 -> report_package_ready
반려 -> rejected
완료 -> completed
```

Require a native confirmation dialog before `rejected` and `completed`. Disable every status action while one update is pending. Apply the returned server document only after success; preserve the old status and show an inline error on failure.

- [ ] **Step 6: Run tests and build**

Run: `npm test`

Expected: PASS.

Run: `npm run build`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add admin-web/src/App.tsx admin-web/src/styles.css admin-web/src/components
git commit -m "feat(admin): add real review workspace"
```

---

### Task 4: PHP Security And File Boundary Core

**Files:**
- Create: `nas-admin-api/config.example.php`
- Create: `nas-admin-api/src/autoload.php`
- Create: `nas-admin-api/src/AdminConfig.php`
- Create: `nas-admin-api/src/AccessGuard.php`
- Create: `nas-admin-api/src/StatusPolicy.php`
- Create: `nas-admin-api/src/PathGuard.php`
- Create: `nas-admin-api/src/RangeParser.php`
- Create: `nas-admin-api/tests/Assert.php`
- Create: `nas-admin-api/tests/run.php`

**Interfaces:**
- Produces: `AccessGuard::allows(string $ip, array $cidrs): bool`, `StatusPolicy::requireAdminStatus(string $status): string`, `PathGuard::resolve(string $root, string $relative): string`, and `RangeParser::parse(?string $header, int $size): ?ByteRange`.
- Consumes: PHP 8.0 standard library only.

- [ ] **Step 1: Create a minimal test runner and write failing access tests**

The runner exits nonzero on failure and covers:

```php
Assert::true(AccessGuard::allows('192.168.0.42', ['192.168.0.0/24']));
Assert::true(AccessGuard::allows('127.0.0.1', ['127.0.0.1/32']));
Assert::false(AccessGuard::allows('8.8.8.8', ['192.168.0.0/24']));
Assert::false(AccessGuard::allows('192.168.1.42', ['192.168.0.0/24']));
```

- [ ] **Step 2: Add failing policy, path, and range tests**

Require rejection of `uploading`, unknown statuses, absolute paths, `..`, encoded traversal after decoding, missing files, and symlinks that resolve outside storage.

Range cases:

```text
bytes=0-499      -> start 0, end 499
bytes=500-       -> start 500, end size-1
bytes=-500       -> final 500 bytes
bytes=999999-    -> unsatisfiable
bytes=5-4        -> unsatisfiable
multiple ranges  -> unsupported
```

- [ ] **Step 3: Run tests and verify RED**

Run: `php nas-admin-api/tests/run.php`

Expected: FAIL because the production classes do not exist. If `php` is unavailable on the PC, pause PHP implementation and obtain either a local PHP 8 runtime or NAS SSH access; do not claim these tests ran.

- [ ] **Step 4: Implement the minimal security classes**

Use `inet_pton` and bit masks for IPv4 CIDR checks. `PathGuard` must combine the configured root with a slash-normalized relative path, call `realpath` on both, and require the candidate prefix to equal `rootReal . DIRECTORY_SEPARATOR` before returning it.

Represent ranges as:

```php
final class ByteRange {
    public function __construct(
        public readonly int $start,
        public readonly int $end,
        public readonly int $total
    ) {}
}
```

- [ ] **Step 5: Run tests and verify GREEN**

Run: `php nas-admin-api/tests/run.php`

Expected: PASS with a summary count and exit code 0.

- [ ] **Step 6: Commit**

```bash
git add nas-admin-api/config.example.php nas-admin-api/src nas-admin-api/tests
git commit -m "feat(admin-api): add internal security core"
```

---

### Task 5: Google Service Account And Firestore REST Gateway

**Files:**
- Create: `nas-admin-api/src/HttpResult.php`
- Create: `nas-admin-api/src/HttpTransport.php`
- Create: `nas-admin-api/src/CurlTransport.php`
- Create: `nas-admin-api/src/GoogleAccessTokenProvider.php`
- Create: `nas-admin-api/src/FirestoreValue.php`
- Create: `nas-admin-api/src/FirestoreClient.php`
- Create: `nas-admin-api/tests/FakeTransport.php`
- Modify: `nas-admin-api/tests/run.php`

**Interfaces:**
- Produces: `GoogleAccessTokenProvider::accessToken(): string` and `FirestoreClient::{listSubmissions,getSubmission,updateStatus,summary}`.
- Consumes: `HttpTransport::request(string $method, string $url, array $headers, ?string $body): HttpResult`.

- [ ] **Step 1: Write failing service-account token tests**

Use a fixture RSA key generated only for tests. Assert the token provider sends a form request to `https://oauth2.googleapis.com/token` with grant type `urn:ietf:params:oauth:grant-type:jwt-bearer`, caches the returned token until 60 seconds before expiry, and refreshes after expiry.

The signed JWT claims are exact:

```json
{
  "iss": "service-account@example.iam.gserviceaccount.com",
  "scope": "https://www.googleapis.com/auth/datastore",
  "aud": "https://oauth2.googleapis.com/token",
  "iat": 1000,
  "exp": 4600
}
```

- [ ] **Step 2: Write failing Firestore value mapping tests**

Fixtures must decode `stringValue`, `integerValue`, `booleanValue`, `timestampValue`, `nullValue`, `arrayValue`, and `mapValue`. Extract the document ID from the final segment of Firestore's resource `name`.

- [ ] **Step 3: Write failing list/get/update tests with `FakeTransport`**

Assert list uses Firestore `documents:runQuery`, collection `submissions`, descending `createdAt`, and a hard maximum of 100. Assert update calls the document `PATCH` URL with:

```text
updateMask.fieldPaths=status
updateMask.fieldPaths=updatedAt
currentDocument.exists=true
```

The request body contains only Firestore-typed `status` and current UTC `updatedAt` fields.

- [ ] **Step 4: Run tests and verify RED**

Run: `php nas-admin-api/tests/run.php`

Expected: FAIL on missing transport and Firestore classes.

- [ ] **Step 5: Implement transport, token provider, and Firestore decoder**

Use `openssl_sign(..., OPENSSL_ALGO_SHA256)` for RS256 and URL-safe base64 without padding. Use cURL timeouts of 5 seconds to connect and 15 seconds total. Never include HTTP response bodies in public exception messages.

Store the cached token as JSON with `token` and `expiresAt` using `LOCK_EX`, mode `0600` where supported. Protect refresh with `flock` on a sibling lock file.

- [ ] **Step 6: Implement Firestore list/get/update/summary calls**

`summary()` runs count aggregations for total, `waiting_review`, `reviewing`, and `completed`. Return integer zeros only when a successful aggregation has no value; transport/auth failures remain errors.

`listSubmissions()` returns only the fields required by the UI and encodes the last query cursor as URL-safe base64 JSON containing `createdAt` and document name. Decode and validate that shape before using it in a `startAt` cursor.

- [ ] **Step 7: Run tests and verify GREEN**

Run: `php nas-admin-api/tests/run.php`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add nas-admin-api/src nas-admin-api/tests
git commit -m "feat(admin-api): connect Firestore REST"
```

---

### Task 6: Internal List, Detail, And Status Endpoints

**Files:**
- Create: `nas-admin-api/src/AuditLog.php`
- Create: `nas-admin-api/public/api/bootstrap.php`
- Create: `nas-admin-api/public/api/submissions.php`
- Create: `nas-admin-api/public/api/submission.php`
- Create: `nas-admin-api/public/api/status.php`
- Modify: `nas-admin-api/tests/run.php`

**Interfaces:**
- Consumes: `AccessGuard`, `StatusPolicy`, `FirestoreClient`, and `AuditLog`.
- Produces: same-origin JSON contracts consumed by `admin-web/src/api/adminApi.ts`.

- [ ] **Step 1: Write failing endpoint service tests**

Factor request-independent functions so tests can assert:

```php
list_response($firestore, null, 50, null);
detail_response($firestore, $pathGuard, 'submission-001');
status_response($firestore, $audit, '192.168.0.42', [
    'submissionId' => 'submission-001',
    'status' => 'reviewing',
]);
```

Reject blank or malformed document IDs; use `^[A-Za-z0-9_-]{1,150}$`. Reject JSON bodies with keys other than `submissionId` and `status`.

- [ ] **Step 2: Run tests and verify RED**

Run: `php nas-admin-api/tests/run.php`

Expected: FAIL because endpoint services and audit logger are absent.

- [ ] **Step 3: Implement shared bootstrap and JSON response rules**

Bootstrap loads `SAFECLIP_ADMIN_CONFIG` or defaults to `/volume1/SafeClipConfig/admin.php`, starts no session, applies the LAN guard using `REMOTE_ADDR`, and returns:

```json
{"ok":false,"error":"Internal administrator access only."}
```

with `403` for denied clients. Add `Cache-Control: no-store` to JSON endpoints. Never read `X-Forwarded-For`.

- [ ] **Step 4: Implement submissions and detail endpoints**

`submissions.php` accepts only GET and returns:

```json
{
  "ok": true,
  "summary": {"total": 12, "waitingReview": 4, "reviewing": 2, "completed": 3},
  "items": [],
  "nextCursor": null
}
```

`submission.php` accepts only GET, fetches one document, and adds `hasVideo` after safe path resolution. It returns `404` when the document does not exist.

- [ ] **Step 5: Implement status endpoint and audit line**

`status.php` accepts only POST JSON. Fetch the current document, verify the audit destination is writable, apply the allowlisted update, append one JSON line containing timestamp, source IP, submission ID, previous status, and new status, then return the updated detail.

If the audit preflight fails, return `500` before changing Firestore. If the append unexpectedly fails after Firestore succeeds, return `500`, write a PHP server error, and require the UI to refetch the detail before enabling another status action. Do not automatically send the same status update twice.

- [ ] **Step 6: Run tests and verify GREEN**

Run: `php nas-admin-api/tests/run.php`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add nas-admin-api/src/AuditLog.php nas-admin-api/public nas-admin-api/tests/run.php
git commit -m "feat(admin-api): expose internal review endpoints"
```

---

### Task 7: Range-Based NAS Video Streaming

**Files:**
- Create: `nas-admin-api/public/api/video.php`
- Modify: `nas-admin-api/tests/run.php`

**Interfaces:**
- Consumes: `FirestoreClient::getSubmission`, `PathGuard::resolve`, and `RangeParser::parse`.
- Produces: `200` full stream or `206` single-range stream consumed by the native browser video player.

- [ ] **Step 1: Write failing stream-plan tests**

Extract a pure `build_stream_plan(array $submission, string $storageRoot, ?string $rangeHeader): StreamPlan` and assert:

```text
no range       -> 200, full length
bytes=0-999    -> 206, Content-Range bytes 0-999/total
bad range      -> 416, Content-Range bytes */total
no NAS path    -> 404
missing file   -> 404
unsupported ext-> 415
```

- [ ] **Step 2: Run tests and verify RED**

Run: `php nas-admin-api/tests/run.php`

Expected: FAIL because stream planning and endpoint do not exist.

- [ ] **Step 3: Implement safe streaming**

Allow `.mp4`, `.mov`, `.avi`, and `.ts`. Map extensions to conservative video MIME types. Send `Accept-Ranges: bytes`, `Content-Length`, `Content-Type`, `Content-Disposition: inline`, and `Cache-Control: private, no-store`.

Open with `fopen($path, 'rb')`, seek to the range start, and write in 1 MiB chunks while tracking remaining bytes. Stop on client disconnect. Do not use `file_get_contents` or read the entire file.

- [ ] **Step 4: Run tests and verify GREEN**

Run: `php nas-admin-api/tests/run.php`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add nas-admin-api/public/api/video.php nas-admin-api/tests/run.php
git commit -m "feat(admin-api): stream NAS video ranges"
```

---

### Task 8: Upload Recovery Sidecar And NAS Link Contract

**Files:**
- Modify: `nas-upload-api/public/upload.php`
- Modify: `nas-upload-api/tests/UploadApiStaticTests.ps1`
- Modify: `nas-upload-api/README.md`

**Interfaces:**
- Consumes: existing upload fields `submission_id`, `original_file_name`, and `device_label`.
- Produces: atomic `.json` sidecar and existing response fields `stored_name`, `relative_path`, and `size_bytes`.

- [ ] **Step 1: Add a failing sidecar contract test**

Extend the PowerShell static test to require `submission_id`, `received_at`, `relative_path`, `json_encode`, temporary sidecar naming, and `rename` after successful video move.

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File nas-upload-api/tests/UploadApiStaticTests.ps1
```

Expected: FAIL because sidecar behavior is missing.

- [ ] **Step 3: Implement atomic recovery sidecar writing**

After `move_uploaded_file` succeeds, write metadata to `{$targetPath}.json.tmp`, verify `json_encode` succeeded, then rename to `{$targetPath}.json`. If sidecar creation fails, delete the just-moved video and return `500` so storage and metadata cannot silently diverge.

Sidecar keys are exact:

```json
{
  "submission_id": "submission-001",
  "original_file_name": "REC001.mp4",
  "stored_name": "stored.mp4",
  "relative_path": "2026/07/27/stored.mp4",
  "size_bytes": 123,
  "received_at": "2026-07-27T12:00:00+09:00",
  "device_label": "android"
}
```

- [ ] **Step 4: Run static and PHP syntax checks**

Run the PowerShell test and `php -l nas-upload-api/public/upload.php`.

Expected: both PASS. If local PHP is unavailable, run the syntax check on NAS before deployment and report that limitation explicitly.

- [ ] **Step 5: Commit**

```bash
git add nas-upload-api/public/upload.php nas-upload-api/tests/UploadApiStaticTests.ps1 nas-upload-api/README.md
git commit -m "feat(upload): write recovery sidecars"
```

---

### Task 9: Deployment Package And NAS Setup Guide

**Files:**
- Create: `tools/build-nas-admin-package.ps1`
- Create: `nas-admin-api/README.md`
- Create: `docs/exec-plans/2026-07-27-admin-review-web.md`
- Modify: `ARCHITECTURE.md`

**Interfaces:**
- Consumes: `admin-web/dist` and `nas-admin-api/public/api`.
- Produces: `build/nas-admin-site` ready for `/volume1/web/safeclip-admin`.

- [ ] **Step 1: Write a failing package structure check**

The script must fail if `admin-web/dist/index.html` is absent. After copying, it verifies:

```text
build/nas-admin-site/index.html
build/nas-admin-site/assets/
build/nas-admin-site/api/submissions.php
build/nas-admin-site/api/submission.php
build/nas-admin-site/api/status.php
build/nas-admin-site/api/video.php
```

- [ ] **Step 2: Implement the package script**

Use `Resolve-Path` and `Copy-Item` with explicit repository-relative targets. Remove only the previously resolved `build/nas-admin-site` directory after confirming it is under the repository `build` directory. Never touch the NAS share from this script.

- [ ] **Step 3: Document NAS configuration**

The README must include:

1. Create `/volume1/SafeClipConfig` outside the web root.
2. Copy `config.example.php` to `/volume1/SafeClipConfig/admin.php`.
3. Store the dedicated service account JSON there.
4. Grant the dedicated service account a custom IAM role containing only `datastore.entities.get`, `datastore.entities.list`, and `datastore.entities.update`.
5. Grant `http` read access to config/credential, write access to token cache/audit log, and read access to `SafeClipUpLoads`.
6. Enable PHP `curl` and `openssl`.
7. Create a Web Station portal rooted at `/volume1/web/safeclip-admin` on internal port `8090`.
8. Do not configure ipTIME port forwarding for `8090`.
9. Open `http://192.168.0.3:8090` from a company PC.

- [ ] **Step 4: Build and package**

Run:

```powershell
Set-Location admin-web
npm test
npm run build
Set-Location ..
powershell -ExecutionPolicy Bypass -File tools/build-nas-admin-package.ps1
```

Expected: tests and build pass; package structure check passes.

- [ ] **Step 5: Update architecture and execution log**

Record `nas-admin-api/` as the internal review boundary, list verification commands and remaining NAS manual steps, and state that Android NAS upload integration is the next separate implementation plan.

- [ ] **Step 6: Commit**

```bash
git add tools/build-nas-admin-package.ps1 nas-admin-api/README.md docs/exec-plans/2026-07-27-admin-review-web.md ARCHITECTURE.md
git commit -m "docs: add admin NAS deployment workflow"
```

---

### Task 10: End-To-End Verification On NAS

**Files:**
- Modify only if verification reveals a defect in files owned by Tasks 1-9.

**Interfaces:**
- Consumes: deployed internal portal, a real Firestore submission, and its `nasRelativePath` video.
- Produces: evidence that the administrator status reaches the existing Android status reader.

- [ ] **Step 1: Create one controlled Firestore/NAS test pair**

Use a real `submissions/{documentId}` and a small MP4 under `SafeClipUpLoads`. Set the document's `nasRelativePath` to the file's path relative to `/volume1/SafeClipUpLoads`.

- [ ] **Step 2: Verify list, detail, and seek behavior**

Open the internal portal from a company PC. Confirm the submission appears, open it, play the MP4, and seek to at least three non-adjacent timestamps. Browser network tools must show `206 Partial Content` for seek requests.

- [ ] **Step 3: Verify Firestore and Android status propagation**

Change `waiting_review` to `reviewing`. Confirm Firestore contains exactly `status = reviewing` and a new `updatedAt`. Refresh the Android submission history and confirm the Korean label is `검토중`.

- [ ] **Step 4: Verify failure and security cases**

Confirm:

```text
unknown status            -> 400
invalid submission ID     -> 400
missing Firestore document-> 404
missing NAS video         -> detail works, video 404
path traversal value      -> video 404/400 without absolute path leakage
mobile data to port 8090  -> unreachable
company LAN to port 8090  -> reachable
DSM/SMB public exposure   -> absent
```

- [ ] **Step 5: Perform visual verification**

Capture desktop at `1440x900` and narrow viewport at `390x844`. Verify no overlapping controls, clipped Korean text, blank video area when a video exists, nested cards, or layout shifts between loading and loaded table states.

- [ ] **Step 6: Run final automated verification**

Run:

```powershell
Set-Location admin-web
npm test
npm run build
Set-Location ..
powershell -ExecutionPolicy Bypass -File nas-upload-api/tests/UploadApiStaticTests.ps1
php nas-admin-api/tests/run.php
git diff --check
```

Expected: every available command passes. If PHP runs only on NAS, include the NAS command output in the completion report instead of implying it ran locally.

- [ ] **Step 7: Commit verified fixes, if any**

Stage only files changed to resolve observed verification defects, then commit:

```bash
git commit -m "fix: complete admin review verification"
```

Do not create an empty commit when verification required no changes.
