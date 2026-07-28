<?php

declare(strict_types=1);

require __DIR__ . '/bootstrap.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    json_fail(405, 'Only GET is allowed.');
}

$config = app_config();
$limit = max(1, min((int)($config['max_submissions'] ?? 50), 100));

if ((string)($_GET['mode'] ?? '') === 'sample') {
    $submissions = sample_submissions_from_folder($config, $limit);
    $response = [
        'mode' => 'sample_folder',
        'sampleFolder' => sample_folder_diagnostics($config, count($submissions)),
        'submissions' => $submissions,
    ];
    save_json_cache('submissions-cache.json', $response);
    json_success($response);
}

if (!is_file((string)($config['service_account_json'] ?? ''))) {
    json_fail(500, 'Firebase service account JSON file is missing.');
}

$payload = firestore_request($config, 'POST', ':runQuery', [
    'structuredQuery' => [
        'from' => [['collectionId' => 'submissions']],
        'orderBy' => [[
            'field' => ['fieldPath' => 'createdAt'],
            'direction' => 'DESCENDING',
        ]],
        'limit' => $limit,
    ],
]);

$submissions = [];
foreach ($payload as $row) {
    if (!isset($row['document']) || !is_array($row['document'])) {
        continue;
    }

    $document = $row['document'];
    $fields = firestore_fields($document);
    $id = document_id_from_name((string)($document['name'] ?? ''));
    $videoPath = find_video_path($config, $fields);

    $submissions[] = [
        'id' => $id,
        'status' => $fields['status'] ?? 'waiting_review',
        'ownerUid' => $fields['ownerUid'] ?? '',
        'guestId' => $fields['guestId'] ?? '',
        'ownerDisplayName' => $fields['ownerDisplayName'] ?? '',
        'ownerEmail' => $fields['ownerEmail'] ?? '',
        'submitterLabel' => submitter_label($fields),
        'originalFileName' => $fields['originalFileName'] ?? '',
        'fileSizeBytes' => $fields['fileSizeBytes'] ?? null,
        'incidentDateTime' => $fields['incidentDateTime'] ?? '',
        'incidentLocationText' => $fields['incidentLocationText'] ?? '',
        'violationTypeCandidate' => $fields['violationTypeCandidate'] ?? '',
        'userMemo' => $fields['userMemo'] ?? '',
        'createdAtText' => format_firestore_time((string)($fields['createdAt'] ?? '')),
        'updatedAtText' => format_firestore_time((string)($fields['updatedAt'] ?? '')),
        'videoPath' => $videoPath,
        'videoExists' => $videoPath !== '',
        'raw' => $fields,
    ];
}

$response = ['mode' => 'firestore', 'submissions' => $submissions];
save_json_cache('submissions-cache.json', $response);
json_success($response);

function submitter_label(array $fields): string
{
    foreach (['ownerDisplayName', 'ownerEmail', 'guestId', 'ownerUid'] as $field) {
        $value = trim((string)($fields[$field] ?? ''));
        if ($value !== '') {
            return $value;
        }
    }

    return '제출자 없음';
}

function sample_submissions_from_folder(array $config, int $limit): array
{
    $dir = sample_video_dir($config);
    if (!is_dir($dir)) {
        return [];
    }

    $files = [];
    foreach (scandir($dir) ?: [] as $name) {
        $path = $dir . DIRECTORY_SEPARATOR . $name;
        if ($name === '.' || $name === '..' || !is_file($path) || !allowed_video_extension($path)) {
            continue;
        }
        $files[] = ['name' => $name, 'mtime' => filemtime($path) ?: 0];
    }

    usort($files, fn(array $a, array $b): int => $b['mtime'] <=> $a['mtime']);
    $files = array_slice($files, 0, $limit);

    return array_map(static function (array $file): array {
        return [
            'id' => 'sample-' . rawurlencode($file['name']),
            'status' => 'waiting_review',
            'ownerUid' => '',
            'guestId' => 'NAS-SAMPLE',
            'ownerDisplayName' => 'NAS 샘플 폴더',
            'ownerEmail' => '',
            'submitterLabel' => 'NAS 샘플 폴더',
            'originalFileName' => $file['name'],
            'incidentLocationText' => '\\\\SyDisk\\Videos',
            'violationTypeCandidate' => '샘플 영상',
            'userMemo' => 'NAS Videos 폴더에서 읽은 영상입니다.',
            'createdAtText' => date('Y-m-d H:i', $file['mtime']),
            'videoPath' => $file['name'],
            'videoExists' => true,
            'sample' => true,
        ];
    }, $files);
}

function sample_folder_diagnostics(array $config, int $videoCount): array
{
    $dir = sample_video_dir($config);

    return [
        'path' => $dir,
        'exists' => is_dir($dir),
        'readable' => is_readable($dir),
        'videoCount' => $videoCount,
    ];
}

function format_firestore_time(string $value): string
{
    if ($value === '') {
        return '';
    }

    $timestamp = strtotime($value);
    if ($timestamp === false) {
        return $value;
    }

    return date('Y-m-d H:i', $timestamp);
}
