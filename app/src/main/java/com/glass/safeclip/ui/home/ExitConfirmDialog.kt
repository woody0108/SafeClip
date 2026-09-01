package com.glass.safeclip.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun ExitConfirmDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val loweredButtonModifier = Modifier.padding(top = 8.dp)

    AlertDialog(
        onDismissRequest = onConfirm,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        title = { Text("앱을 종료하시겠습니까?") },
        confirmButton = {
            TextButton(
                modifier = loweredButtonModifier,
                onClick = onConfirm
            ) {
                Text("예")
            }
        },
        dismissButton = {
            TextButton(
                modifier = loweredButtonModifier,
                onClick = onCancel
            ) {
                Text("아니오")
            }
        }
    )
}
