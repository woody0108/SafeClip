# SafeClip 회사 웹서버

이 폴더는 NAS Web Station에 올리는 회사 관리자 웹입니다.

이 웹서버가 하는 일은 세 가지뿐입니다.

1. Firestore `submissions` 문서 목록 보기
2. NAS에 저장된 영상 보기
3. `검토완료` 버튼으로 Firestore `status`를 `completed`로 변경

업로드 기능은 이 웹서버에 없습니다. 영상 업로드는 Android 앱이 별도로 처리합니다.

## 지금 샘플 영상으로 먼저 보기

Windows에서 보이는 폴더:

```text
\\SyDisk\Videos
```

NAS Web Station/PHP에서는 보통 아래 경로로 읽습니다.

```text
/volume1/Videos
```

`config.php`가 아직 없어도 이 폴더에 `mp4`, `mov`, `avi`, `ts` 영상을 넣으면 회사 웹 화면에서 샘플 제출처럼 보여줍니다.

주의: NAS에 `config.php`가 이미 있으면 `config.example.php`는 읽지 않습니다. 그때는 `config.php` 안의 `sample_video_dir` 값을 `/volume1/Videos`로 맞춰야 합니다.

## NAS에 넣는 위치

DSM File Station에서 이 폴더를 아래처럼 넣습니다.

```text
web/
  company-web-server/
```

브라우저 주소:

```text
http://NAS주소/company-web-server/
```

## 처음 설정

1. `config.example.php`를 복사해서 `config.php`로 이름을 바꿉니다.
2. Firebase 서비스 계정 JSON 파일을 NAS의 웹 폴더 밖에 둡니다.
3. `config.php`의 `service_account_json` 경로를 실제 JSON 경로로 바꿉니다.
4. `storage_dir`가 실제 영상 저장 폴더와 맞는지 확인합니다.

예상 저장 폴더:

```text
/volume1/SafeClipUploads
```

## Firestore 문서에서 읽는 영상 경로

웹은 Firestore 제출 문서에서 아래 필드 중 하나를 찾습니다.

```text
nasFiles.front
nasFiles.rear
frontVideoPath
rearVideoPath
nasRelativePath
```

처음 앱 연결할 때는 앱이 영상 업로드 성공 후 Firestore 문서에 이 경로를 넣어주면 됩니다.

## 상태 변경

`검토완료` 버튼을 누르면 해당 Firestore 문서가 이렇게 바뀝니다.

```text
status = completed
updatedAt = 서버 시간
```

앱은 이미 `completed`를 “완료” 상태로 읽습니다.
