package com.glass.safeclip.ui.submission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.theme.SafeClipBorder
import com.glass.safeclip.ui.theme.SafeClipCyan
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraPosition
import com.kakao.vectormap.camera.CameraUpdateFactory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun LocationPickerScreen(
    initialQuery: String,
    onBack: () -> Unit,
    onLocationSelected: (SubmissionLocationSelection) -> Unit
) {
    SafeClipScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SafeClipTopBar(
                title = "위치 선택",
                subtitle = "지도에서 신고 위치를 지정해주세요.",
                trailing = {
                    SecondaryActionButton(text = "뒤로", onClick = onBack)
                }
            )
            LocationPickerPanel(
                initialQuery = initialQuery,
                kakaoRestApiKey = "",
                onLocationSelected = onLocationSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LocationPickerPanel(
    initialQuery: String,
    kakaoRestApiKey: String,
    onLocationSelected: (SubmissionLocationSelection) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val geocoder = remember { Geocoder(context, Locale.KOREA) }
    var query by remember { mutableStateOf(initialQuery) }
    var selectedAddress by remember { mutableStateOf("") }
    var selectedPosition by remember { mutableStateOf<LatLng?>(null) }
    var message by remember { mutableStateOf("검색하거나 지도를 움직여 위치를 맞춰주세요.") }
    var searchResults by remember { mutableStateOf<List<KakaoLocationSearchResult>>(emptyList()) }
    var searchPanelExpanded by remember { mutableStateOf(true) }
    var lastSearchSelection by remember { mutableStateOf<KakaoLocationSelection?>(null) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var deviceStartPosition by remember { mutableStateOf<LatLng?>(null) }
    val mapViewHolder = remember { MapViewHolder() }

    fun updateFromPosition(position: LatLng) {
        lastSearchSelection?.takeIf { it.matches(position) }?.let {
            selectedAddress = it.addressText
            message = "선택한 위치를 확인해주세요."
            return
        }
        lastSearchSelection = null
        scope.launch {
            val address = reverseGeocode(geocoder, position)
            selectedAddress = address
            selectedPosition = if (address.isBlank()) null else position
            message = if (address.isBlank()) {
                "선택한 위치의 주소를 찾지 못했습니다. 지도를 조금 움직여 다시 확인해주세요."
            } else {
                "선택한 위치를 확인해주세요."
            }
        }
    }

    fun moveTo(position: LatLng, addressText: String) {
        lastSearchSelection = KakaoLocationSelection(position, addressText)
        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(position, 16))
        selectedAddress = addressText
        selectedPosition = position
        message = "선택한 위치를 확인해주세요."
        searchResults = emptyList()
        searchPanelExpanded = false
    }

    fun moveToDevicePosition(position: LatLng) {
        deviceStartPosition = position
        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(position, 16))
        updateFromPosition(position)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            readLastKnownDevicePosition(context)?.let(::moveToDevicePosition)
                ?: run {
                    message = "현재 위치를 바로 찾지 못했습니다. 검색하거나 지도를 움직여 위치를 맞춰주세요."
                }
        } else {
            message = "위치 권한이 없어 기본 위치에서 시작합니다. 검색하거나 지도를 움직여 위치를 맞춰주세요."
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            readLastKnownDevicePosition(context)?.let(::moveToDevicePosition)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun moveTo(address: Address) {
        moveTo(
            position = LatLng.from(address.latitude, address.longitude),
            addressText = address.toDisplayAddress()
        )
    }

    fun moveTo(result: KakaoLocationSearchResult) {
        moveTo(
            position = LatLng.from(result.latitude, result.longitude),
            addressText = result.selectedAddress
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            if (searchPanelExpanded) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("위치 검색") },
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryActionButton(
                    text = "키워드 검색",
                    onClick = {
                        scope.launch {
                            searchResults = emptyList()
                            val kakaoResults = searchKakaoLocations(kakaoRestApiKey, query)
                            if (kakaoResults.isNotEmpty()) {
                                searchResults = kakaoResults
                                message = "검색 결과에서 주소를 선택하면 지도가 그 위치로 이동합니다."
                            } else if (kakaoRestApiKey.isBlank()) {
                                val result = searchAddress(geocoder, query)
                                if (result == null) {
                                    message = "카카오 REST API 키가 없어서 간단 검색만 사용 중입니다. 검색 결과가 없습니다."
                                } else {
                                    moveTo(result)
                                }
                            } else {
                                message = "검색 결과가 없습니다. 도로명, 지번, 건물명을 조금 더 자세히 입력해주세요."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (searchResults.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        searchResults.forEach { result ->
                            KakaoLocationResultRow(
                                result = result,
                                onClick = { moveTo(result) }
                            )
                        }
                    }
                }
            } else {
                SecondaryActionButton(
                    text = "다시 검색",
                    onClick = { searchPanelExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = selectedAddress.ifBlank { message },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp
            )
            Text(
                text = "지도 가운데 핀이 신고 위치입니다. 손으로 지도를 움직인 뒤 위치를 확정해주세요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            PrimaryActionButton(
                text = "이 위치 사용",
                enabled = selectedAddress.isNotBlank() && selectedPosition != null,
                onClick = {
                    val position = selectedPosition ?: return@PrimaryActionButton
                    onLocationSelected(
                        SubmissionLocationSelection(
                            addressText = selectedAddress,
                            latitude = position.latitude,
                            longitude = position.longitude,
                            source = "map_selected"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    MapView(viewContext).apply {
                        mapViewHolder.view = this
                        start(
                            object : MapLifeCycleCallback() {
                                override fun onMapDestroy() = Unit

                                override fun onMapError(error: Exception) {
                                    message = "카카오 지도를 불러오지 못했습니다. Native app key와 키 해시 등록을 확인해주세요."
                                }
                            },
                            object : KakaoMapReadyCallback() {
                                override fun onMapReady(map: KakaoMap) {
                                    kakaoMap = map
                                    message = "지도가 준비되었습니다. 가운데 핀을 원하는 위치에 맞춰주세요."
                                    val startPosition = deviceStartPosition
                                        ?: map.cameraPosition?.position
                                        ?: DefaultMapPosition
                                    updateFromPosition(startPosition)
                                    map.setOnCameraMoveEndListener { _: KakaoMap, camera: CameraPosition, _: GestureType ->
                                        updateFromPosition(camera.position)
                                    }
                                }

                                override fun getPosition(): LatLng = deviceStartPosition ?: DefaultMapPosition

                                override fun getZoomLevel(): Int = 16
                            }
                        )
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            resume()
                        }
                    }
                }
            )
            Text(
                text = "+",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
                    .background(Color.Transparent),
                color = SafeClipCyan,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewHolder.view?.resume()
                Lifecycle.Event.ON_PAUSE -> mapViewHolder.view?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewHolder.view?.pause()
            mapViewHolder.view?.finish()
            mapViewHolder.view = null
        }
    }
}

private val DefaultMapPosition: LatLng = LatLng.from(37.5665, 126.9780)

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

@Suppress("MissingPermission")
private fun readLastKnownDevicePosition(context: Context): LatLng? {
    if (!hasLocationPermission(context)) return null
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    val location = providers
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull(Location::getTime)
        ?: return null
    return LatLng.from(location.latitude, location.longitude)
}

data class SubmissionLocationSelection(
    val addressText: String,
    val latitude: Double,
    val longitude: Double,
    val source: String
)

private class MapViewHolder {
    var view: MapView? = null
}

private data class KakaoLocationSelection(
    val position: LatLng,
    val addressText: String
) {
    fun matches(other: LatLng): Boolean {
        return abs(position.latitude - other.latitude) < 0.00001 &&
            abs(position.longitude - other.longitude) < 0.00001
    }
}

@Composable
private fun KakaoLocationResultRow(
    result: KakaoLocationSearchResult,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, SafeClipBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.zoneNo.ifBlank { result.title },
                    color = SafeClipCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "지도", color = MaterialTheme.colorScheme.primary)
            }
            result.roadAddress?.takeIf { it.isNotBlank() }?.let {
                Text(text = "도로명  $it", lineHeight = 19.sp)
            }
            result.landAddress?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "지번  $it",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

private data class KakaoLocationSearchResult(
    val title: String,
    val zoneNo: String,
    val roadAddress: String?,
    val landAddress: String?,
    val latitude: Double,
    val longitude: Double
) {
    val selectedAddress: String
        get() = roadAddress?.takeIf { it.isNotBlank() }
            ?: landAddress?.takeIf { it.isNotBlank() }
            ?: title
}

private suspend fun searchKakaoLocations(
    restApiKey: String,
    query: String
): List<KakaoLocationSearchResult> = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim()
    if (restApiKey.isBlank() || cleanQuery.isBlank()) return@withContext emptyList()
    val addressResults = runCatching {
        requestKakaoLocationSearch(
            restApiKey = restApiKey,
            endpoint = "https://dapi.kakao.com/v2/local/search/address.json",
            query = cleanQuery,
            size = 10
        ).parseAddressResults()
    }.getOrDefault(emptyList())
    val keywordResults = runCatching {
        requestKakaoLocationSearch(
            restApiKey = restApiKey,
            endpoint = "https://dapi.kakao.com/v2/local/search/keyword.json",
            query = cleanQuery,
            size = 10
        ).parseKeywordResults()
    }.getOrDefault(emptyList())

    (addressResults + keywordResults)
        .distinctBy { "${it.longitude},${it.latitude},${it.selectedAddress}" }
        .take(15)
}

private fun requestKakaoLocationSearch(
    restApiKey: String,
    endpoint: String,
    query: String,
    size: Int
): String {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val url = URL("$endpoint?query=$encodedQuery&size=$size")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 5000
        readTimeout = 5000
        setRequestProperty("Authorization", "KakaoAK $restApiKey")
    }
    return try {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun String.parseAddressResults(): List<KakaoLocationSearchResult> {
    val documents = JSONObject(this).optJSONArray("documents") ?: return emptyList()
    return buildList {
        for (index in 0 until documents.length()) {
            val item = documents.optJSONObject(index) ?: continue
            val road = item.optJSONObject("road_address")
            val land = item.optJSONObject("address")
            val x = item.optString("x").toDoubleOrNull() ?: continue
            val y = item.optString("y").toDoubleOrNull() ?: continue
            add(
                KakaoLocationSearchResult(
                    title = item.optString("address_name"),
                    zoneNo = road?.optString("zone_no").orEmpty(),
                    roadAddress = road?.optString("address_name"),
                    landAddress = land?.optString("address_name") ?: item.optString("address_name"),
                    latitude = y,
                    longitude = x
                )
            )
        }
    }
}

private fun String.parseKeywordResults(): List<KakaoLocationSearchResult> {
    val documents = JSONObject(this).optJSONArray("documents") ?: return emptyList()
    return buildList {
        for (index in 0 until documents.length()) {
            val item = documents.optJSONObject(index) ?: continue
            val x = item.optString("x").toDoubleOrNull() ?: continue
            val y = item.optString("y").toDoubleOrNull() ?: continue
            add(
                KakaoLocationSearchResult(
                    title = item.optString("place_name").ifBlank { item.optString("address_name") },
                    zoneNo = "",
                    roadAddress = item.optString("road_address_name"),
                    landAddress = item.optString("address_name"),
                    latitude = y,
                    longitude = x
                )
            )
        }
    }
}

private suspend fun searchAddress(
    geocoder: Geocoder,
    query: String
): Address? = withContext(Dispatchers.IO) {
    runCatching {
        geocoder.getFromLocationName(query.trim(), 1)?.firstOrNull()
    }.getOrNull()
}

private suspend fun reverseGeocode(
    geocoder: Geocoder,
    position: LatLng
): String = withContext(Dispatchers.IO) {
    runCatching {
        geocoder.getFromLocation(position.latitude, position.longitude, 1)
            ?.firstOrNull()
            ?.toDisplayAddress()
            .orEmpty()
    }.getOrDefault("")
}

private fun Address.toDisplayAddress(): String {
    getAddressLine(0)
        ?.replace("대한민국", "")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    return listOfNotNull(adminArea, subAdminArea, locality, subLocality, thoroughfare, subThoroughfare, featureName)
        .joinToString(" ")
        .takeIf { it.isNotBlank() }
        .orEmpty()
}
