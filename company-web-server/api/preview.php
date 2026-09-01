<?php

declare(strict_types=1);

require __DIR__ . '/bootstrap.php';

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') {
    json_fail(405, 'Only GET is allowed.');
}

$config = app_config();
if (($config['local_preview_enabled'] ?? false) !== true || company_web_upstream_url($config) === '') {
    json_fail(404, 'Local preview is not enabled.');
}

$id = preg_match('/^[A-Za-z0-9._-]{1,200}$/', (string)($_GET['id'] ?? ''))
    ? (string)$_GET['id']
    : '';
$fileIndex = max(0, (int)($_GET['file'] ?? 0));
if ($id === '') {
    json_fail(400, 'Invalid preview request.');
}

$payload = upstream_company_payload($config, 'api/submissions.php');
$relativePath = '';
foreach ((array)($payload['submissions'] ?? []) as $submission) {
    if (!is_array($submission) || (string)($submission['id'] ?? '') !== $id) {
        continue;
    }
    $attachment = (array)(($submission['attachments'] ?? [])[$fileIndex] ?? []);
    if (($attachment['kind'] ?? '') === 'video' && ($attachment['exists'] ?? false) === true) {
        $relativePath = clean_relative_path((string)($attachment['nasRelativePath'] ?? ''));
    }
    break;
}
if ($relativePath === '') {
    json_fail(404, 'Preview source video was not found.');
}

$sourcePath = resolve_video_file_path($config, $relativePath);
if ($sourcePath === '' || !is_file($sourcePath)) {
    json_fail(404, 'NAS source video was not found from this PC.');
}

$cacheDir = rtrim((string)($config['preview_cache_dir'] ?? 'C:/SafeClipAI/preview-cache'), "/\\");
if (!is_dir($cacheDir) && !mkdir($cacheDir, 0770, true) && !is_dir($cacheDir)) {
    json_fail(500, 'Could not create preview cache directory.');
}
$cacheKey = hash('sha256', $sourcePath . ':' . filesize($sourcePath) . ':' . filemtime($sourcePath));
$previewPath = $cacheDir . DIRECTORY_SEPARATOR . $cacheKey . '.mp4';

if (!is_file($previewPath)) {
    create_h264_preview($config, $sourcePath, $previewPath);
}

stream_preview($previewPath);

function create_h264_preview(array $config, string $sourcePath, string $previewPath): void
{
    if (!function_exists('proc_open')) {
        json_fail(500, 'PHP proc_open is required for video preview conversion.');
    }

    $lockPath = $previewPath . '.lock';
    $lock = fopen($lockPath, 'c');
    if ($lock === false || !flock($lock, LOCK_EX)) {
        json_fail(500, 'Could not lock preview conversion.');
    }
    if (is_file($previewPath)) {
        flock($lock, LOCK_UN);
        fclose($lock);
        return;
    }

    $temporaryPath = $previewPath . '.tmp.mp4';
    $command = [
        (string)($config['ffmpeg_path'] ?? 'ffmpeg'),
        '-nostdin', '-hide_banner', '-loglevel', 'error', '-y',
        '-i', $sourcePath,
        '-map', '0:v:0', '-map', '0:a:0?',
        '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '24',
        '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-b:a', '128k',
        '-movflags', '+faststart',
        $temporaryPath,
    ];
    $pipes = [];
    $process = proc_open($command, [
        0 => ['pipe', 'r'],
        1 => ['pipe', 'w'],
        2 => ['pipe', 'w'],
    ], $pipes);
    if (!is_resource($process)) {
        flock($lock, LOCK_UN);
        fclose($lock);
        json_fail(500, 'Could not start ffmpeg.');
    }
    fclose($pipes[0]);
    $stdout = stream_get_contents($pipes[1]);
    $stderr = stream_get_contents($pipes[2]);
    fclose($pipes[1]);
    fclose($pipes[2]);
    $exitCode = proc_close($process);

    if ($exitCode !== 0 || !is_file($temporaryPath)) {
        @unlink($temporaryPath);
        flock($lock, LOCK_UN);
        fclose($lock);
        json_fail(500, 'ffmpeg preview conversion failed: ' . trim((string)($stderr ?: $stdout)));
    }
    if (!rename($temporaryPath, $previewPath)) {
        @unlink($temporaryPath);
        flock($lock, LOCK_UN);
        fclose($lock);
        json_fail(500, 'Could not publish converted preview.');
    }
    flock($lock, LOCK_UN);
    fclose($lock);
    @unlink($lockPath);
}

function stream_preview(string $path): void
{
    $size = filesize($path);
    if ($size === false) {
        json_fail(404, 'Preview file size is unknown.');
    }
    $start = 0;
    $end = $size - 1;
    $status = 200;
    if (isset($_SERVER['HTTP_RANGE']) && preg_match('/bytes=(\d*)-(\d*)/', (string)$_SERVER['HTTP_RANGE'], $matches)) {
        $status = 206;
        if ($matches[1] !== '') $start = (int)$matches[1];
        if ($matches[2] !== '') $end = min((int)$matches[2], $end);
        if ($start > $end) {
            header('Content-Range: bytes */' . $size, true, 416);
            exit;
        }
    }
    $length = $end - $start + 1;
    http_response_code($status);
    header('Content-Type: video/mp4');
    header('Accept-Ranges: bytes');
    header('Content-Length: ' . $length);
    header('Content-Disposition: inline');
    if ($status === 206) header("Content-Range: bytes {$start}-{$end}/{$size}");

    $handle = fopen($path, 'rb');
    if ($handle === false) exit;
    fseek($handle, $start);
    $remaining = $length;
    while ($remaining > 0 && !feof($handle)) {
        $chunk = fread($handle, min(8192, $remaining));
        if ($chunk === false) break;
        echo $chunk;
        $remaining -= strlen($chunk);
        flush();
    }
    fclose($handle);
    exit;
}
