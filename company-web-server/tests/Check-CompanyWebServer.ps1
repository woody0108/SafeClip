$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')

function Assert-FileContains {
    param(
        [string] $Path,
        [string] $Pattern,
        [string] $Message
    )

    $content = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
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
    'api/asks.php',
    'api/diagnostics.php',
    'api/video.php',
    'api/analysis.php',
    'api/analysis-evidence.php',
    'api/analysis_helpers.php',
    'api/preview.php',
    'tests/Check-AnalysisHelpers.php',
    'tests/Check-VideoRequestHelpers.php',
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
$asksPhp = Join-Path $root 'api/asks.php'
$diagnosticsPhp = Join-Path $root 'api/diagnostics.php'
$videoPhp = Join-Path $root 'api/video.php'
$submissionsPhp = Join-Path $root 'api/submissions.php'
$config = Join-Path $root 'config.example.php'
$bootstrapPhp = Join-Path $root 'api/bootstrap.php'

Assert-FileContains $index 'aria-current="page"' 'Sidebar must not expose fake clickable navigation buttons.'
Assert-FileContains $appJs 'api/submissions\.php' 'Admin UI must read submissions from the server API.'
Assert-FileContains $appJs 'api/status\.php' 'Admin UI must call the status API.'
Assert-FileContains $appJs 'api/asks\.php' 'Admin UI must call the ask API.'
Assert-FileContains $appJs 'filteredSubmissions' 'Admin UI must filter submissions.'
Assert-FileContains $appJs 'filteredAsks' 'Admin UI must filter asks.'
Assert-FileContains $appJs 'api/video\.php' 'Admin UI must show videos through the server video API.'
Assert-FileContains $appJs 'status-review-completed' 'Review complete button must use the Korean review-completed Firestore status.'
Assert-FileContains $appJs 'statuses' 'Admin UI must render all fixed status buttons.'
Assert-FileContains $appJs 'updateSelectedStatus' 'Admin UI must update any selected status.'
Assert-FileContains $appJs 'companyComment' 'Admin UI must send and render company comments.'
Assert-FileContains $appJs 'commentForStatusChange' 'Admin UI must request a comment for comment-required statuses.'
Assert-FileContains $index 'status-actions' 'Admin UI must expose a status action area.'
Assert-FileContains $index 'detail-company-comment' 'Admin UI must show company comments in submission detail.'
Assert-FileContains $appJs 'formatApiError' 'Admin UI must show clear setup errors from the API.'
Assert-FileContains $appJs 'selectedFileIndex' 'Admin UI must track the selected attachment inside a submission.'
Assert-FileContains $appJs 'data-file-filter' 'Admin UI must provide all/video/photo file tabs.'
Assert-FileContains $appJs 'submitterLabel' 'Admin UI must show submitter nickname/guest label.'
Assert-FileContains $appJs 'incidentDate' 'Admin UI must show incident date from Firestore.'
Assert-FileContains $appJs 'incidentTime' 'Admin UI must show incident time from Firestore.'
Assert-FileContains $submissionsPhp 'incidentLocationDetail' 'Submissions API must include location detail from Firestore.'
Assert-FileContains $index 'detail-location-detail' 'Admin UI must show location detail in submission detail.'
Assert-FileContains $index 'ai-analysis-button' 'Admin UI must expose the AI analysis button.'
Assert-FileContains $index 'ai-evidence' 'Admin UI must expose the AI evidence area.'
Assert-FileContains $index 'operation-mode' 'Admin UI must show whether it is using NAS or sample mode.'
Assert-FileContains $appJs 'api/analysis\.php' 'Admin UI must call the AI analysis API.'
Assert-FileContains $appJs 'renderAnalysis' 'Admin UI must render AI analysis state.'
Assert-FileContains $appJs 'evidenceUrls' 'Admin UI must render only server-approved evidence URLs.'
Assert-FileContains $appJs 'previewBaseUrl' 'Admin UI must use the PC-generated browser preview when configured.'
Assert-FileContains $submissionsPhp 'incidentLatitude' 'Submissions API must include latitude from Firestore.'
Assert-FileContains $submissionsPhp 'incidentLongitude' 'Submissions API must include longitude from Firestore.'
Assert-FileContains $submissionsPhp 'kakaoMapJavascriptKey' 'Submissions API must expose the configured Kakao JavaScript key.'
Assert-FileContains $index 'detail-map' 'Admin UI must include a Kakao map container.'
Assert-FileContains $appJs 'renderDetailMap' 'Admin UI must render the selected submission location map.'
Assert-FileContains $appJs 'addressSearch' 'Admin UI must fall back to address geocoding when coordinates are missing.'
Assert-FileContains $appJs 'fileSizeLabel' 'Admin UI must show Firestore file size.'
Assert-FileContains $statusPhp 'allowedStatuses' 'Status API must allow the Korean fixed status list.'
Assert-FileContains $statusPhp 'companyComment' 'Status API must write companyComment.'
Assert-FileContains $statusPhp 'updatedAt' 'Status API must update updatedAt.'
Assert-FileContains $statusPhp 'upstream_http_request' 'Local status API must proxy review changes to the NAS server.'
Assert-FileContains $asksPhp "collectionId' => 'ask'" 'Ask API must read the ask collection.'
Assert-FileContains $asksPhp 'questionType' 'Ask API must include the question type field.'
Assert-FileContains $asksPhp 'answer' 'Ask API must write answers.'
Assert-FileContains $asksPhp "action === 'delete'" 'Ask API must delete ask documents.'
Assert-FileContains $index 'ask-view' 'Admin UI must include the ask response view.'
Assert-FileContains $index 'data-submission-filter="waiting"' 'Admin UI must include waiting submission filter.'
Assert-FileContains $index 'data-ask-filter="waiting"' 'Admin UI must include unanswered ask filter.'
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
Assert-FileContains $videoPhp 'proxy_upstream_video' 'Local video API must proxy NAS video Range requests.'
Assert-FileContains (Join-Path $root 'api/preview.php') 'libx264' 'PC preview API must convert unsupported videos to H.264.'
Assert-FileContains $bootstrapPhp 'SafeClipUpLoads' 'Video API must read from the NAS upload storage folder.'
Assert-FileContains $bootstrapPhp 'RecursiveDirectoryIterator' 'Video API must search nested NAS date/time folders.'
Assert-FileContains $bootstrapPhp 'jpg' 'Video API must allow submitted JPEG photos.'
Assert-FileContains $submissionsPhp 'submissions' 'Submissions API must read the Firestore submissions collection.'
Assert-FileContains $submissionsPhp 'sample_submissions_from_folder' 'Submissions API must fall back to NAS sample video folder.'
Assert-FileContains $submissionsPhp "\\$_GET\\['mode'\\]" 'Sample folder mode must be explicit, not the default.'
Assert-FileContains $submissionsPhp 'sampleFolder' 'Submissions API must return sample folder diagnostics.'
Assert-FileContains $submissionsPhp 'save_json_cache' 'Submissions API must save the latest JSON response locally when possible.'
Assert-FileContains $submissionsPhp 'submitterLabel' 'Submissions API must normalize submitter label.'
Assert-FileContains $submissionsPhp 'submittedAt' 'Submissions API must order and display submittedAt.'
Assert-FileContains $bootstrapPhp 'submission_attachments' 'Submissions API must normalize all Firestore attachments.'
Assert-FileContains $submissionsPhp 'raw' 'Submissions API must include raw Firestore fields for inspection.'
Assert-FileContains $config 'service_account_json' 'Config must use a server-side Firebase service account file.'
Assert-FileContains $config 'SafeClipUpLoads' 'Config example must include the NAS upload folder.'

$analysisCheck = & php (Join-Path $root 'tests/Check-AnalysisHelpers.php')
if ($LASTEXITCODE -ne 0 -or $analysisCheck -notmatch 'Analysis helper checks passed') {
    throw 'Analysis helper checks failed.'
}
$analysisCheck

$videoCheck = & php (Join-Path $root 'tests/Check-VideoRequestHelpers.php')
if ($LASTEXITCODE -ne 0 -or $videoCheck -notmatch 'Video request helper checks passed') {
    throw 'Video request helper checks failed.'
}
$videoCheck

$indexContent = Get-Content -LiteralPath $index -Raw -Encoding UTF8
$appContent = Get-Content -LiteralPath $appJs -Raw -Encoding UTF8
if ($indexContent -match 'front-tab|rear-tab') {
    throw 'Video UI must not contain front/rear tabs.'
}
if ($appContent -match 'selectedCamera|frontTab|rearTab|videoPaths') {
    throw 'Admin UI script must not keep front/rear camera state.'
}

Write-Host 'Company web server checks passed.'
