<?php

declare(strict_types=1);

require __DIR__ . '/bootstrap.php';
require __DIR__ . '/analysis_helpers.php';

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') {
    json_fail(405, 'Only GET is allowed.');
}

$id = trim((string)($_GET['id'] ?? ''));
$fileIndex = max(0, (int)($_GET['file'] ?? 0));
$name = (string)($_GET['name'] ?? '');
if ($id === '' || $name === '') {
    json_fail(400, 'Invalid evidence request.');
}

$path = analysis_resolve_evidence(analysis_exchange_dir(app_config()), $id, $fileIndex, $name);
if ($path === '') {
    json_fail(404, 'Evidence image was not found.');
}

$extension = strtolower(pathinfo($path, PATHINFO_EXTENSION));
header('Content-Type: ' . ($extension === 'png' ? 'image/png' : 'image/jpeg'));
header('Content-Length: ' . (string)filesize($path));
header('Cache-Control: private, max-age=300');
readfile($path);
