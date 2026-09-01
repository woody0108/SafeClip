param(
    [int] $Port = 8080
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path $PSScriptRoot
$configPath = Join-Path $root 'config.php'

if (-not (Test-Path -LiteralPath $configPath -PathType Leaf)) {
    throw 'config.php가 없습니다. 로컬 영상 및 AI 교환 폴더 경로를 먼저 설정하세요.'
}

Write-Host "SafeClip NAS 연동 회사 웹을 시작합니다: http://127.0.0.1:$Port/"
Write-Host "로컬 샘플 확인 주소: http://127.0.0.1:$Port/?mode=sample"
php -S "127.0.0.1:$Port" -t $root
