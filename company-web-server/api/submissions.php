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
            'field' => ['fieldPath' => 'submittedAt'],
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
    $attachments = submission_attachments($config, $fields);
    $firstAttachment = $attachments[0] ?? null;

    $submissions[] = [
        'id' => $id,
        'status' => normalize_status((string)($fields['status'] ?? '')),
        'ownerUid' => $fields['ownerUid'] ?? '',
        'guestId' => $fields['guestId'] ?? '',
        'ownerDisplayName' => $fields['ownerDisplayName'] ?? '',
        'submitterLabel' => submitter_label($fields),
        'incidentDate' => $fields['incidentDate'] ?? '',
        'incidentTime' => $fields['incidentTime'] ?? '',
        'incidentLocation' => $fields['incidentLocation'] ?? '',
        'reportType' => $fields['reportType'] ?? '',
        'reportMemo' => $fields['reportMemo'] ?? '',
        'companyComment' => $fields['companyComment'] ?? '',
        'submittedAtText' => format_firestore_time((string)($fields['submittedAt'] ?? '')),
        'updatedAtText' => format_firestore_time((string)($fields['updatedAt'] ?? '')),
        'submissionSequence' => $fields['submissionSequence'] ?? null,
        'submissionSequenceText' => $fields['submissionSequenceText'] ?? '',
        'nasSubmissionFolder' => $fields['nasSubmissionFolder'] ?? '',
        'attachments' => $attachments,
        'videoPath' => (string)($firstAttachment['nasRelativePath'] ?? ''),
        'videoExists' => (bool)($firstAttachment['exists'] ?? false),
        'raw' => $fields,
    ];
}

$response = ['mode' => 'firestore', 'submissions' => $submissions];
save_json_cache('submissions-cache.json', $response);
json_success($response);

function submitter_label(array $fields): string
{
    foreach (['ownerDisplayName', 'guestId', 'ownerUid'] as $field) {
        $value = trim((string)($fields[$field] ?? ''));
        if ($value !== '') {
            return $value;
        }
    }

    return '제출자 없음';
}

function normalize_status(string $status): string
{
    return in_array($status, ['검토 대기 중', '검토 완료', '보완 요청', '신고 완료', '신고 결과'], true)
        ? $status
        : '검토 대기 중';
}

function sample_submissions_from_folder(array $config, int $limit): array
{
    $dir = sample_video_dir($config);
    if (!is_dir($dir)) {
        return [];
    }

    $files = [];
    try {
        $iterator = new RecursiveIteratorIterator(
            new RecursiveDirectoryIterator($dir, FilesystemIterator::SKIP_DOTS),
            RecursiveIteratorIterator::SELF_FIRST
        );
        foreach ($iterator as $file) {
            if (!$file->isFile() || !allowed_review_extension($file->getPathname())) {
                continue;
            }
            $relativePath = ltrim(str_replace('\\', '/', substr($file->getPathname(), strlen($dir))), '/');
            $files[] = [
                'name' => $file->getFilename(),
                'path' => $relativePath,
                'mtime' => $file->getMTime() ?: 0,
            ];
        }
    } catch (UnexpectedValueException) {
        return [];
    }

    usort($files, fn(array $a, array $b): int => $b['mtime'] <=> $a['mtime']);
    $files = array_slice($files, 0, $limit);

    return array_map(static function (array $file): array {
        return [
            'id' => 'sample-' . rawurlencode($file['path']),
            'status' => '검토 대기 중',
            'ownerUid' => '',
            'guestId' => 'NAS-SAMPLE',
            'ownerDisplayName' => 'NAS 샘플 폴더',
            'submitterLabel' => 'NAS 샘플 폴더',
            'incidentLocation' => '\\\\SyDisk\\SafeClipUpLoads',
            'reportType' => '샘플 영상',
            'reportMemo' => 'NAS SafeClipUpLoads 폴더에서 읽은 파일입니다.',
            'companyComment' => '',
            'submittedAtText' => date('Y-m-d H:i', $file['mtime']),
            'attachments' => [[
                'index' => 0,
                'displayName' => $file['name'],
                'kind' => is_image_extension($file['path']) ? 'photo' : 'video',
                'mimeType' => is_image_extension($file['path']) ? 'image/jpeg' : 'video/mp4',
                'sizeBytes' => null,
                'uploadedSizeBytes' => null,
                'nasRelativePath' => $file['path'],
                'exists' => true,
            ]],
            'videoPath' => $file['path'],
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
