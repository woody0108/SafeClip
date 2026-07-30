<?php

declare(strict_types=1);

require __DIR__ . '/bootstrap.php';

$config = app_config();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $limit = max(1, min((int)($_GET['limit'] ?? 50), 100));
    $query = [
        'structuredQuery' => [
            'from' => [['collectionId' => 'ask']],
            'limit' => $limit,
        ],
    ];
    $id = trim((string)($_GET['id'] ?? ''));
    if ($id !== '') {
        $query['structuredQuery']['where'] = [
            'fieldFilter' => [
                'field' => ['fieldPath' => 'id'],
                'op' => 'EQUAL',
                'value' => ['stringValue' => $id],
            ],
        ];
    } else {
        $query['structuredQuery']['orderBy'] = [[
            'field' => ['fieldPath' => 'questionAt'],
            'direction' => 'DESCENDING',
        ]];
    }
    $payload = firestore_request($config, 'POST', ':runQuery', $query);

    $asks = [];
    foreach ($payload as $row) {
        if (!isset($row['document']) || !is_array($row['document'])) {
            continue;
        }
        $document = $row['document'];
        $fields = firestore_fields($document);
        $asks[] = [
            'documentId' => document_id_from_name((string)($document['name'] ?? '')),
            'id' => (string)($fields['id'] ?? ''),
            'questionType' => (string)($fields['questionType'] ?? '일반 문의'),
            'questionAt' => (string)($fields['questionAt'] ?? ''),
            'questionAtText' => format_ask_time((string)($fields['questionAt'] ?? '')),
            'question' => (string)($fields['question'] ?? ''),
            'answer' => (string)($fields['answer'] ?? ''),
        ];
    }
    usort($asks, static function (array $left, array $right): int {
        return strcmp((string)($right['questionAt'] ?? ''), (string)($left['questionAt'] ?? ''));
    });
    foreach ($asks as &$ask) {
        unset($ask['questionAt']);
    }
    unset($ask);

    json_success(['asks' => $asks]);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode((string)file_get_contents('php://input'), true);
    if (!is_array($input)) {
        json_fail(400, 'Invalid JSON body.');
    }

    $action = trim((string)($input['action'] ?? ''));
    if ($action === 'delete') {
        $documentId = clean_ask_document_id((string)($input['documentId'] ?? ''));
        if ($documentId === '') {
            json_fail(400, 'Ask document ID is missing.');
        }

        firestore_request(
            $config,
            'DELETE',
            '/ask/' . rawurlencode($documentId)
        );

        json_success([
            'documentId' => $documentId,
        ]);
    }

    if ($action === 'create') {
        $id = trim((string)($input['id'] ?? ''));
        $questionType = trim((string)($input['questionType'] ?? '기타 문의'));
        $question = trim((string)($input['question'] ?? ''));
        if ($id === '') {
            json_fail(400, 'ID is missing.');
        }
        if ($question === '') {
            json_fail(400, 'Question is missing.');
        }

        $created = firestore_request(
            $config,
            'POST',
            '/ask',
            [
                'fields' => [
                    'id' => ['stringValue' => limited_ask_text($id, 120)],
                    'questionType' => ['stringValue' => limited_ask_text($questionType, 80)],
                    'questionAt' => ['timestampValue' => gmdate('Y-m-d\TH:i:s\Z')],
                    'question' => ['stringValue' => limited_ask_text($question, 2000)],
                    'answer' => ['stringValue' => ''],
                ],
            ]
        );

        json_success([
            'documentId' => document_id_from_name((string)($created['name'] ?? '')),
        ]);
    }

    $documentId = clean_ask_document_id((string)($input['documentId'] ?? ''));
    $answer = trim((string)($input['answer'] ?? ''));
    if ($documentId === '') {
        json_fail(400, 'Ask document ID is missing.');
    }

    firestore_request(
        $config,
        'PATCH',
        '/ask/' . rawurlencode($documentId) . '?updateMask.fieldPaths=answer',
        [
            'fields' => [
                'answer' => ['stringValue' => $answer],
            ],
        ]
    );

    json_success([
        'documentId' => $documentId,
        'answer' => $answer,
    ]);
}

json_fail(405, 'Only GET and POST are allowed.');

function clean_ask_document_id(string $id): string
{
    return preg_match('/^[A-Za-z0-9._-]{1,160}$/', $id) ? $id : '';
}

function limited_ask_text(string $value, int $maxLength): string
{
    if (function_exists('mb_substr')) {
        return mb_substr($value, 0, $maxLength);
    }

    return substr($value, 0, $maxLength);
}

function format_ask_time(string $value): string
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
