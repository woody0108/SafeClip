package com.glass.safeclip.data.media

import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation

@OptIn(UnstableApi::class)
class TimestampBandPadding : MatrixTransformation {
    lateinit var layout: TimestampBandLayout
        private set
    private val matrix = Matrix()

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        layout = TimestampBandLayout.forFrame(inputWidth, inputHeight)
        matrix.reset()
        matrix.setScale(1f, layout.scaleY)
        matrix.postTranslate(0f, layout.translateY)
        return Size(layout.width, layout.outputHeight)
    }

    override fun getMatrix(presentationTimeUs: Long): Matrix = matrix
}
