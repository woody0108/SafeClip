<?php

declare(strict_types=1);

require __DIR__ . '/bootstrap.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    json_fail(405, 'Only GET is allowed.');
}

$id = clean_document_id((string)($_GET['id'] ?? ''));
if ($id === '') {
    json_fail(400, 'Invalid video request.');
}

$config = app_config();
$samplePrefix = 'sample-';
if (str_starts_with($id, $samplePrefix)) {
    $sampleName = clean_relative_path(rawurldecode(substr($id, strlen($samplePrefix))));
    $samplePath = resolve_video_file_path($config, $sampleName);
    if ($samplePath === '' || !is_file($samplePath)) {
        json_fail(404, 'Sample video file was not found.');
    }

    stream_video($samplePath, mime_for_extension(strtolower(pathinfo($samplePath, PATHINFO_EXTENSION))));
}

$document = firestore_request($config, 'GET', '/submissions/' . rawurlencode($id));
$fields = firestore_fields($document);
$fileIndex = max(0, (int)($_GET['file'] ?? 0));
$attachments = submission_attachments($config, $fields);
$relativePath = clean_relative_path((string)($attachments[$fileIndex]['nasRelativePath'] ?? ''));
if ($relativePath === '') {
    json_fail(404, 'Video file was not found on NAS.');
}

$filePath = resolve_video_file_path($config, $relativePath);
if ($filePath === '') {
    json_fail(404, 'Video file was not found on NAS.');
}

stream_video($filePath, mime_for_extension(strtolower(pathinfo($filePath, PATHINFO_EXTENSION))));

function stream_video(string $filePath, string $mimeType): void
{
    $size = filesize($filePath);
    if ($size === false) {
        json_fail(404, 'Video file size is unknown.');
    }

    $start = 0;
    $end = $size - 1;
    $status = 200;

    if (isset($_SERVER['HTTP_RANGE']) && preg_match('/bytes=(\d*)-(\d*)/', (string)$_SERVER['HTTP_RANGE'], $matches)) {
        $status = 206;
        if ($matches[1] !== '') {
            $start = (int)$matches[1];
        }
        if ($matches[2] !== '') {
            $end = (int)$matches[2];
        }
        if ($start > $end || $end >= $size) {
            header('Content-Range: bytes */' . $size, true, 416);
            exit;
        }
    }

    $length = $end - $start + 1;
    http_response_code($status);
    header('Content-Type: ' . $mimeType);
    header('Accept-Ranges: bytes');
    header('Content-Length: ' . $length);
    header('Content-Disposition: inline');
    if ($status === 206) {
        header("Content-Range: bytes {$start}-{$end}/{$size}");
    }

    $handle = fopen($filePath, 'rb');
    if ($handle === false) {
        exit;
    }

    fseek($handle, $start);
    $remaining = $length;
    while ($remaining > 0 && !feof($handle)) {
        $chunk = fread($handle, min(8192, $remaining));
        if ($chunk === false) {
            break;
        }
        echo $chunk;
        $remaining -= strlen($chunk);
        flush();
    }
    fclose($handle);
    exit;
}

function mime_for_extension(string $extension): string
{
    return match ($extension) {
        'mov' => 'video/quicktime',
        'avi' => 'video/x-msvideo',
        'ts' => 'video/mp2t',
        'jpg', 'jpeg' => 'image/jpeg',
        default => 'video/mp4',
    };
}

function clean_document_id(string $id): string
{
    return preg_match('/^[A-Za-z0-9._-]{1,200}$/', $id) ? $id : '';
}
