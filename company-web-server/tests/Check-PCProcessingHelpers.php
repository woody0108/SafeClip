<?php

declare(strict_types=1);

require dirname(__DIR__) . '/api/bootstrap.php';

$helperPath = dirname(__DIR__) . '/api/pc_processing_helpers.php';
if (is_file($helperPath)) {
    require $helperPath;
}

$config = [
    'pc_processing_token' => 'test-secret',
    'pc_allowed_nas_ip' => '192.168.0.3',
];

assert_true(
    pc_processing_authorized($config, '192.168.0.3', 'test-secret'),
    'Configured NAS address and token must be accepted.'
);
assert_true(
    pc_processing_authorized(
        ['pc_processing_token' => 'test-secret', 'pc_allowed_nas_ip' => ' 192.168.0.3 '],
        '192.168.0.3',
        'test-secret'
    ),
    'Configured NAS address whitespace must be ignored.'
);
assert_true(
    !pc_processing_authorized($config, '192.168.0.4', 'test-secret'),
    'A caller outside the configured NAS address must be rejected.'
);
assert_true(
    !pc_processing_authorized($config, '192.168.0.3', 'wrong'),
    'A mismatched worker token must be rejected.'
);
assert_true(
    !pc_processing_authorized($config, '192.168.0.3', ''),
    'A missing worker token must be rejected.'
);

$request = pc_preview_request([
    'id' => 'submission-123',
    'file' => '2',
    'path' => 'C:\\sensitive\\video.mp4',
]);
assert_same(
    ['id' => 'submission-123', 'fileIndex' => 2],
    $request,
    'Preview requests must use only the submission ID and attachment index.'
);
assert_same(
    [],
    pc_preview_request(['id' => 'C:\\sensitive\\video.mp4', 'file' => '0']),
    'A filesystem path must not be accepted as a submission ID.'
);

echo "PC processing helper checks passed.\n";

function assert_true(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

function assert_same(mixed $expected, mixed $actual, string $message): void
{
    if ($expected !== $actual) {
        throw new RuntimeException($message);
    }
}
