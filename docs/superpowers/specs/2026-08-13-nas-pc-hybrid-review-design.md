# NAS-PC Hybrid Review Design

## Goal

Use `http://192.168.0.3:8080/` as the only company review UI. The NAS owns
submissions, inquiries, review state, and AI job state. The PC at
`192.168.0.37` runs only background video conversion and AI processing.

The existing `http://127.0.0.1:8080/` review frontend is retired from normal
operation.

## Architecture

### NAS

- Serves the company review website at `192.168.0.3:8080`.
- Reads Firestore submissions and inquiry data.
- Reads original videos from `/volume1/SafeClipUpLoads`.
- Creates AI jobs under `/volume1/SafeClipUpLoads/_ai`.
- Reads AI results from the same exchange directory.
- Proxies browser preview requests to the PC conversion API.

### PC

- Uses fixed address `192.168.0.37`.
- Runs a background-only preview API on a dedicated port.
- Reads NAS videos through `\\SyDisk\SafeClipUpLoads`.
- Converts unsupported or HEVC videos to cached H.264 MP4 previews.
- Runs the existing AI worker, which polls the NAS AI exchange directory.
- Does not serve the company review frontend and does not open a browser when
  started.

## Data Flows

### Video Preview

1. The browser requests a video from the NAS website.
2. The NAS preview endpoint sends an authenticated request to the PC preview
   API using submission ID and attachment index.
3. The PC verifies the shared token and caller address, resolves the source
   video from the NAS share, and creates or reuses an H.264 cache file.
4. The PC streams the preview to the NAS, preserving byte-range behavior.
5. The NAS streams the response to the browser from the original NAS origin.

The browser never connects directly to `192.168.0.37`.

### AI Analysis

1. The operator clicks `AI analysis` on the NAS website.
2. NAS PHP validates the selected attachment and writes a pending JSON job.
3. The PC AI worker polls the exchange directory and claims the job.
4. The worker reads the NAS video, analyzes it, and writes result JSON and up
   to three evidence images.
5. The NAS page polls job state and displays the result.

AI analysis does not depend on a long-running NAS-to-PC HTTP request, so PC
restarts and long model runtimes do not lose queued work.

## Security

- The PC preview API binds to the LAN interface on a dedicated port, not port
  8080.
- Windows Firewall allows that port only from NAS address `192.168.0.3`.
- Every NAS-to-PC preview request includes a random shared token stored only in
  local/NAS configuration files.
- The PC API exposes only health and preview endpoints. Directory listing,
  arbitrary paths, uploads, deletion, and shell parameters are not accepted.
- Submission IDs and attachment indexes are resolved through trusted NAS
  metadata; callers cannot provide filesystem paths.
- Existing Firestore and upload credentials are not copied into frontend code.

## Failure Handling

- If the PC is off, the NAS page shows that preview conversion is unavailable
  while submission details, inquiries, and original metadata remain usable.
- AI jobs remain pending while the PC worker is off and continue when it starts.
- Preview conversion uses a cache and lock file to prevent duplicate ffmpeg
  work.
- Timeouts and PC connection failures return JSON or a visible review-page
  message rather than a blank player.
- The startup script verifies the NAS share, starts the preview API and AI
  worker, and reports their health without opening a frontend browser.

## Testing

- Unit/helper tests cover token checks, allowed callers, attachment resolution,
  queue creation, and safe relative paths.
- API tests cover health, unauthorized preview requests, missing videos,
  byte-range responses, and cached conversion.
- End-to-end verification uses the NAS website to preview one HEVC video, queue
  one AI job, display its result, and confirm inquiries still work.
- The PC-local frontend must not be required for any operator workflow.

## Deployment

1. Deploy updated NAS PHP files and configuration.
2. Install the PC background preview configuration and startup script.
3. Add the scoped Windows Firewall rule for `192.168.0.3`.
4. Verify PC health from the NAS.
5. Verify preview and AI analysis from `192.168.0.3:8080`.
6. Keep a backup of the previous NAS deployment for rollback.

