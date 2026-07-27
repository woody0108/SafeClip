$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$apiRoot = Join-Path $repoRoot "nas-upload-api"
$publicRoot = Join-Path $apiRoot "public"
$uploadPhp = Join-Path $publicRoot "upload.php"
$configExample = Join-Path $apiRoot "config.example.php"
$readme = Join-Path $apiRoot "README.md"

function Assert-True($condition, $message) {
    if (-not $condition) {
        throw $message
    }
}

Assert-True (Test-Path -LiteralPath $uploadPhp) "public/upload.php must exist."
Assert-True (Test-Path -LiteralPath $configExample) "config.example.php must exist."
Assert-True (Test-Path -LiteralPath $readme) "README.md must exist."

$uploadSource = Get-Content -LiteralPath $uploadPhp -Raw
$configSource = Get-Content -LiteralPath $configExample -Raw
$readmeSource = Get-Content -LiteralPath $readme -Raw

Assert-True ($uploadSource.Contains("`$_SERVER['REQUEST_METHOD'] !== 'POST'")) "upload.php must reject non-POST requests."
Assert-True ($uploadSource -match "X-SafeClip-Upload-Key") "upload.php must support the X-SafeClip-Upload-Key header."
Assert-True ($uploadSource.Contains("`$_FILES['video']")) "upload.php must use the video file field."
Assert-True ($uploadSource -match "hash_equals") "upload.php must compare upload keys with hash_equals."
Assert-True ($uploadSource -match "move_uploaded_file") "upload.php must use move_uploaded_file."
Assert-True ($uploadSource -match "allowed_extensions") "upload.php must read allowed extensions from config."
Assert-True (-not $uploadSource.Contains("`$_GET['delete']") -and -not $uploadSource.Contains("`$_GET['list']")) "upload.php must not add list or delete behavior."

Assert-True ($configSource -match "upload_key") "config.example.php must define upload_key."
Assert-True ($configSource -match "storage_dir") "config.example.php must define storage_dir."
Assert-True ($configSource -match "/volume1/SafeClipUpLoads") "config.example.php must point to the expected Synology share path."
Assert-True ($configSource -match "mp4" -and $configSource -match "mov" -and $configSource -match "avi" -and $configSource -match "ts") "config.example.php must allow mp4, mov, avi, and ts."
Assert-True ($readmeSource -match "\\\\SyDisk\\SafeClipUpLoads") "README.md must explain where admins retrieve uploaded files."

Write-Host "NAS upload API static checks passed."
