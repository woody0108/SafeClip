# NAS-PC Hybrid Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `http://192.168.0.3:8080/` the only review UI while `192.168.0.37` provides authenticated background H.264 conversion and the existing NAS-queue AI worker.

**Architecture:** NAS PHP owns all operator-facing pages and queues AI jobs in `/volume1/SafeClipUpLoads/_ai`. A PC-only PHP router exposes token-protected `/health` and `/preview` endpoints on port `8091`; NAS proxies preview byte ranges to it, while the existing Python worker polls the shared AI queue independently.

**Tech Stack:** PHP 8, Synology Web Station, Windows PowerShell, Windows Firewall, ffmpeg, Python worker, SMB, vanilla JavaScript.

## Global Constraints

- The only operator UI is `http://192.168.0.3:8080/`.
- The PC fixed address is `192.168.0.37`; the NAS address is `192.168.0.3`.
- The PC processing port is `8091` and accepts requests only from the NAS.
- Browsers never connect directly to the PC processing API.
- AI jobs remain file-queue based under `SafeClipUpLoads/_ai`.
- Configuration secrets stay in ignored `config.php` files and are never placed in frontend JavaScript.
- Existing user changes outside the listed files are not modified.

---

### Task 1: PC Processing API Security Boundary

**Files:**
- Create: `company-web-server/pc-router.php`
- Create: `company-web-server/api/pc_processing_helpers.php`
- Create: `company-web-server/api/pc-preview.php`
- Modify: `company-web-server/config.example.php`
- Create: `company-web-server/tests/Check-PCProcessingHelpers.php`

**Interfaces:**
- Consumes: `app_config()`, `upstream_company_payload()`, `resolve_video_file_path()`, and preview conversion helpers from the existing PHP server.
- Produces: `pc_processing_authorized(array $config, string $remoteAddress, string $providedToken): bool`, `GET /health`, and `GET /preview?id=<submission>&file=<index>`.

- [ ] **Step 1: Write failing helper tests**

Add assertions that only `192.168.0.3` plus the configured token is accepted, mismatched tokens fail, missing tokens fail, and a caller cannot supply a filesystem path.

```php
$config = ['pc_processing_token' => 'test-secret', 'pc_allowed_nas_ip' => '192.168.0.3'];
assert_true(pc_processing_authorized($config, '192.168.0.3', 'test-secret'));
assert_true(!pc_processing_authorized($config, '192.168.0.4', 'test-secret'));
assert_true(!pc_processing_authorized($config, '192.168.0.3', 'wrong'));
```

- [ ] **Step 2: Verify the helper test fails**

Run: `C:\php\php.exe company-web-server\tests\Check-PCProcessingHelpers.php`

Expected: FAIL because `pc_processing_authorized()` is undefined.

- [ ] **Step 3: Implement the minimal security helper and router**

The helper trims configuration values and uses `hash_equals()`. The router accepts only `/health` and `/preview`; every other path returns HTTP 404 JSON. `/health` also requires `X-SafeClip-Worker-Token` so the processing port exposes no unauthenticated service details.

```php
function pc_processing_authorized(array $config, string $remoteAddress, string $providedToken): bool
{
    $allowedIp = trim((string)($config['pc_allowed_nas_ip'] ?? ''));
    $expected = (string)($config['pc_processing_token'] ?? '');
    return $allowedIp !== '' && $remoteAddress === $allowedIp
        && $expected !== '' && hash_equals($expected, $providedToken);
}
```

- [ ] **Step 4: Implement the PC preview endpoint**

Validate `id` and `file`, retrieve trusted attachment metadata from the NAS submissions API, resolve the SMB source file, convert once to the existing H.264 cache, and stream HTTP byte ranges. Do not accept `path`, ffmpeg arguments, or output filenames from the request.

- [ ] **Step 5: Run focused tests and PHP syntax checks**

Run:

```powershell
C:\php\php.exe company-web-server\tests\Check-PCProcessingHelpers.php
C:\php\php.exe -l company-web-server\pc-router.php
C:\php\php.exe -l company-web-server\api\pc-preview.php
```

Expected: helper checks pass and both files report no syntax errors.

### Task 2: NAS-to-PC Preview Proxy

**Files:**
- Modify: `company-web-server/api/preview.php`
- Modify: `company-web-server/api/submissions.php`
- Modify: `company-web-server/api/bootstrap.php`
- Modify: `company-web-server/assets/app.js`
- Modify: `company-web-server/config.example.php`
- Create: `company-web-server/tests/Check-PreviewProxyHelpers.php`
- Modify: `company-web-server/tests/Check-CompanyWebServer.ps1`

**Interfaces:**
- Consumes: `pc_processing_base_url`, `pc_processing_token`, request `Range`, submission ID, and attachment index.
- Produces: same-origin NAS `api/preview.php` with streaming status and headers preserved from the PC.

- [ ] **Step 1: Write failing proxy helper tests**

Test URL construction, allowed forwarded response headers, and rejection of a blank PC base URL or token.

```php
$url = pc_preview_url('http://192.168.0.37:8091', 'abc', 2);
assert_same('http://192.168.0.37:8091/preview?id=abc&file=2', $url);
assert_true(pc_preview_response_header_allowed('Content-Range'));
assert_true(!pc_preview_response_header_allowed('Set-Cookie'));
```

- [ ] **Step 2: Verify the proxy helper test fails**

Run: `C:\php\php.exe company-web-server\tests\Check-PreviewProxyHelpers.php`

Expected: FAIL because the proxy helpers are undefined.

- [ ] **Step 3: Implement streaming cURL proxy behavior**

Forward only `Range` and `X-SafeClip-Worker-Token`. Preserve status, `Content-Type`, `Accept-Ranges`, `Content-Length`, and `Content-Range`. Stream chunks through `CURLOPT_WRITEFUNCTION` without loading the converted video into PHP memory. Use a conversion timeout of 600 seconds and a connection timeout of 5 seconds.

- [ ] **Step 4: Make NAS submissions advertise only the NAS preview endpoint**

Remove normal-operation dependence on `external_preview_base_url`. Set `previewBaseUrl` to `api/preview.php` when PC processing is configured, so the browser remains on the NAS origin.

- [ ] **Step 5: Add visible player failure copy**

On the video element `error` event, display `PC 영상 변환 서버를 확인하세요.` while keeping submission metadata and inquiry controls usable.

- [ ] **Step 6: Run proxy and company-web tests**

Run:

```powershell
C:\php\php.exe company-web-server\tests\Check-PreviewProxyHelpers.php
powershell -ExecutionPolicy Bypass -File company-web-server\tests\Check-CompanyWebServer.ps1
```

Expected: both suites pass.

### Task 3: Background-Only PC Startup

**Files:**
- Create: `company-web-server/Start-PCProcessingServer.ps1`
- Modify outside repository during deployment: `C:\SafeClipAI\Start-SafeClipPC.ps1`
- Reuse outside repository: `C:\SafeClipAI\Start-LocalWorker.ps1`

**Interfaces:**
- Consumes: PC address `192.168.0.37`, port `8091`, repository path, local ignored config, and NAS SMB paths.
- Produces: one PHP processing process and one Python AI worker; no browser window and no local review frontend.

- [ ] **Step 1: Add startup script static checks**

Extend `Check-CompanyWebServer.ps1` to require `pc-router.php`, port `8091`, and absence of `Start-Process <review URL>` from the PC processing startup script.

- [ ] **Step 2: Verify the static check fails**

Run: `powershell -ExecutionPolicy Bypass -File company-web-server\tests\Check-CompanyWebServer.ps1`

Expected: FAIL because `Start-PCProcessingServer.ps1` does not exist.

- [ ] **Step 3: Implement idempotent startup**

Check the NAS share, start PHP only when `192.168.0.37:8091` is not listening, start the Python worker only when its command line is absent, and poll authenticated `/health`. Use hidden process windows and do not call `Start-Process` with a browser URL.

- [ ] **Step 4: Update the deployed wrapper safely**

Back up `C:\SafeClipAI\Start-SafeClipPC.ps1`, then replace its local-frontend launch with calls to the versioned processing startup and existing AI worker arguments.

- [ ] **Step 5: Verify local frontend retirement**

Run the wrapper and verify:

```powershell
Get-NetTCPConnection -LocalPort 8091 -State Listen
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
```

Expected: `8091` listens on `192.168.0.37`; the PC-owned `127.0.0.1:8080` process is absent.

### Task 4: Firewall and Runtime Configuration

**Files:**
- Modify ignored local file: `company-web-server/config.php`
- Modify NAS file with backup: `\\SyDisk\SafeClip\config.php`
- Create: `company-web-server/Install-PCProcessingFirewall.ps1`

**Interfaces:**
- Consumes: a generated random token, NAS IP `192.168.0.3`, PC IP `192.168.0.37`, and port `8091`.
- Produces: matching PC/NAS configuration and a scoped inbound Windows Firewall rule.

- [ ] **Step 1: Generate a 32-byte random token without printing it**

Store the token directly into local and NAS PHP configuration. Never place it in command output, Git, JavaScript, or the final report.

- [ ] **Step 2: Add configuration values**

PC config receives `pc_processing_token`, `pc_allowed_nas_ip`, NAS SMB storage paths, NAS upstream URL, ffmpeg path, and preview cache path. NAS config receives `pc_processing_base_url = http://192.168.0.37:8091` and the matching token.

- [ ] **Step 3: Install the scoped firewall rule**

Create an inbound TCP rule for local port `8091`, remote address `192.168.0.3`, private profile only. Remove or disable any older broad SafeClip processing rule with the same display name before creating the scoped rule.

- [ ] **Step 4: Verify access boundaries**

From the PC, an unauthenticated health request must return 403. From NAS PHP with the configured token, health must return 200. A request to `/` must return 404 JSON.

### Task 5: NAS Deployment and End-to-End Verification

**Files:**
- Deploy with backup: `company-web-server/api/*.php` to `\\SyDisk\SafeClip\api\`
- Deploy: `company-web-server/assets/app.js` to `\\SyDisk\SafeClip\assets\app.js`
- Deploy only changed public files required by the NAS page.

**Interfaces:**
- Consumes: completed Tasks 1-4.
- Produces: a single NAS operator UI with PC-backed preview and queue-backed AI.

- [ ] **Step 1: Back up the current NAS deployment**

Create a timestamped backup directory under the NAS SafeClip deployment and copy every file that will be replaced. Verify all resolved backup and destination paths remain under `\\SyDisk\SafeClip` before copying.

- [ ] **Step 2: Deploy changed NAS files**

Copy only the verified PHP, JavaScript, and configuration changes. Confirm deployed PHP files begin with `3C 3F 70 68` and contain no UTF-8 BOM.

- [ ] **Step 3: Run full automated verification**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File company-web-server\tests\Check-CompanyWebServer.ps1
python -m pytest -q
```

Expected: all company-web checks and all AI worker tests pass.

- [ ] **Step 4: Verify NAS-only video preview**

Open `http://192.168.0.3:8080/`, select the known HEVC submission, and verify the player reaches ready state through NAS `api/preview.php`. Confirm no browser request targets `127.0.0.1:8080` or `192.168.0.37:8091`.

- [ ] **Step 5: Verify NAS-only AI analysis**

Click AI analysis on the NAS page, verify pending then running then completed, and confirm result text and evidence links load from the NAS origin.

- [ ] **Step 6: Verify unaffected workflows**

Confirm inquiry list/reply, review-state update, Firestore submission list, and Android submission-status refresh still work.

- [ ] **Step 7: Record operational commands**

Update `company-web-server/README.md` with the single NAS URL, background startup command, health troubleshooting, PC-off behavior, and rollback backup location. Do not document the shared token value.

