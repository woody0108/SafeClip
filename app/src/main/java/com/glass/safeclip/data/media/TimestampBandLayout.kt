package com.glass.safeclip.data.media

data class TimestampBandLayout(
    val width: Int,
    val contentHeight: Int,
    val bandHeight: Int
) {
    val outputHeight: Int get() = contentHeight + bandHeight
    val scaleY: Float get() = contentHeight.toFloat() / outputHeight
    val translateY: Float get() = -bandHeight.toFloat() / outputHeight

    companion object {
        fun forFrame(width: Int, height: Int): TimestampBandLayout {
            require(width > 0 && height > 0) { "Frame dimensions must be positive." }
            val minimumBand = maxOf(24, (width + 23) / 24)
            // 영상 인코더용 짝수 높이를 맞추되 원본 영역은 축소하지 않는다.
            val outputHeight = ((height + minimumBand + 1) / 2) * 2
            return TimestampBandLayout(width, height, outputHeight - height)
        }
    }
}
