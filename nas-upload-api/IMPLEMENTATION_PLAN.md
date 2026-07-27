# SafeClip NAS Upload API Implementation Plan

## Goal

Build a small PHP 8.0 upload receiver for the company Synology NAS. The first server only accepts video uploads from the internal company network and stores them under the NAS shared folder that will later be exposed as `\\SyDisk\SafeClipUpLoads`.

## Scope

- Create a separate `nas-upload-api/` folder.
- Do not modify the Android app.
- Provide one upload endpoint: `public/upload.php`.
- Store uploaded files outside the web-served PHP folder.
- Start with a simple upload key.
- Allow only `mp4`, `mov`, `avi`, and `ts`.
- Do not provide list, read, download, or delete APIs in this MVP.

## NAS Assumptions

- Synology DS214.
- Web Station with PHP 8.0.
- Internal address similar to `192.168.0.3:5000` for DSM.
- Upload storage share will be available to admins as `\\SyDisk\SafeClipUpLoads`.
- PHP config should point to the NAS filesystem path for that share, for example `/volume1/SafeClipUpLoads`.

## Files

- `nas-upload-api/README.md`: setup and PC test instructions.
- `nas-upload-api/config.example.php`: copy to `config.php` on the NAS and edit secret/storage path.
- `nas-upload-api/public/upload.php`: single upload API endpoint.
- `nas-upload-api/public/test-upload.html`: browser-based manual test page.
- `nas-upload-api/tests/UploadApiStaticTests.ps1`: local static guard tests for important MVP requirements.

## API Contract

Request:

- Method: `POST`
- File field: `video`
- Auth: `X-SafeClip-Upload-Key` header or `upload_key` form field
- Optional fields: `submission_id`, `original_file_name`, `device_label`, `memo`

Success response:

```json
{
  "ok": true,
  "stored_name": "20260721_143000_submission-example.mp4",
  "relative_path": "2026/07/21/20260721_143000_submission-example.mp4",
  "size_bytes": 1234567
}
```

Failure response:

```json
{
  "ok": false,
  "error": "message"
}
```

## Security Notes

- Keep `guest` blocked on the NAS share.
- Use a long random upload key, not a short password.
- Keep the upload storage folder outside the Web Station public folder.
- Keep Web Station internal-only; do not configure port forwarding for this MVP.
- Set PHP upload size limits intentionally before testing large blackbox files.

## Manual Verification

1. Copy `config.example.php` to `config.php` on the NAS.
2. Set `upload_key` and `storage_dir`.
3. Put `public/` under the Web Station site folder.
4. Upload a small video with `curl` or `test-upload.html`.
5. Confirm the file appears in `\\SyDisk\SafeClipUpLoads`.
