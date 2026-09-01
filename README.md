# SafeClip

SafeClip은 블랙박스 영상 확인과 제출을 간단하게 만드는 Android 중심 서비스입니다. USB-C 카드 리더나 microSD에서 영상을 불러오고, 필요한 구간과 사진을 확인한 뒤 회사 검토 흐름으로 전달할 수 있습니다.

현재 저장소에는 Android 앱과 Synology NAS 기반 업로드·검토 도구가 함께 들어 있습니다.

## 주요 기능

- Storage Access Framework를 이용한 USB-C·microSD 블랙박스 폴더 선택
- 선택한 폴더와 `DCIM/SafeClip`의 영상·사진 조회 및 미리보기
- 영상 구간 선택, 프레임 캡처, 음성 제거 사본 생성
- 사건 정보와 여러 첨부 파일을 포함한 제출 흐름
- Firebase Authentication·Firestore 기반 로그인과 제출 상태 관리
- NAS 업로드 및 회사 검토 화면
- 휴대폰 실시간 녹화와 블루투스 리모컨 이벤트 저장 실험

실시간 녹화는 아직 기기별 장시간 녹화, 저장 공간 부족, 실제 블루투스 리모컨 등 추가 검증이 필요한 기능입니다. 현재 제품의 우선 범위는 기존 블랙박스 영상의 확인과 제출입니다.

## 저장소 구성

```text
SafeClip/
  app/                 Kotlin·Jetpack Compose Android 앱
  company-web-server/  Synology Web Station용 회사 검토 웹
  nas-upload-api/      Synology NAS용 PHP 업로드 API
  admin-web/           초기 React/Vite 관리자 화면
  docs/                제품 사양, 설계 문서, 실행 계획, 참고 자료
  tools/               스토어 이미지 등 보조 도구
```

상세한 모듈 설명은 [ARCHITECTURE.md](ARCHITECTURE.md), 작업 규칙은 [AGENTS.md](AGENTS.md)를 참고하세요.

## Android 개발 환경

- Android Studio
- JDK 11 이상 또는 Android Studio에 포함된 JBR
- Android SDK 36
- 최소 지원 버전: Android 7.0, API 24
- 애플리케이션 ID: `com.glass.safeclip`
- 현재 앱 버전: `1.0.2`, `versionCode 3`

프로젝트 루트 폴더를 Android Studio에서 열고 Gradle 동기화를 실행합니다.

## 로컬 설정

루트의 `local.properties`는 Git에 포함되지 않습니다. Android SDK 경로와 사용하는 서비스 키를 로컬에서 설정합니다.

```properties
sdk.dir=C\:\\Users\\사용자명\\AppData\\Local\\Android\\Sdk
safeclip.nasUploadUrl=https://example.com/upload.php
safeclip.nasUploadKey=replace-with-local-secret
safeclip.kakaoNativeAppKey=replace-with-local-key
safeclip.kakaoRestApiKey=replace-with-local-key
```

NAS 또는 Kakao 설정이 필요하지 않은 화면은 해당 값을 비워 둔 상태에서도 빌드할 수 있습니다. 실제 키, Firebase 서비스 계정, NAS의 `config.php`는 커밋하지 마세요.

Firebase Android 설정이 필요한 빌드에서는 올바른 `app/google-services.json`을 준비해야 합니다.

## 빌드와 검증

Windows PowerShell 기준 명령입니다.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin lintDebug
```

생성되는 디버그 APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Play Console용 AAB를 만들 때:

```powershell
.\gradlew.bat bundleRelease
```

릴리스 서명은 로컬 환경의 기존 업로드 키 설정을 사용해야 합니다. 생성물과 촬영 테스트용 MP4는 소스 저장소에 추가하지 않습니다.

## 파일 접근과 저장

- 외장 SD 카드와 USB 리더는 사용자가 선택한 폴더 URI를 통해 접근합니다.
- 앱이 생성한 영상과 사진은 기본적으로 `DCIM/SafeClip`에 저장합니다.
- Android 14 이상에서는 전체 또는 제한된 사진·영상 접근 권한을 모두 처리합니다.
- 대용량 영상은 전체 파일을 한 번에 메모리에 올리지 않는 흐름을 유지해야 합니다.

## 서버 구성

`nas-upload-api/`는 Android 앱이 제출 영상을 NAS로 전송하는 PHP API입니다. 공개 웹 폴더와 실제 영상 저장 폴더를 분리하고 업로드 키를 서버의 비공개 `config.php`에 설정합니다.

`company-web-server/`는 Firestore 제출 목록과 NAS 영상을 확인하고 검토 상태를 변경하는 회사용 화면입니다. 자세한 배포 방법은 [company-web-server/README.md](company-web-server/README.md)를 참고하세요.

## 관련 문서

- [프로젝트 구조](ARCHITECTURE.md)
- [제품 사양](docs/product-specs/README.md)
- [설계 문서](docs/design-docs/README.md)
- [실행 계획](docs/exec-plans/README.md)
- [실시간 녹화 기기 검증](docs/exec-plans/2026-08-25-live-recording-device-validation.md)

## 주의 사항

- 영상 저장, 제출, 위치·음성 처리처럼 개인정보와 관련된 동작은 사용자 선택과 동의를 전제로 합니다.
- SafeClip은 신고를 자동 판정하거나 자동 접수하지 않습니다. 회사가 자료를 검토하고 사용자가 최종 제출하는 흐름을 우선합니다.
- `device-test-artifacts/`에는 화면 검증용 이미지와 UI 덤프만 보관하며 실제 촬영 MP4는 보관하지 않습니다.
