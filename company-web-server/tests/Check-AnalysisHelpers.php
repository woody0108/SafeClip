<?php

declare(strict_types=1);

require dirname(__DIR__) . '/api/analysis_helpers.php';

$root = sys_get_temp_dir() . DIRECTORY_SEPARATOR . 'safeclip-analysis-' . bin2hex(random_bytes(4));
mkdir($root, 0770, true);

try {
    assert_true(
        str_starts_with(analysis_path('/volume1/SafeClipAI', 'jobs'), '/volume1/SafeClipAI'),
        'NAS absolute path must keep its leading slash.'
    );
    $requestKey = analysis_request_key('sample-driving-video', 0);
    assert_true((bool)preg_match('/^[a-f0-9]{64}$/', $requestKey), 'Request key must be a SHA-256 value.');

    $created = analysis_create_job($root, 'sample-driving-video', 0, 'Driving/sample.mp4');
    assert_true(is_file($created['jobPath']), 'Pending job file was not created.');

    $job = json_decode((string)file_get_contents($created['jobPath']), true);
    assert_true(($job['status'] ?? '') === 'pending', 'New job must be pending.');
    assert_true(($job['nasRelativeVideoPath'] ?? '') === 'Driving/sample.mp4', 'Job must keep the verified relative path.');

    $state = analysis_read_state($root, 'sample-driving-video', 0);
    assert_true(($state['status'] ?? '') === 'pending', 'Pending job state was not found.');

    $submissionId = (string)$created['submissionId'];
    $resultDir = $root . DIRECTORY_SEPARATOR . 'results' . DIRECTORY_SEPARATOR . $submissionId;
    mkdir($resultDir . DIRECTORY_SEPARATOR . 'evidence', 0770, true);
    file_put_contents($resultDir . DIRECTORY_SEPARATOR . 'evidence' . DIRECTORY_SEPARATOR . 'plate-best.jpg', 'jpg');
    file_put_contents($resultDir . DIRECTORY_SEPARATOR . 'analysis.json', json_encode([
        'status' => 'completed',
        'jobId' => $created['jobId'],
        'evidenceImages' => ['evidence/plate-best.jpg'],
    ]));

    $completed = analysis_read_state($root, 'sample-driving-video', 0);
    assert_true(($completed['status'] ?? '') === 'completed', 'Completed result must take precedence over queue state.');
    assert_true(
        analysis_resolve_evidence($root, 'sample-driving-video', 0, 'evidence/plate-best.jpg') !== '',
        'Registered evidence image was not resolved.'
    );
    assert_true(
        analysis_resolve_evidence($root, 'sample-driving-video', 0, '../secret.jpg') === '',
        'Evidence traversal must be rejected.'
    );

    echo "Analysis helper checks passed.\n";
} finally {
    remove_tree($root);
}

function assert_true(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

function remove_tree(string $path): void
{
    if (!is_dir($path)) {
        return;
    }
    foreach (new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator($path, FilesystemIterator::SKIP_DOTS),
        RecursiveIteratorIterator::CHILD_FIRST
    ) as $item) {
        $item->isDir() ? rmdir($item->getPathname()) : unlink($item->getPathname());
    }
    rmdir($path);
}
