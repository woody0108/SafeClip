<?php

declare(strict_types=1);

require __DIR__ . '/bootstrap.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    json_fail(405, 'Only GET is allowed.');
}

$config = app_config();
$serviceAccountPath = (string)($config['service_account_json'] ?? '');
$serviceAccountExists = $serviceAccountPath !== '' && is_file($serviceAccountPath);
$serviceAccountReadable = $serviceAccountExists && is_readable($serviceAccountPath);
$serviceAccountLooksValid = false;

if ($serviceAccountReadable) {
    $serviceAccount = json_decode((string)file_get_contents($serviceAccountPath), true);
    $serviceAccountLooksValid = is_array($serviceAccount)
        && !empty($serviceAccount['client_email'])
        && !empty($serviceAccount['private_key']);
}

$sampleDir = sample_video_dir($config);

json_success([
    'phpVersion' => PHP_VERSION,
    'curlLoaded' => function_exists('curl_init'),
    'opensslLoaded' => function_exists('openssl_sign'),
    'serviceAccountPath' => $serviceAccountPath,
    'serviceAccountExists' => $serviceAccountExists,
    'serviceAccountReadable' => $serviceAccountReadable,
    'serviceAccountLooksValid' => $serviceAccountLooksValid,
    'firebaseProjectId' => (string)($config['firebase_project_id'] ?? ''),
    'sampleVideoDir' => $sampleDir,
    'sampleVideoDirExists' => is_dir($sampleDir),
    'sampleVideoDirReadable' => is_readable($sampleDir),
]);
