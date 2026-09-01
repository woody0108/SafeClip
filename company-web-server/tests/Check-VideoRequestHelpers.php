<?php

declare(strict_types=1);

require dirname(__DIR__) . '/api/bootstrap.php';

$valid = sample_relative_path_from_id('sample-Driving%2FREC_2026_07_20_07_15_59_F.mp4');
if ($valid !== 'Driving/REC_2026_07_20_07_15_59_F.mp4') {
    throw new RuntimeException('Encoded sample folder path must be accepted.');
}

$traversal = sample_relative_path_from_id('sample-..%2Fsecret.mp4');
if ($traversal !== '') {
    throw new RuntimeException('Sample path traversal must be rejected.');
}

$ownerFolder = json_decode('"\\ubca0\\uc9f1\\uc774\\ub4e4"', true);
$escapedUnicodePath = clean_relative_path(
    '2026/08/12/\\ubca0\\uc9f1\\uc774\\ub4e4/01/video.mp4'
);
if ($escapedUnicodePath !== '2026/08/12/' . $ownerFolder . '/01/video.mp4') {
    throw new RuntimeException('Escaped Unicode folders must be normalized before path validation.');
}

$decoded = decode_json_object("\xEF\xBB\xBF" . '{"ok":true,"submissions":[]}');
if (($decoded['ok'] ?? false) !== true) {
    throw new RuntimeException('Upstream JSON with UTF-8 BOM must be decoded.');
}

$upstream = company_web_upstream_url(['upstream_company_web_url' => 'http://192.168.0.3:8080/']);
if ($upstream !== 'http://192.168.0.3:8080') {
    throw new RuntimeException('Upstream company web URL must be normalized.');
}

echo "Video request helper checks passed.\n";
