<?php

declare(strict_types=1);

require __DIR__ . '/bootstrap.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_fail(405, 'Only POST is allowed.');
}

$input = json_decode((string)file_get_contents('php://input'), true);
if (!is_array($input)) {
    json_fail(400, 'Invalid JSON body.');
}

$id = clean_document_id((string)($input['id'] ?? ''));
$status = (string)($input['status'] ?? '');
$companyComment = trim((string)($input['companyComment'] ?? ''));
if ($id === '') {
    json_fail(400, 'Submission ID is missing.');
}
$allowedStatuses = ['검토 대기 중', '검토 완료', '보완 요청', '신고 완료', '신고 결과'];
if (!in_array($status, $allowedStatuses, true)) {
    json_fail(400, 'Unknown submission status.');
}
if (in_array($status, ['보완 요청', '신고 결과'], true) && $companyComment === '') {
    json_fail(400, 'Company comment is required for this status.');
}

$config = app_config();
$updateMask = 'updateMask.fieldPaths=status&updateMask.fieldPaths=updatedAt&updateMask.fieldPaths=companyComment';
firestore_request(
    $config,
    'PATCH',
    '/submissions/' . rawurlencode($id) . '?' . $updateMask,
    [
        'fields' => [
            'status' => ['stringValue' => $status],
            'companyComment' => ['stringValue' => $companyComment],
            'updatedAt' => ['timestampValue' => gmdate('Y-m-d\TH:i:s\Z')],
        ],
    ]
);

save_json_cache('last-status-update.json', [
    'id' => $id,
    'status' => $status,
    'companyComment' => $companyComment,
    'updatedAt' => gmdate('Y-m-d\TH:i:s\Z'),
]);

json_success(['id' => $id, 'status' => $status, 'companyComment' => $companyComment]);

function clean_document_id(string $id): string
{
    return preg_match('/^[A-Za-z0-9._-]{1,160}$/', $id) ? $id : '';
}
