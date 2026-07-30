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

    ensure_php_upload_limit_allows_request();

    $expectedKey = (string)($config['upload_key'] ?? '');
    $providedKey = upload_key_from_request();
    if ($expectedKey === '' || !hash_equals($expectedKey, $providedKey)) {
        fail(401, 'Invalid upload key.');
    }

    $file = upload_file_from_request();
    if ($file === null) {
        fail(400, 'Missing upload file field.');
    }

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
        fail(415, 'Unsupported file extension.');
    }

    $storageDir = rtrim((string)($config['storage_dir'] ?? ''), "/\\");
    if ($storageDir === '') {
        fail(500, 'Server storage_dir is missing.');
    }

    $submissionId = sanitize_name((string)($_POST['submission_id'] ?? 'upload'));
    $submitterLabel = sanitize_path_segment((string)($_POST['submitter_label'] ?? 'guest'));
    $datePath = date('Y/m/d');
    $submissionFolder = find_or_allocate_submission_folder(
        $storageDir,
        $datePath,
        $submitterLabel,
        $submissionId
    );
    $targetDir = $storageDir . DIRECTORY_SEPARATOR . str_replace('/', DIRECTORY_SEPARATOR, $submissionFolder['relativePath']);
    ensure_directory($targetDir);

    // 파일명은 URL/경로 조작을 막기 위해 안전한 문자만 남깁니다.
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
        'relative_path' => $submissionFolder['relativePath'] . '/' . $storedName,
        'size_bytes' => $sizeBytes,
        'submission_folder' => $submissionFolder['relativePath'],
        'submission_sequence' => $submissionFolder['sequence'],
        'submission_sequence_text' => $submissionFolder['sequenceText'],
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

function upload_file_from_request(): ?array
{
    if (isset($_FILES['file'])) {
        return $_FILES['file'];
    }
    if (isset($_FILES['video'])) {
        return $_FILES['video'];
    }
    return null;
}

function ensure_php_upload_limit_allows_request(): void
{
    $contentLength = (int)($_SERVER['CONTENT_LENGTH'] ?? 0);
    if ($contentLength <= 0) {
        return;
    }

    $postMaxBytes = ini_bytes((string)ini_get('post_max_size'));
    if ($postMaxBytes > 0 && $contentLength > $postMaxBytes) {
        fail(
            413,
            'Uploaded request exceeds PHP post_max_size. ' .
                'content_length=' . $contentLength .
                ', post_max_size=' . (string)ini_get('post_max_size') .
                ', upload_max_filesize=' . (string)ini_get('upload_max_filesize')
        );
    }
}

function ini_bytes(string $value): int
{
    $value = trim($value);
    if ($value === '') {
        return 0;
    }

    $unit = strtolower(substr($value, -1));
    $number = (float)$value;
    return match ($unit) {
        'g' => (int)($number * 1024 * 1024 * 1024),
        'm' => (int)($number * 1024 * 1024),
        'k' => (int)($number * 1024),
        default => (int)$number,
    };
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

function sanitize_path_segment(string $value): string
{
    $value = trim($value);
    $value = preg_replace('/[^\p{L}\p{N}._-]+/u', '-', $value) ?? '';
    $value = trim($value, '.-_');

    return $value === '' ? 'guest' : $value;
}

function find_or_allocate_submission_folder(
    string $storageDir,
    string $datePath,
    string $submitterLabel,
    string $submissionId
): array {
    $submitterRelative = $datePath . '/' . $submitterLabel;
    $submitterDir = $storageDir . DIRECTORY_SEPARATOR . str_replace('/', DIRECTORY_SEPARATOR, $submitterRelative);
    ensure_directory($submitterDir);

    $existing = find_existing_submission_sequence($submitterDir, $submissionId);
    if ($existing !== null) {
        return sequence_folder($submitterRelative, $existing);
    }

    $sequence = next_submission_sequence($submitterDir);
    return sequence_folder($submitterRelative, $sequence);
}

function find_existing_submission_sequence(string $submitterDir, string $submissionId): ?int
{
    foreach (sequence_directories($submitterDir) as $sequenceText) {
        $sequenceDir = $submitterDir . DIRECTORY_SEPARATOR . $sequenceText;
        foreach ((array)scandir($sequenceDir) as $fileName) {
            if ($fileName === '.' || $fileName === '..') {
                continue;
            }
            if (str_contains($fileName, $submissionId)) {
                return (int)$sequenceText;
            }
        }
    }

    return null;
}

function next_submission_sequence(string $submitterDir): int
{
    $max = 0;
    foreach (sequence_directories($submitterDir) as $sequenceText) {
        $max = max($max, (int)$sequenceText);
    }

    return $max + 1;
}

function sequence_directories(string $submitterDir): array
{
    $sequences = [];
    foreach ((array)scandir($submitterDir) as $name) {
        if (preg_match('/^\d{2,}$/', $name) && is_dir($submitterDir . DIRECTORY_SEPARATOR . $name)) {
            $sequences[] = $name;
        }
    }
    sort($sequences, SORT_STRING);

    return $sequences;
}

function sequence_folder(string $submitterRelative, int $sequence): array
{
    $sequenceText = sprintf('%02d', $sequence);
    return [
        'relativePath' => $submitterRelative . '/' . $sequenceText,
        'sequence' => $sequence,
        'sequenceText' => $sequenceText,
    ];
}

function ensure_directory(string $path): void
{
    if (is_dir($path)) {
        return;
    }

    if (!mkdir($path, 0770, true) && !is_dir($path)) {
        fail(500, 'Could not create NAS upload date folder: ' . $path);
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
