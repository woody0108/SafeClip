package com.glass.safeclip.data.submission

import android.content.Context
import android.location.Geocoder
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AndroidSubmissionMetadataReader(
    private val context: Context
) {
    suspend fun read(attachment: SubmissionAttachment): SubmissionFileMetadata? {
        return withContext(Dispatchers.IO) {
            when (attachment.kind) {
                SubmissionAttachmentKind.Photo -> readPhoto(attachment)
                SubmissionAttachmentKind.Video -> readVideo(attachment)
            }
        }
    }

    private fun readPhoto(attachment: SubmissionAttachment): SubmissionFileMetadata? {
        val uri = Uri.parse(attachment.uriString)
        val exif = context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input)
        } ?: return null
        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        val latLong = FloatArray(2)
        val locationText = if (exif.getLatLong(latLong)) {
            roadLocationText(latLong[0].toDouble(), latLong[1].toDouble())
        } else {
            null
        }

        return metadataOrNull(
            dateTime = formatPhotoDate(dateTime),
            locationText = locationText
        )
    }

    private fun readVideo(attachment: SubmissionAttachment): SubmissionFileMetadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(attachment.uriString))
            val dateTime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
            val locationText = parseVideoLocation(location)?.let { (latitude, longitude) ->
                roadLocationText(latitude, longitude)
            }
            metadataOrNull(
                dateTime = formatVideoDate(dateTime),
                locationText = locationText
            )
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun metadataOrNull(dateTime: String?, locationText: String?): SubmissionFileMetadata? {
        if (dateTime.isNullOrBlank() && locationText.isNullOrBlank()) return null
        return SubmissionFileMetadata(
            incidentDateTimeText = dateTime,
            locationText = locationText
        )
    }

    private fun formatVideoDate(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val normalized = value.trim()
        val parsed = runCatching {
            SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(normalized)
        }.getOrNull() ?: runCatching {
            SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(normalized)
        }.getOrNull()
        return parsed?.let {
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(it)
        } ?: normalized
    }

    private fun formatPhotoDate(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val parsed = runCatching {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(value.trim())
        }.getOrNull()
        return parsed?.let {
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(it)
        } ?: value
    }

    private fun parseVideoLocation(value: String?): Pair<Double, Double>? {
        if (value.isNullOrBlank()) return null
        val match = Regex("([+-]\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?)").find(value) ?: return null
        return match.groupValues[1].toDoubleOrNull()?.let { latitude ->
            match.groupValues[2].toDoubleOrNull()?.let { longitude -> latitude to longitude }
        }
    }

    @Suppress("DEPRECATION")
    private fun roadLocationText(latitude: Double, longitude: Double): String {
        val address = runCatching {
            Geocoder(context, Locale.KOREA).getFromLocation(latitude, longitude, 1)?.firstOrNull()
        }.getOrNull()
        val parts = listOfNotNull(
            address?.adminArea,
            address?.locality,
            address?.subLocality,
            address?.thoroughfare,
            address?.featureName
        ).filter { it.isNotBlank() }.distinct()
        if (parts.isNotEmpty()) {
            return parts.joinToString(" ") + " 인근"
        }
        return String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
    }
}
