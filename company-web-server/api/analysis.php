<?php

declare(strict_types=1);

require __DIR__ . '/bootstrap.php';
require __DIR__ . '/analysis_helpers.php';

$method = (string)($_SERVER['REQUEST_METHOD'] ?? 'GET');
if (!in_array($method, ['GET', 'POST'], true)) {
    json_fail(405, 'Only GET and POST are allowed.');
}

$input = $method === 'POST'
    ? json_decode((string)file_get_contents('php://input'), true)
    : $_GET;
if (!is_array($input)) {
    json_fail(400, 'Invalid JSON request.');
}

$id = trim((string)($input['id'] ?? ''));
$fileIndex = max(0, (int)($input['file'] ?? 0));
if ($id === '' || strlen($id) > 1000) {
    json_fail(400, 'Invalid submission id.');
}

$config = app_config();
$exchangeDir = analysis_exchange_dir($config);

if ($method === 'POST') {
    $current = analysis_read_state($exchangeDir, $id, $fileIndex);
    if (in_array((string)($current['status'] ?? ''), ['pending', 'running'], true)) {
        json_success(['analysis' => analysis_public_state($current, $id, $fileIndex)]);
    }

    $attachment = analysis_selected_attachment($config, $id, $fileIndex);
    analysis_create_job(
        $exchangeDir,
        $id,
        $fileIndex,
        (string)$attachment['nasRelativePath']
    );
}

$state = analysis_read_state($exchangeDir, $id, $fileIndex);
json_success(['analysis' => analysis_public_state($state, $id, $fileIndex)]);

function analysis_selected_attachment(array $config, string $id, int $fileIndex): array
{
    if (str_starts_with($id, 'sample-')) {
        $relativePath = clean_relative_path(rawurldecode(substr($id, strlen('sample-'))));
        $filePath = resolve_video_file_path($config, $relativePath);
        if ($fileIndex !== 0 || $relativePath === '' || !allowed_video_extension($relativePath) || $filePath === '') {
            json_fail(404, 'Selected sample video was not found.');
        }
        return ['nasRelativePath' => $relativePath];
    }

    if (company_web_upstream_url($config) !== '') {
        $payload = upstream_company_payload($config, 'api/submissions.php');
        foreach ((array)($payload['submissions'] ?? []) as $submission) {
            if (!is_array($submission) || (string)($submission['id'] ?? '') !== $id) {
                continue;
            }
            $attachment = (array)(($submission['attachments'] ?? [])[$fileIndex] ?? []);
            if (($attachment['kind'] ?? '') !== 'video'
                || ($attachment['exists'] ?? false) !== true
                || !allowed_video_extension((string)($attachment['nasRelativePath'] ?? ''))) {
                break;
            }
            return $attachment;
        }
        json_fail(404, 'Selected upstream NAS video was not found.');
    }

    if (!preg_match('/^[A-Za-z0-9._-]{1,200}$/', $id)) {
        json_fail(400, 'Invalid submission id.');
    }
    $document = firestore_request($config, 'GET', '/submissions/' . rawurlencode($id));
    $attachments = submission_attachments($config, firestore_fields($document));
    $attachment = $attachments[$fileIndex] ?? null;
    if (!is_array($attachment)
        || ($attachment['kind'] ?? '') !== 'video'
        || ($attachment['exists'] ?? false) !== true
        || !allowed_video_extension((string)($attachment['nasRelativePath'] ?? ''))) {
        json_fail(404, 'Selected video was not found.');
    }
    return $attachment;
}

function analysis_public_state(array $state, string $id, int $fileIndex): array
{
    if (($state['status'] ?? '') !== 'completed' || !is_array($state['result'] ?? null)) {
        return $state;
    }

    $state['evidenceUrls'] = array_map(
        static fn(string $name): string => 'api/analysis-evidence.php?' . http_build_query([
            'id' => $id,
            'file' => $fileIndex,
            'name' => $name,
        ]),
        array_slice(array_map('strval', (array)($state['result']['evidenceImages'] ?? [])), 0, 3)
    );
    return $state;
}
