# SafeClip Company Web Server Handoff

이 문서는 새 Codex 작업을 시작할 때 현재 상태를 빠르게 이어받기 위한 기록이다.

## 현재까지 성공한 것

- Synology NAS Web Station에서 회사 검토 웹이 열림.
- 회사 검토 웹 위치:
  - 로컬 작업 폴더: `C:\Users\win11\GitHub\SafeClip\company-web-server`
  - NAS 배포 위치: `web/company-web-server`
  - 내부 테스트 주소: `http://NAS주소:8080/`
- NAS 샘플 영상 폴더 `/volume1/Videos` 읽기 성공.
- `http://NAS주소:8080/api/submissions.php?mode=sample`에서 샘플 영상 목록 JSON 확인 성공.
- `Videos` 공유 폴더에서 `http` 권한을 허용하니 영상 파일 읽기 성공.
- Firebase 서비스 계정 JSON을 NAS에 연결했고 Firestore 읽기 성공.
- 서비스 계정 IAM에 `Cloud Datastore User` 권한을 주니 Firestore `submissions` 목록을 읽음.
- 회사 웹에서 `originalFileName`과 `/volume1/Videos` 안의 실제 파일명이 같으면 영상 재생됨.
- 회사 웹의 `검토완료` 버튼으로 Firestore 문서의 `status`가 `completed`로 바뀜.
- Android 앱 제출 내역에서도 `completed`가 “완료”로 반영됨.

## 현재 회사 웹서버 구조

```text
company-web-server/
  index.html
  config.example.php
  README.md
  api/
    bootstrap.php
    diagnostics.php
    submissions.php
    status.php
    video.php
  assets/
    app.js
    styles.css
  data/
    .gitkeep
  tests/
    Check-CompanyWebServer.ps1
```

## 회사 웹서버가 하는 일

- `api/submissions.php`
  - 기본: NAS PHP가 Firestore `submissions` 컬렉션을 읽어서 JSON 반환.
  - 샘플 확인: `?mode=sample`을 붙이면 `/volume1/Videos` 폴더 영상 목록 반환.
  - 가능하면 `data/submissions-cache.json`에 마지막 응답 저장.
- `api/video.php`
  - 선택된 Firestore 문서의 `originalFileName` 또는 NAS 경로 필드로 NAS 영상 파일을 찾아 스트리밍.
  - 브라우저 영상 seek를 위해 HTTP Range 요청 지원.
- `api/status.php`
  - `검토완료` 버튼에서 호출.
  - Firestore 문서의 `status`를 `completed`로 수정.
  - 가능하면 `data/last-status-update.json`에 마지막 수정 내역 저장.
- `api/diagnostics.php`
  - NAS PHP 상태 확인용.
  - `curl`, `openssl`, Firebase 서비스 계정 JSON 경로/권한 확인.

## NAS 설정 메모

- DSM 버전: `DSM 7.1.1-42962 Update 9`
- NAS 모델: Synology DiskStation DS214
- Web Station으로 운영.
- 기존 홈페이지가 같은 NAS의 80/443을 사용 중.
- 회사 검토 웹은 별도 포트 `8080`으로 내부 테스트.
- `Videos` 공유 폴더는 PHP가 읽어야 하므로 DSM 공유 폴더 권한에서 `http`에 읽기 권한 필요.
- Firebase 서비스 계정 JSON은 웹 루트 밖에 둬야 한다.
  - 예: `/volume1/SafeClipSecrets/firebase-service-account.json`
- `web/company-web-server/config.php` 예시:

```php
<?php

return [
    'firebase_project_id' => 'safeclip-fd26c',
    'service_account_json' => '/volume1/SafeClipSecrets/firebase-service-account.json',
    'storage_dir' => '/volume1/SafeClipUploads',
    'sample_video_dir' => '/volume1/Videos',
    'max_submissions' => 50,
];
```

## Firestore 문서에서 확인된 주요 필드

현재 Android 앱은 Firestore `submissions` 문서에 아래와 같은 필드를 저장한다.

```text
createdAt
fileSizeBytes
guestId
incidentDateTime
incidentLocationText
originalFileName
originalFolderPath
originalLastModifiedMillis
ownerDisplayName
ownerEmail
ownerUid
reportReviewConsent
sourceUri
status
trafficRiskDataConsent
updatedAt
userMemo
videoStorageConsent
violationTypeCandidate
```

회사 웹은 목록/상세에서 주로 아래를 사용한다.

```text
ownerDisplayName / ownerEmail / guestId / ownerUid
createdAt
incidentDateTime
incidentLocationText
violationTypeCandidate
userMemo
originalFileName
fileSizeBytes
status
```

## 중요한 경계

### 서버웹 작업일 때

서버웹 작업은 아래 폴더만 수정한다.

```text
C:\Users\win11\GitHub\SafeClip\company-web-server
```

서버웹 작업에서 해도 되는 것:

- 회사 검토 화면 UI 수정.
- PHP API 수정.
- Firestore 문서 읽기/상태 수정 로직 수정.
- NAS 영상 보기 로직 수정.
- `company-web-server/tests/Check-CompanyWebServer.ps1` 수정.

서버웹 작업에서 건드리면 안 되는 것:

- Android 앱 코드 `app/`
- Android Gradle 설정
- Android Firestore 제출 생성 로직
- Android 화면/상태 표시 로직

### 모바일앱 작업일 때

모바일앱 작업은 아래 폴더를 중심으로 수정한다.

```text
C:\Users\win11\GitHub\SafeClip\app
```

모바일앱 작업에서 해야 할 다음 큰 일:

- 앱에서 NAS로 영상 업로드 연결.
- 업로드 성공 후 Firestore 문서와 NAS 파일명이 맞도록 유지.
- 가능하면 `originalFileName`과 NAS 저장 파일명을 동일하게 맞추거나, 별도 NAS 경로 필드를 Firestore에 추가.
- 업로드 진행률/실패/재시도 UX 추가.

모바일앱 작업에서 건드리면 안 되는 것:

- 회사 검토 웹 UI/API `company-web-server/`
- NAS Web Station 포털 설정 문서/파일
- Firebase 서비스 계정 JSON

## 다음 작업 우선순위

1. 회사 웹서버 polish:
   - Firestore 목록 카드와 상세 표시를 더 보기 좋게 정리.
   - 영상이 없는 제출 문서의 안내 문구 개선.
   - `completed` 외 상태 버튼이 필요한지 결정.
2. 모바일앱 NAS 업로드:
   - 기존 Firestore 문서 생성 흐름을 유지하면서 영상 파일을 NAS로 전송.
   - 전송 후 회사 웹에서 바로 재생되도록 파일명/경로 정합성 맞추기.
3. 운영 보안:
   - 회사 웹은 내부망 전용으로 유지.
   - 외부 업로드는 별도 업로드 전용 API/포트/보안 정책으로 분리.
   - 관리자 로그인은 추후 추가.

## 새 작업 시작 시 주의

- 먼저 이 문서를 읽고, 이번 작업이 `서버웹`인지 `모바일앱`인지 확정한다.
- 사용자가 “웹서버”, “회사 웹”, “검토 화면”이라고 하면 기본 작업 범위는 `company-web-server/`다.
- 사용자가 “앱”, “제출하기”, “업로드”, “S25”, “Android”라고 하면 기본 작업 범위는 `app/`다.
- 반대쪽 폴더는 사용자가 명시적으로 요청하지 않으면 수정하지 않는다.
- ZIP 파일은 만들지 않는다. 사용자는 `company-web-server` 폴더 자체를 NAS에 덮어쓴다.
