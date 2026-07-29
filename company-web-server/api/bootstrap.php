<?php

declare(strict_types=1);

const FIRESTORE_BASE = 'https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents';

set_exception_handler(static function (Throwable $error): void {
    json_fail(500, 'PHP error: ' . $error->getMessage());
});

function app_config(): array
{
    $path = dirname(__DIR__) . DIRECTORY_SEPARATOR . 'config.php';
    if (!is_file($path)) {
        $path = dirname(__DIR__) . DIRECTORY_SEPARATOR . 'config.example.php';
    }

    $config = require $path;
    if (!is_array($config)) {
        json_fail(500, 'config.php must return an array.');
    }

    return $config;
}

function sample_video_dir(array $config): string
{
    return rtrim((string)($config['sample_video_dir'] ?? storage_dir($config)), "/\\");
}

function allowed_review_extension(string $path): bool
{
    return in_array(strtolower(pathinfo($path, PATHINFO_EXTENSION)), ['mp4', 'mov', 'avi', 'ts', 'jpg', 'jpeg'], true);
}

function allowed_video_extension(string $path): bool
{
    return in_array(strtolower(pathinfo($path, PATHINFO_EXTENSION)), ['mp4', 'mov', 'avi', 'ts'], true);
}

function is_image_extension(string $path): bool
{
    return in_array(strtolower(pathinfo($path, PATHINFO_EXTENSION)), ['jpg', 'jpeg'], true);
}

function sample_video_path(array $config, string $fileName): string
{
    $fileName = basename(str_replace('\\', '/', $fileName));
    if ($fileName === '' || !allowed_review_extension($fileName)) {
        return '';
    }

    $directPath = sample_video_dir($config) . DIRECTORY_SEPARATOR . $fileName;
    if (is_file($directPath)) {
        return $directPath;
    }

    return find_file_by_basename(sample_video_dir($config), $fileName);
}

function find_video_path(array $config, array $fields): string
{
    $nasFiles = is_array($fields['nasFiles'] ?? null) ? $fields['nasFiles'] : [];
    $attachments = is_array($fields['attachments'] ?? null) ? $fields['attachments'] : [];
    $candidates = [
        (string)($nasFiles['front'] ?? ''),
        (string)($nasFiles['rear'] ?? ''),
        (string)($nasFiles['file1'] ?? ''),
        (string)($fields['frontVideoPath'] ?? ''),
        (string)($fields['rearVideoPath'] ?? ''),
        (string)($fields['nasRelativePath'] ?? ''),
    ];
    foreach ($attachments as $attachment) {
        if (is_array($attachment)) {
            $candidates[] = (string)($attachment['nasRelativePath'] ?? '');
            $candidates[] = (string)($attachment['displayName'] ?? '');
        }
    }
    $candidates[] = (string)($fields['originalFileName'] ?? '');

    foreach ($candidates as $candidate) {
        $path = clean_relative_path($candidate);
        if ($path !== '' && resolve_video_file_path($config, $path) !== '') {
            return $path;
        }
    }

    return '';
}

function resolve_video_file_path(array $config, string $relativePath): string
{
    $relativePath = clean_relative_path($relativePath);
    if ($relativePath === '' || !allowed_review_extension($relativePath)) {
        return '';
    }

    $samplePath = sample_video_path($config, $relativePath);
    if ($samplePath !== '' && is_file($samplePath)) {
        return $samplePath;
    }

    $storageDir = storage_dir($config);
    $storedPath = $storageDir . DIRECTORY_SEPARATOR . str_replace('/', DIRECTORY_SEPARATOR, $relativePath);
    if (is_file($storedPath)) {
        return $storedPath;
    }

    return find_file_by_basename($storageDir, basename($relativePath));
}

function storage_dir(array $config): string
{
    return rtrim((string)($config['storage_dir'] ?? '/volume1/SafeClipUpLoads'), "/\\");
}

function find_file_by_basename(string $rootDir, string $fileName): string
{
    $fileName = basename(str_replace('\\', '/', $fileName));
    if ($fileName === '' || !is_dir($rootDir)) {
        return '';
    }

    try {
        $iterator = new RecursiveIteratorIterator(
            new RecursiveDirectoryIterator($rootDir, FilesystemIterator::SKIP_DOTS),
            RecursiveIteratorIterator::SELF_FIRST
        );
        foreach ($iterator as $file) {
            if (!$file->isFile()) {
                continue;
            }
            if ($file->getFilename() === $fileName && allowed_review_extension($file->getPathname())) {
                return $file->getPathname();
            }
        }
    } catch (UnexpectedValueException) {
        return '';
    }

    return '';
}

function firestore_access_token(array $config): string
{
    if (!function_exists('openssl_sign')) {
        json_fail(500, 'PHP openssl extension is not enabled.');
    }

    $jsonPath = (string)($config['service_account_json'] ?? '');
    if ($jsonPath === '' || !is_file($jsonPath)) {
        json_fail(500, 'Firebase service account JSON file is missing.');
    }

    $service = json_decode((string)file_get_contents($jsonPath), true);
    if (!is_array($service)) {
        json_fail(500, 'Firebase service account JSON is invalid.');
    }
    if (empty($service['client_email']) || empty($service['private_key'])) {
        json_fail(500, 'Firebase service account JSON is missing client_email or private_key.');
    }

    $now = time();
    $header = base64url_encode(json_encode(['alg' => 'RS256', 'typ' => 'JWT']) ?: '');
    $claim = base64url_encode(json_encode([
        'iss' => $service['client_email'] ?? '',
        'scope' => 'https://www.googleapis.com/auth/datastore',
        'aud' => 'https://oauth2.googleapis.com/token',
        'iat' => $now,
        'exp' => $now + 3600,
    ]) ?: '');

    $unsignedJwt = $header . '.' . $claim;
    $signature = '';
    if (!openssl_sign($unsignedJwt, $signature, (string)($service['private_key'] ?? ''), OPENSSL_ALGO_SHA256)) {
        json_fail(500, 'Could not sign Firebase service account request.');
    }

    $body = http_build_query([
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $unsignedJwt . '.' . base64url_encode($signature),
    ]);

    $response = http_request('POST', 'https://oauth2.googleapis.com/token', [
        'Content-Type: application/x-www-form-urlencoded',
    ], $body);

    $payload = json_decode($response['body'], true);
    if ($response['status'] >= 400 || !is_array($payload) || empty($payload['access_token'])) {
        json_fail(500, 'Could not get Firebase access token: ' . firebase_error_message($payload, $response['body']));
    }

    return (string)$payload['access_token'];
}

function firestore_request(array $config, string $method, string $path, ?array $body = null): array
{
    $projectId = (string)($config['firebase_project_id'] ?? '');
    if ($projectId === '') {
        json_fail(500, 'firebase_project_id is missing.');
    }

    $url = sprintf(FIRESTORE_BASE, rawurlencode($projectId)) . $path;
    $headers = [
        'Authorization: Bearer ' . firestore_access_token($config),
        'Content-Type: application/json',
    ];

    $response = http_request($method, $url, $headers, $body === null ? null : json_encode($body, JSON_UNESCAPED_UNICODE));
    $payload = json_decode($response['body'], true);

    if ($response['status'] >= 400) {
        json_fail($response['status'], 'Firebase request failed: ' . firebase_error_message($payload, $response['body']));
    }

    return is_array($payload) ? $payload : [];
}

function http_request(string $method, string $url, array $headers = [], ?string $body = null): array
{
    if (!function_exists('curl_init')) {
        json_fail(500, 'PHP curl extension is not enabled.');
    }

    $curl = curl_init($url);
    curl_setopt_array($curl, [
        CURLOPT_CUSTOMREQUEST => $method,
        CURLOPT_HTTPHEADER => $headers,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 30,
    ]);

    if ($body !== null) {
        curl_setopt($curl, CURLOPT_POSTFIELDS, $body);
    }

    $responseBody = curl_exec($curl);
    $status = (int)curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
    $error = curl_error($curl);
    curl_close($curl);

    if ($responseBody === false) {
        json_fail(500, $error ?: 'HTTP request failed.');
    }

    return ['status' => $status, 'body' => (string)$responseBody];
}

function firestore_value(array $field): mixed
{
    if (array_key_exists('stringValue', $field)) {
        return $field['stringValue'];
    }
    if (array_key_exists('integerValue', $field)) {
        return (int)$field['integerValue'];
    }
    if (array_key_exists('doubleValue', $field)) {
        return (float)$field['doubleValue'];
    }
    if (array_key_exists('booleanValue', $field)) {
        return (bool)$field['booleanValue'];
    }
    if (array_key_exists('timestampValue', $field)) {
        return $field['timestampValue'];
    }
    if (isset($field['mapValue']['fields']) && is_array($field['mapValue']['fields'])) {
        $result = [];
        foreach ($field['mapValue']['fields'] as $key => $value) {
            $result[$key] = firestore_value($value);
        }
        return $result;
    }
    if (isset($field['arrayValue']['values']) && is_array($field['arrayValue']['values'])) {
        return array_map(static fn(array $value): mixed => firestore_value($value), $field['arrayValue']['values']);
    }

    return null;
}

function firebase_error_message(mixed $payload, string $rawBody): string
{
    if (is_array($payload)) {
        if (isset($payload['error']['message'])) {
            return (string)$payload['error']['message'];
        }
        if (isset($payload['error_description'])) {
            return (string)$payload['error_description'];
        }
        if (isset($payload['error'])) {
            return is_string($payload['error']) ? $payload['error'] : json_encode($payload['error'], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
        }
    }

    $body = trim($rawBody);
    return $body !== '' ? substr($body, 0, 500) : 'empty Firebase response';
}

function firestore_fields(array $document): array
{
    $fields = [];
    foreach (($document['fields'] ?? []) as $key => $value) {
        if (is_array($value)) {
            $fields[$key] = firestore_value($value);
        }
    }

    return $fields;
}

function document_id_from_name(string $name): string
{
    $parts = explode('/', $name);
    return (string)end($parts);
}

function clean_relative_path(string $path): string
{
    $path = str_replace('\\', '/', trim($path));
    $path = ltrim($path, '/');

    if ($path === '' || str_contains($path, '..')) {
        return '';
    }

    return $path;
}

function save_json_cache(string $fileName, array $data): void
{
    $dir = dirname(__DIR__) . DIRECTORY_SEPARATOR . 'data';
    if (!is_dir($dir)) {
        @mkdir($dir, 0770, true);
    }
    if (!is_dir($dir) || !is_writable($dir)) {
        return;
    }

    @file_put_contents(
        $dir . DIRECTORY_SEPARATOR . $fileName,
        json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE)
    );
}

function base64url_encode(string $value): string
{
    return rtrim(strtr(base64_encode($value), '+/', '-_'), '=');
}

function json_success(array $data): void
{
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['ok' => true] + $data, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function json_fail(int $status, string $message): void
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['ok' => false, 'error' => $message], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}
