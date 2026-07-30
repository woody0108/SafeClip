<?php

declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');

echo json_encode([
    'ok' => true,
    'upload_max_filesize' => ini_get('upload_max_filesize'),
    'post_max_size' => ini_get('post_max_size'),
    'max_input_time' => ini_get('max_input_time'),
    'max_execution_time' => ini_get('max_execution_time'),
    'memory_limit' => ini_get('memory_limit'),
    'user_ini_filename' => ini_get('user_ini.filename'),
    'user_ini_cache_ttl' => ini_get('user_ini.cache_ttl'),
    'loaded_ini_file' => php_ini_loaded_file(),
    'scanned_ini_files' => php_ini_scanned_files(),
], JSON_UNESCAPED_SLASHES);
