package com.glass.safeclip.ui.folder

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.video.VideoListText

@Composable
fun ImagePreviewScreen(
    file: ManagedFolderFile,
    onBack: () -> Unit
) {
    SafeClipScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SafeClipTopBar(
                title = "사진 미리보기",
                subtitle = file.displayName,
                trailing = {
                    SecondaryActionButton(text = "뒤로", onClick = onBack)
                }
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 560.dp),
                    factory = { context ->
                        ImageView(context).apply {
                            adjustViewBounds = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    },
                    update = { imageView ->
                        imageView.setImageURI(Uri.parse(file.uriString))
                    }
                )
                Text(
                    text = VideoListText.fileSizeLabel(file.sizeBytes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
