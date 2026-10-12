package com.glass.safeclip.ui.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.glass.safeclip.data.media.Media3EventClipAssembler
import com.glass.safeclip.data.media.AndroidSafeClipMediaPublisher
import com.glass.safeclip.data.plate.AndroidPlateEventAnalyzer
import com.glass.safeclip.data.recording.AndroidEventLocationProvider
import com.glass.safeclip.data.recording.CameraXRecordingSessionController
import com.glass.safeclip.data.recording.RollingSegmentRepository
import java.io.File

class LiveRecordingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appContext = context.applicationContext
        val root = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        val repository = RollingSegmentRepository(File(root, "recording/segments"))
        val plateAnalyzer = AndroidPlateEventAnalyzer(
            cacheDirectory = File(appContext.cacheDir, "plate-analysis").apply { mkdirs() },
            publisher = AndroidSafeClipMediaPublisher(appContext)
        )
        val controller = CameraXRecordingSessionController(
            appContext,
            repository,
            frameAnalyzer = plateAnalyzer
        )
        val assembler = Media3EventClipAssembler(appContext, repository)
        return LiveRecordingViewModel(
            controller = controller,
            segments = repository,
            assembler = assembler,
            locationProvider = AndroidEventLocationProvider(appContext),
            plateAnalyzer = plateAnalyzer
        ) as T
    }
}
