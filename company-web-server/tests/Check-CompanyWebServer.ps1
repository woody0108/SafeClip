$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')

function Assert-FileContains {
    param(
        [string] $Path,
        [string] $Pattern,
        [string] $Message
    )

    $content = Get-Content -LiteralPath $Path -Raw
    if ($content -notmatch $Pattern) {
        throw $Message
    }
}

$requiredFiles = @(
    'README.md',
    'config.example.php',
    'index.html',
    'assets/app.js',
    'assets/styles.css',
    'api/bootstrap.php',
    'api/submissions.php',
    'api/status.php',
    'api/diagnostics.php',
    'api/video.php',
    'data/.gitkeep'
)

foreach ($file in $requiredFiles) {
    $path = Join-Path $root $file
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing required file: $file"
    }
}

$index = Join-Path $root 'index.html'
$appJs = Join-Path $root 'assets/app.js'
$statusPhp = Join-Path $root 'api/status.php'
$diagnosticsPhp = Join-Path $root 'api/diagnostics.php'
$videoPhp = Join-Path $root 'api/video.php'
$submissionsPhp = Join-Path $root 'api/submissions.php'
$config = Join-Path $root 'config.example.php'
$bootstrapPhp = Join-Path $root 'api/bootstrap.php'

Assert-FileContains $index 'aria-current="page"' 'Sidebar must not expose fake clickable navigation buttons.'
Assert-FileContains $appJs 'api/submissions\.php' 'Admin UI must read submissions from the server API.'
Assert-FileContains $appJs 'api/status\.php' 'Admin UI must call the status API.'
Assert-FileContains $appJs 'api/video\.php' 'Admin UI must show videos through the server video API.'
Assert-FileContains $appJs 'completed' 'Review complete button must use the completed Firestore status.'
Assert-FileContains $appJs 'formatApiError' 'Admin UI must show clear setup errors from the API.'
Assert-FileContains $appJs 'videoPath' 'Admin UI must use one video path per submission.'
Assert-FileContains $appJs 'submitterLabel' 'Admin UI must show submitter nickname/email/guest label.'
Assert-FileContains $appJs 'incidentDateTime' 'Admin UI must show incident date/time from Firestore.'
Assert-FileContains $appJs 'fileSizeLabel' 'Admin UI must show Firestore file size.'
Assert-FileContains $statusPhp "'completed'" 'Status API must allow completed.'
Assert-FileContains $statusPhp 'updatedAt' 'Status API must update updatedAt.'
Assert-FileContains $diagnosticsPhp 'serviceAccountExists' 'Diagnostics API must check Firebase service account path.'
Assert-FileContains $diagnosticsPhp 'curlLoaded' 'Diagnostics API must check PHP curl extension.'
Assert-FileContains $diagnosticsPhp 'opensslLoaded' 'Diagnostics API must check PHP openssl extension.'
Assert-FileContains $bootstrapPhp 'Throwable' 'Bootstrap must convert PHP failures to JSON errors.'
Assert-FileContains $bootstrapPhp 'curl_init' 'Bootstrap must detect missing curl extension.'
Assert-FileContains $bootstrapPhp 'openssl_sign' 'Bootstrap must detect signing errors.'
Assert-FileContains $bootstrapPhp 'firebase_error_message' 'Bootstrap must expose Firebase error details.'
Assert-FileContains $bootstrapPhp 'Firebase request failed:' 'Firestore API failures must include Firebase response details.'
Assert-FileContains $videoPhp 'Range' 'Video API must support browser video seeking.'
Assert-FileContains $videoPhp 'resolve_video_file_path' 'Video API must resolve files from the NAS upload storage folder.'
Assert-FileContains $bootstrapPhp 'SafeClipUpLoads' 'Video API must read from the NAS upload storage folder.'
Assert-FileContains $bootstrapPhp 'RecursiveDirectoryIterator' 'Video API must search nested NAS date/time folders.'
Assert-FileContains $bootstrapPhp 'jpg' 'Video API must allow submitted JPEG photos.'
Assert-FileContains $submissionsPhp 'submissions' 'Submissions API must read the Firestore submissions collection.'
Assert-FileContains $submissionsPhp 'sample_submissions_from_folder' 'Submissions API must fall back to NAS sample video folder.'
Assert-FileContains $submissionsPhp "\\$_GET\\['mode'\\]" 'Sample folder mode must be explicit, not the default.'
Assert-FileContains $submissionsPhp 'sampleFolder' 'Submissions API must return sample folder diagnostics.'
Assert-FileContains $submissionsPhp 'save_json_cache' 'Submissions API must save the latest JSON response locally when possible.'
Assert-FileContains $submissionsPhp 'submitterLabel' 'Submissions API must normalize submitter label.'
Assert-FileContains $submissionsPhp 'raw' 'Submissions API must include raw Firestore fields for inspection.'
Assert-FileContains $config 'service_account_json' 'Config must use a server-side Firebase service account file.'
Assert-FileContains $config 'SafeClipUpLoads' 'Config example must include the NAS upload folder.'

$indexContent = Get-Content -LiteralPath $index -Raw
$appContent = Get-Content -LiteralPath $appJs -Raw
if ($indexContent -match 'front-tab|rear-tab') {
    throw 'Video UI must not contain front/rear tabs.'
}
if ($appContent -match 'selectedCamera|frontTab|rearTab|videoPaths') {
    throw 'Admin UI script must not keep front/rear camera state.'
}

Write-Host 'Company web server checks passed.'
