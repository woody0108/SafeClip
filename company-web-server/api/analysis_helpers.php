<?php

declare(strict_types=1);

function analysis_exchange_dir(array $config): string
{
    return rtrim((string)($config['ai_exchange_dir'] ?? '/volume1/SafeClipAI'), "/\\");
}

function analysis_request_key(string $submissionId, int $fileIndex): string
{
    return hash('sha256', $submissionId . ':' . max(0, $fileIndex));
}

function analysis_submission_id(string $submissionId, int $fileIndex): string
{
    return 'sc-' . substr(analysis_request_key($submissionId, $fileIndex), 0, 32);
}

function analysis_create_job(
    string $exchangeDir,
    string $originalSubmissionId,
    int $fileIndex,
    string $relativeVideoPath
): array {
    $relativeVideoPath = analysis_safe_relative_path($relativeVideoPath);
    if ($relativeVideoPath === '') {
        throw new InvalidArgumentException('Invalid video path.');
    }

    $requestKey = analysis_request_key($originalSubmissionId, $fileIndex);
    $submissionId = analysis_submission_id($originalSubmissionId, $fileIndex);
    $jobId = 'job-' . gmdate('YmdHis') . '-' . bin2hex(random_bytes(5));
    $job = [
        'schemaVersion' => 1,
        'jobId' => $jobId,
        'submissionId' => $submissionId,
        'attachmentIndex' => max(0, $fileIndex),
        'nasRelativeVideoPath' => $relativeVideoPath,
        'status' => 'pending',
        'requestedAt' => gmdate('c'),
    ];

    $jobPath = analysis_path($exchangeDir, 'jobs', 'pending', $jobId . '.json');
    analysis_write_json($jobPath, $job);
    analysis_write_json(
        analysis_path($exchangeDir, 'jobs', 'latest', $requestKey . '.json'),
        [
            'jobId' => $jobId,
            'submissionId' => $submissionId,
            'attachmentIndex' => max(0, $fileIndex),
            'requestedAt' => $job['requestedAt'],
        ]
    );

    return $job + ['jobPath' => $jobPath];
}

function analysis_read_state(string $exchangeDir, string $originalSubmissionId, int $fileIndex): array
{
    $pointerPath = analysis_path(
        $exchangeDir,
        'jobs',
        'latest',
        analysis_request_key($originalSubmissionId, $fileIndex) . '.json'
    );
    $pointer = analysis_read_json($pointerPath);
    if ($pointer === null) {
        return ['status' => 'idle'];
    }

    $jobId = analysis_safe_id((string)($pointer['jobId'] ?? ''));
    $submissionId = analysis_safe_id((string)($pointer['submissionId'] ?? ''));
    if ($jobId === '' || $submissionId === '') {
        return ['status' => 'failed', 'errorMessage' => 'Analysis job pointer is invalid.'];
    }

    $result = analysis_read_json(analysis_path($exchangeDir, 'results', $submissionId, 'analysis.json'));
    if ($result !== null && (string)($result['jobId'] ?? '') === $jobId) {
        return [
            'status' => 'completed',
            'jobId' => $jobId,
            'submissionId' => $submissionId,
            'result' => $result,
        ];
    }

    foreach (['running', 'pending', 'failed', 'completed'] as $status) {
        $job = analysis_read_json(analysis_path($exchangeDir, 'jobs', $status, $jobId . '.json'));
        if ($job !== null) {
            return $job + ['status' => $status];
        }
    }

    return [
        'status' => 'failed',
        'jobId' => $jobId,
        'submissionId' => $submissionId,
        'errorMessage' => 'Analysis job file was not found.',
    ];
}

function analysis_resolve_evidence(
    string $exchangeDir,
    string $originalSubmissionId,
    int $fileIndex,
    string $relativeName
): string {
    $state = analysis_read_state($exchangeDir, $originalSubmissionId, $fileIndex);
    if (($state['status'] ?? '') !== 'completed' || !is_array($state['result'] ?? null)) {
        return '';
    }

    $safeName = analysis_safe_relative_path($relativeName);
    $registered = array_map('strval', array_slice((array)($state['result']['evidenceImages'] ?? []), 0, 3));
    if ($safeName === '' || !in_array($safeName, $registered, true)) {
        return '';
    }
    if (!in_array(strtolower(pathinfo($safeName, PATHINFO_EXTENSION)), ['jpg', 'jpeg', 'png'], true)) {
        return '';
    }

    $submissionId = (string)$state['submissionId'];
    $path = analysis_path($exchangeDir, 'results', $submissionId, ...explode('/', $safeName));
    return is_file($path) ? $path : '';
}

function analysis_safe_relative_path(string $path): string
{
    $path = str_replace('\\', '/', trim($path));
    if ($path === '' || str_starts_with($path, '/') || str_contains($path, ':')) {
        return '';
    }
    $parts = explode('/', $path);
    if (in_array('..', $parts, true) || in_array('', $parts, true)) {
        return '';
    }
    return implode('/', $parts);
}

function analysis_safe_id(string $value): string
{
    return preg_match('/^[A-Za-z0-9._-]{1,200}$/', $value) ? $value : '';
}

function analysis_path(string ...$parts): string
{
    if ($parts === []) {
        return '';
    }
    $root = rtrim((string)array_shift($parts), "/\\");
    $children = array_map(
        static fn(string $part): string => trim($part, "/\\"),
        $parts
    );
    return $children === [] ? $root : $root . DIRECTORY_SEPARATOR . implode(DIRECTORY_SEPARATOR, $children);
}

function analysis_read_json(string $path): ?array
{
    if (!is_file($path)) {
        return null;
    }
    $data = json_decode((string)file_get_contents($path), true);
    return is_array($data) ? $data : null;
}

function analysis_write_json(string $path, array $data): void
{
    $directory = dirname($path);
    if (!is_dir($directory) && !mkdir($directory, 0770, true) && !is_dir($directory)) {
        throw new RuntimeException('Could not create analysis directory.');
    }
    $temporaryPath = $path . '.tmp-' . bin2hex(random_bytes(4));
    $encoded = json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    if ($encoded === false || file_put_contents($temporaryPath, $encoded, LOCK_EX) === false) {
        throw new RuntimeException('Could not write analysis JSON.');
    }
    if (!rename($temporaryPath, $path)) {
        @unlink($temporaryPath);
        throw new RuntimeException('Could not publish analysis JSON.');
    }
}
