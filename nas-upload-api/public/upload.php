<?php

declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');

try {
    $configPath = __DIR__ . '/../config.php';
    if (!is_file($configPath)) {
        fail(500, 'Server config.php is missing.');
    }

    $config = require $configPath;
    if (!is_array($config)) {
        fail(500, 'Server config.php must return an array.');
    }

    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        fail(405, 'Only POST uploads are allowed.');
    }

    $expectedKey = (string)($config['upload_key'] ?? '');
    $providedKey = upload_key_from_request();
    if ($expectedKey === '' || !hash_equals($expectedKey, $providedKey)) {
        fail(401, 'Invalid upload key.');
    }

    if (!isset($_FILES['video'])) {
        fail(400, 'Missing video file field.');
    }

    $file = $_FILES['video'];
    if (!is_array($file) || ($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
        fail(400, upload_error_message((int)($file['error'] ?? UPLOAD_ERR_NO_FILE)));
    }

    $sizeBytes = (int)($file['size'] ?? 0);
    $maxFileBytes = (int)($config['max_file_bytes'] ?? 0);
    if ($sizeBytes <= 0) {
        fail(400, 'Uploaded file is empty.');
    }
    if ($maxFileBytes > 0 && $sizeBytes > $maxFileBytes) {
        fail(413, 'Uploaded file is too large.');
    }

    $originalName = (string)($_POST['original_file_name'] ?? $file['name'] ?? 'video');
    $extension = strtolower(pathinfo($originalName, PATHINFO_EXTENSION));
    $allowedExtensions = array_map('strtolower', (array)($config['allowed_extensions'] ?? []));
    if (!in_array($extension, $allowedExtensions, true)) {
        fail(415, 'Unsupported video extension.');
    }

    $storageDir = rtrim((string)($config['storage_dir'] ?? ''), "/\\");
    if ($storageDir === '') {
        fail(500, 'Server storage_dir is missing.');
    }

    $datePath = date('Y/m/d');
    $targetDir = $storageDir . DIRECTORY_SEPARATOR . str_replace('/', DIRECTORY_SEPARATOR, $datePath);
    ensure_directory($targetDir);

    // 파일명은 URL/경로 조작을 막기 위해 안전한 문자만 남깁니다.
    $submissionId = sanitize_name((string)($_POST['submission_id'] ?? 'upload'));
    $baseName = sanitize_name(pathinfo($originalName, PATHINFO_FILENAME));
    $stamp = date('Ymd_His');
    $random = bin2hex(random_bytes(4));
    $storedName = "{$stamp}_{$submissionId}_{$baseName}_{$random}.{$extension}";
    $targetPath = $targetDir . DIRECTORY_SEPARATOR . $storedName;

    if (!is_uploaded_file((string)$file['tmp_name'])) {
        fail(400, 'Upload did not arrive as an HTTP uploaded file.');
    }

    if (!move_uploaded_file((string)$file['tmp_name'], $targetPath)) {
        fail(500, 'Could not store uploaded file. Check NAS folder permissions.');
    }

    success([
        'stored_name' => $storedName,
        'relative_path' => $datePath . '/' . $storedName,
        'size_bytes' => $sizeBytes,
    ]);
} catch (Throwable $error) {
    fail(500, 'Unexpected upload server error.');
}

function upload_key_from_request(): string
{
    // HTTP header: X-SafeClip-Upload-Key
    if (isset($_SERVER['HTTP_X_SAFECLIP_UPLOAD_KEY'])) {
        return (string)$_SERVER['HTTP_X_SAFECLIP_UPLOAD_KEY'];
    }

    return (string)($_POST['upload_key'] ?? '');
}

function sanitize_name(string $value): string
{
    $value = trim($value);
    $value = preg_replace('/[^A-Za-z0-9._-]+/', '-', $value) ?? '';
    $value = trim($value, '.-_');

    if ($value === '') {
        return 'file';
    }

    return substr($value, 0, 80);
}

function ensure_directory(string $path): void
{
    if (is_dir($path)) {
        return;
    }

    if (!mkdir($path, 0770, true) && !is_dir($path)) {
        fail(500, 'Could not create NAS upload date folder.');
    }
}

function upload_error_message(int $code): string
{
    return match ($code) {
        UPLOAD_ERR_INI_SIZE, UPLOAD_ERR_FORM_SIZE => 'Uploaded file exceeds the PHP upload limit.',
        UPLOAD_ERR_PARTIAL => 'Uploaded file arrived only partially.',
        UPLOAD_ERR_NO_FILE => 'No video file was uploaded.',
        UPLOAD_ERR_NO_TMP_DIR => 'PHP temporary upload folder is missing.',
        UPLOAD_ERR_CANT_WRITE => 'PHP could not write the temporary upload file.',
        UPLOAD_ERR_EXTENSION => 'A PHP extension stopped the upload.',
        default => 'Upload failed.',
    };
}

function success(array $data): void
{
    http_response_code(200);
    echo json_encode(['ok' => true] + $data, JSON_UNESCAPED_SLASHES);
    exit;
}

function fail(int $statusCode, string $message): void
{
    http_response_code($statusCode);
    echo json_encode(['ok' => false, 'error' => $message], JSON_UNESCAPED_SLASHES);
    exit;
}
