<?php

declare(strict_types=1);

require __DIR__ . '/api/bootstrap.php';
require __DIR__ . '/api/pc_processing_helpers.php';
require __DIR__ . '/api/pc-preview.php';

$path = parse_url((string)($_SERVER['REQUEST_URI'] ?? '/'), PHP_URL_PATH);
$path = is_string($path) ? $path : '/';
if (!in_array($path, ['/health', '/preview'], true)) {
    json_fail(404, 'Not found.');
}
if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') {
    json_fail(405, 'Only GET is allowed.');
}

$config = app_config();
$remoteAddress = (string)($_SERVER['REMOTE_ADDR'] ?? '');
$providedToken = (string)($_SERVER['HTTP_X_SAFECLIP_WORKER_TOKEN'] ?? '');
if (!pc_processing_authorized($config, $remoteAddress, $providedToken)) {
    json_fail(403, 'PC processing request is not authorized.');
}

if ($path === '/health') {
    json_success(['service' => 'pc-processing']);
}

pc_preview_handle_request($config, $_GET);
