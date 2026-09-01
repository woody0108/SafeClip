package com.glass.safeclip.ui.settings

enum class SettingsPermissionKind {
    Folder,
    MediaLibrary,
    Camera,
    Microphone,
    Location
}

data class SettingsPermissionItem(
    val kind: SettingsPermissionKind,
    val label: String,
    val description: String,
    val granted: Boolean,
    val actionText: String
)

object SettingsPermissionItems {
    fun from(
        folderGranted: Boolean,
        mediaLibraryGranted: Boolean,
        cameraGranted: Boolean,
        microphoneGranted: Boolean,
        locationGranted: Boolean
    ): List<SettingsPermissionItem> {
        return listOf(
            SettingsPermissionItem(
                kind = SettingsPermissionKind.Folder,
                label = "저장 폴더",
                description = "블랙박스 파일과 SafeClip 저장 폴더를 관리합니다.",
                granted = folderGranted,
                actionText = "폴더 선택"
            ),
            SettingsPermissionItem(
                kind = SettingsPermissionKind.MediaLibrary,
                label = "저장된 사진·영상",
                description = "재설치 전 DCIM/SafeClip에 저장된 영상과 사진을 다시 불러옵니다.",
                granted = mediaLibraryGranted,
                actionText = "권한 요청"
            ),
            SettingsPermissionItem(
                kind = SettingsPermissionKind.Camera,
                label = "카메라",
                description = "실시간 녹화와 이벤트 영상 촬영에 사용합니다.",
                granted = cameraGranted,
                actionText = "권한 요청"
            ),
            SettingsPermissionItem(
                kind = SettingsPermissionKind.Microphone,
                label = "마이크",
                description = "사용자가 녹음을 켠 경우에만 영상 음성을 저장합니다.",
                granted = microphoneGranted,
                actionText = "권한 요청"
            ),
            SettingsPermissionItem(
                kind = SettingsPermissionKind.Location,
                label = "위치",
                description = "이벤트 버튼 시점의 위치를 영상 파일 정보에 기록합니다.",
                granted = locationGranted,
                actionText = "권한 요청"
            )
        )
    }

    fun summary(items: List<SettingsPermissionItem>): String {
        return "권한 ${items.size}개 중 ${items.count { it.granted }}개 사용 가능"
    }
}
