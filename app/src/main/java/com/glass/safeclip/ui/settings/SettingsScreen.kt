package com.glass.safeclip.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.components.TopBarIconButton
import com.glass.safeclip.ui.theme.SafeClipError
import com.glass.safeclip.ui.theme.SafeClipSuccess

@Composable
fun SettingsScreen(
    permissionItems: List<SettingsPermissionItem>,
    onBack: () -> Unit,
    onSelectFolder: () -> Unit,
    onRequestMediaLibrary: () -> Unit,
    onRequestCamera: () -> Unit,
    onRequestMicrophone: () -> Unit,
    onRequestLocation: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    SafeClipScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SafeClipTopBar(
                subtitle = "앱 환경과 권한 관리",
                trailing = {
                    TopBarIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "뒤로가기",
                        onClick = onBack
                    )
                }
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "설정",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SettingsPermissionItems.summary(permissionItems),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
                Text(
                    text = "권한은 해당 기능을 사용할 때만 사용하며 언제든 Android 설정에서 변경할 수 있습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
            }

            permissionItems.forEach { item ->
                PermissionSettingCard(
                    item = item,
                    onClick = when (item.kind) {
                        SettingsPermissionKind.Folder -> onSelectFolder
                        SettingsPermissionKind.MediaLibrary -> onRequestMediaLibrary
                        SettingsPermissionKind.Camera -> onRequestCamera
                        SettingsPermissionKind.Microphone -> onRequestMicrophone
                        SettingsPermissionKind.Location -> onRequestLocation
                    }
                )
            }

            SecondaryActionButton(
                text = "Android 앱 설정 열기",
                onClick = onOpenSystemSettings,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PermissionSettingCard(
    item: SettingsPermissionItem,
    onClick: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.label,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(Modifier.size(10.dp)) {
                    drawCircle(if (item.granted) SafeClipSuccess else SafeClipError)
                }
                Text(
                    text = if (item.granted) "허용됨" else "필요",
                    color = if (item.granted) SafeClipSuccess else SafeClipError,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        Text(
            text = item.description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 21.sp
        )
        SecondaryActionButton(
            text = if (item.granted && item.kind != SettingsPermissionKind.Folder) "허용됨" else item.actionText,
            onClick = onClick,
            enabled = !item.granted || item.kind == SettingsPermissionKind.Folder,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
