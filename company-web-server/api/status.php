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
if ($id === '') {
    json_fail(400, 'Submission ID is missing.');
}
if ($status !== 'completed') {
    json_fail(400, 'Only completed status is allowed.');
}

$config = app_config();
firestore_request(
    $config,
    'PATCH',
    '/submissions/' . rawurlencode($id) . '?updateMask.fieldPaths=status&updateMask.fieldPaths=updatedAt',
    [
        'fields' => [
            'status' => ['stringValue' => 'completed'],
            'updatedAt' => ['timestampValue' => gmdate('Y-m-d\TH:i:s\Z')],
        ],
    ]
);

save_json_cache('last-status-update.json', [
    'id' => $id,
    'status' => 'completed',
    'updatedAt' => gmdate('Y-m-d\TH:i:s\Z'),
]);

json_success(['id' => $id, 'status' => 'completed']);

function clean_document_id(string $id): string
{
    return preg_match('/^[A-Za-z0-9._-]{1,160}$/', $id) ? $id : '';
}
