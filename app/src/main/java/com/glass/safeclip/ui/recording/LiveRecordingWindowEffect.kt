package com.glass.safeclip.ui.recording

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

internal object LiveRecordingOrientation {
    const val recording = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    const val afterRecording = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

@Composable
fun LiveRecordingWindowEffect(isRecording: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        activity.requestedOrientation = LiveRecordingOrientation.recording
        onDispose {
            activity.requestedOrientation = LiveRecordingOrientation.afterRecording
        }
    }

    DisposableEffect(activity, isRecording) {
        val oldBrightness = activity.window.attributes.screenBrightness
        val keptScreenOn = activity.window.attributes.flags and
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0

        if (isRecording) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = 0.08f }
        }
        onDispose {
            if (!keptScreenOn) activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = oldBrightness }
        }
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("실시간 녹화 화면에는 Activity가 필요합니다.")
}
