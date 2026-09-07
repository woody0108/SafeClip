# SafeClip

**블랙박스 영상을 쉽고 빠르게 확인하고 제출하는 Android 서비스**

USB-C 리더·microSD의 영상을 불러와 필요한 구간을 편집하고, 사건 정보와 함께 제출합니다. Android 앱부터 NAS 업로드 API, 회사 검토 웹까지 연결한 프로젝트입니다.

## 주요 기능

- **영상 관리**: 외장 저장소 및 `DCIM/SafeClip`의 영상·사진 조회와 미리보기
- **영상 편집**: 구간 선택, 프레임 캡처, 원본을 유지하는 음성 제거 사본 생성
- **자료 제출**: 사건 정보·첨부 파일 업로드, 로그인 및 제출 내역·처리 상태 확인
- **회사 검토**: NAS 저장 영상 확인과 Firestore 기반 검토 상태 관리
- **실시간 녹화 (실험)**: 화질·녹음 설정, 촬영 시각 표시, 위치 확보 시 MP4 위치 메타데이터 기록
- **이벤트 저장 (실험)**: 볼륨 내리기로 전후 1분, 올리기로 전후 5분 저장. 전후 5분 영상은 1분 단위로 분할하며, 녹화 구간이 부족하면 확보된 범위만 저장

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Android | Kotlin, Jetpack Compose, Material 3, ViewModel, Coroutines |
| 미디어·저장소 | CameraX, Media3, Storage Access Framework, MediaStore |
| 인증·데이터 | Firebase Authentication, Cloud Firestore |
| 서버 | PHP, Synology NAS / Web Station |
| 검증 | JUnit, Coroutines Test, Compose UI Test, Android Lint |

## 구현 포인트

- **권한 기반 파일 접근**: 외장 저장소는 SAF URI로 접근하고, Android의 제한된 사진·영상 접근 권한을 고려
- **이벤트 구간 처리**: 순환 녹화 조각에서 버튼 시점 기준 구간을 추출하고, 연속 요청을 순차 내보내기
- **예외 상황 대응**: 녹화 조기 종료, 위치 조회 실패, 반복 키 입력 등 경계 조건 처리 및 단위 테스트 작성
- **역할 분리**: 화면·상태 관리, 파일 접근, 미디어 처리, 업로드, 제출 데이터를 분리

## 실행

Android Studio에서 프로젝트를 열고 SDK 경로를 `local.properties`에 설정합니다. Firebase 연동에는 `app/google-services.json`, NAS·Kakao 연동에는 별도 로컬 설정이 필요합니다. 서비스 키와 서명 키는 커밋하지 않습니다.

```powershell
# Android Studio에 포함된 JBR 사용 (Windows)
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug
```

## 진행 상태

현재 **내부 테스트 단계**입니다. 실시간 녹화는 기기별 장시간 녹화·발열·저장 공간 부족·실제 블루투스 리모컨에 대한 추가 검증이 필요합니다. 영상 제출은 사용자 동의를 전제로 하며, 위반 여부 자동 판정이나 자동 신고 접수는 제공하지 않습니다.

## 상세 문서

- [프로젝트 구조](ARCHITECTURE.md)
- [제품 사양](docs/product-specs/README.md) · [설계 문서](docs/design-docs/README.md)
- [실기기 검증 기록](docs/exec-plans/2026-08-25-live-recording-device-validation.md)
- [회사 검토 웹 구성](company-web-server/README.md)
