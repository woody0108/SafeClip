package com.glass.safeclip.data.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidEventLocationProvider(
    context: Context
) : EventLocationProvider {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    override suspend fun captureLocation(triggerEpochMs: Long): EventLocation? {
        if (!hasLocationPermission() || locationManager == null) return null

        return runCatching {
            val providers = currentProviders()
            val lastKnown = lastKnownLocations(providers + LocationManager.PASSIVE_PROVIDER)
            val current = LocationRequestCoordinator.collectAvailable(
                timeoutMs = CurrentLocationTimeoutMs,
                requests = providers.map { provider ->
                    { requestCurrentLocation(provider) }
                }
            )

            EventLocationSelection.closestToTrigger(
                candidates = (current + lastKnown).mapNotNull { it.toEventLocationOrNull() },
                triggerEpochMs = triggerEpochMs
            )
        }.getOrNull()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun currentProviders(): List<String> {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return buildList {
            if (fineGranted && locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }
    }

    @Suppress("MissingPermission")
    private fun lastKnownLocations(providers: List<String>): List<Location> {
        return providers.distinct().mapNotNull { provider ->
            runCatching { locationManager?.getLastKnownLocation(provider) }.getOrNull()
        }
    }

    @Suppress("MissingPermission", "DEPRECATION")
    private suspend fun requestCurrentLocation(provider: String): Location? {
        val manager = locationManager ?: return null
        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation { cancellationSignal.cancel() }
                runCatching {
                    manager.getCurrentLocation(
                        provider,
                        cancellationSignal,
                        appContext.mainExecutor
                    ) { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                }.onFailure {
                    if (continuation.isActive) continuation.resume(null)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }

                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                runCatching {
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }.onFailure {
                    manager.removeUpdates(listener)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    private fun Location.toEventLocationOrNull(): EventLocation? {
        if (!latitude.isFinite() || !longitude.isFinite()) return null
        return runCatching {
            EventLocation(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracy.takeIf { hasAccuracy() },
                capturedAtEpochMs = time
            )
        }.getOrNull()
    }

    private companion object {
        const val CurrentLocationTimeoutMs = 5_000L
    }
}
