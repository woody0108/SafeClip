<?php

return [
    // NAS에서 나중에 실제 긴 키로 교체합니다.
    'upload_key' => 'replace-this-with-a-long-random-secret',

    // Synology 공유 폴더 SafeClipUpLoads의 NAS 내부 경로입니다.
    'storage_dir' => '/volume1/SafeClipUpLoads',

    // PHP 자체 upload_max_filesize/post_max_size보다 크게 설정해도 PHP 제한을 넘을 수 없습니다.
    'max_file_bytes' => 2 * 1024 * 1024 * 1024,

    'allowed_extensions' => ['mp4', 'mov', 'avi', 'ts'],
];
