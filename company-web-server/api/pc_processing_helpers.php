<?php

declare(strict_types=1);

function pc_processing_authorized(array $config, string $remoteAddress, string $providedToken): bool
{
    $allowedIp = trim((string)($config['pc_allowed_nas_ip'] ?? ''));
    $expectedToken = (string)($config['pc_processing_token'] ?? '');

    return $allowedIp !== ''
        && $remoteAddress === $allowedIp
        && $expectedToken !== ''
        && hash_equals($expectedToken, $providedToken);
}

function pc_preview_request(array $query): array
{
    $id = (string)($query['id'] ?? '');
    $file = (string)($query['file'] ?? '');
    if (!preg_match('/^[A-Za-z0-9._-]{1,200}$/', $id)) {
        return [];
    }
    if (!preg_match('/^(0|[1-9][0-9]{0,8})$/', $file)) {
        return [];
    }

    return ['id' => $id, 'fileIndex' => (int)$file];
}
