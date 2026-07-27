# SafeClip NAS Upload API

SafeClip company-side upload receiver for a Synology NAS.

This MVP accepts video uploads only. It does not provide read, list, download, or delete APIs. Admins retrieve files later from the NAS shared folder, for example:

```text
\\SyDisk\SafeClipUpLoads
```

## Folder Layout

```text
nas-upload-api/
  config.example.php
  config.php              copied on NAS only, not committed
  public/
    upload.php
    test-upload.html
  tests/
    UploadApiStaticTests.ps1
```

## NAS Setup

1. In DSM Control Panel, create or use the shared folder `SafeClipUpLoads`.
2. Keep `guest` set to no access.
3. Give your admin account read/write access so files can be downloaded later from Windows.
4. If upload returns a permission error, give the Web Station/PHP user read/write permission to `SafeClipUpLoads`.
5. Copy `config.example.php` to `config.php`.
6. Edit `config.php`:

```php
'upload_key' => 'replace-this-with-a-long-random-secret',
'storage_dir' => '/volume1/SafeClipUpLoads',
```

7. Configure Web Station so the web root points to `nas-upload-api/public`, or copy the `public` folder contents into the Web Station site folder while keeping `config.php` outside the public web folder.

## PHP Size Settings

Large blackbox videos need PHP limits high enough for the file size.

Check these PHP settings in Synology Web Station:

```text
upload_max_filesize
post_max_size
max_execution_time
max_input_time
```

For the first test, use a small video under 100 MB. Increase limits only after the small test succeeds.

## PC Upload Test

Use PowerShell with curl:

```powershell
curl.exe -X POST "http://192.168.0.3/safeclip/upload.php" `
  -H "X-SafeClip-Upload-Key: your-long-upload-key" `
  -F "video=@C:\path\to\sample.mp4" `
  -F "submission_id=pc-test-001" `
  -F "device_label=office-pc"
```

Expected success:

```json
{"ok":true,"stored_name":"...","relative_path":"2026/07/21/...","size_bytes":12345}
```

You can also open `test-upload.html` in the browser through Web Station and upload a small video manually.

## Security Rules For MVP

- Keep this internal Wi-Fi only.
- Do not set router port forwarding.
- Do not put uploaded videos under the public web root.
- Use a long random upload key.
- Rotate the upload key if it is shared by mistake.
- Keep list/download/delete APIs out of this MVP.
