package com.glass.safeclip

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings as AndroidSettings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glass.safeclip.data.auth.AuthConnectionResult
import com.glass.safeclip.app.AuthUserProfileFactory
import com.glass.safeclip.app.ExistingUserLoginPolicy
import com.glass.safeclip.app.ExistingUserLoginResult
import com.glass.safeclip.app.FolderLoadCoordinator
import com.glass.safeclip.app.FolderManagerFileSelector
import com.glass.safeclip.app.FolderPermissionSnapshot
import com.glass.safeclip.app.MediaLibraryAccessLevel
import com.glass.safeclip.app.MediaLibraryPermissionPlan
import com.glass.safeclip.app.SavedFolderRestorePlan
import com.glass.safeclip.app.SavedFolderRestorePlanner
import com.glass.safeclip.app.SubmittedFilePreviewRoute
import com.glass.safeclip.app.SubmissionAttachmentSourceFiles
import com.glass.safeclip.app.SubmissionLookupSelector
import com.glass.safeclip.data.auth.FirebaseAuthConnector
import com.glass.safeclip.data.ask.AskDeleteResult
import com.glass.safeclip.data.ask.AskItem
import com.glass.safeclip.data.ask.AskListResult
import com.glass.safeclip.data.ask.AskSaveResult
import com.glass.safeclip.data.ask.FirestoreAskRepository
import com.glass.safeclip.data.file.AndroidDocumentTreeVideoSource
import com.glass.safeclip.data.file.AndroidManagedFileOperator
import com.glass.safeclip.data.file.LastSelectedEventFolderStore
import com.glass.safeclip.data.file.LastSelectedFolderStore
import com.glass.safeclip.data.file.ManagedFileOperation
import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.ManagedFolderFileList
import com.glass.safeclip.data.file.ManagedFolderFileScanner
import com.glass.safeclip.data.file.ManagedFolderVideoCandidate
import com.glass.safeclip.data.file.VideoScanner
import com.glass.safeclip.data.identity.GuestIdentityStore
import com.glass.safeclip.data.media.AndroidFrameCaptureStore
import com.glass.safeclip.data.media.AndroidSafeClipSavedMediaRepository
import com.glass.safeclip.data.media.AndroidVideoClipExporter
import com.glass.safeclip.data.media.Media3AudioRemovalExporter
import com.glass.safeclip.data.media.MediaExportProgress
import com.glass.safeclip.data.media.SafeClipMediaSaveLocation
import com.glass.safeclip.data.profile.FirestoreUserProfileRepository
import com.glass.safeclip.data.profile.UserProfile
import com.glass.safeclip.data.profile.UserProfileSyncResult
import com.glass.safeclip.data.submission.FirestoreSubmissionRepository
import com.glass.safeclip.data.submission.AndroidSubmissionMetadataReader
import com.glass.safeclip.data.submission.NasSubmissionUploadClient
import com.glass.safeclip.data.submission.NasSubmissionUploadProgress
import com.glass.safeclip.data.submission.SubmissionAttachment
import com.glass.safeclip.data.submission.SubmissionInput
import com.glass.safeclip.data.submission.SubmissionDeleteResult
import com.glass.safeclip.data.submission.SubmissionListResult
import com.glass.safeclip.data.submission.SubmissionOwnerLinkResult
import com.glass.safeclip.data.submission.SubmissionSaveResult
import com.glass.safeclip.ui.folder.FolderManagerScreen
import com.glass.safeclip.ui.folder.FolderManagerText
import com.glass.safeclip.ui.folder.FolderViewKind
import com.glass.safeclip.ui.folder.ImagePreviewScreen
import com.glass.safeclip.ui.home.ExitConfirmDialog
import com.glass.safeclip.ui.home.MainHomeScreen
import com.glass.safeclip.ui.home.ReportWarningDialog
import com.glass.safeclip.ui.navigation.SafeClipBackNavigation
import com.glass.safeclip.ui.navigation.SafeClipScreen
import com.glass.safeclip.ui.onboarding.BootLoadingScreen
import com.glass.safeclip.ui.onboarding.ConnectingScreen
import com.glass.safeclip.ui.onboarding.StartScreen
import com.glass.safeclip.ui.recording.LiveRecordingScreen
import com.glass.safeclip.ui.recording.LiveRecordingViewModel
import com.glass.safeclip.ui.recording.LiveRecordingViewModelFactory
import com.glass.safeclip.ui.settings.AskScreen
import com.glass.safeclip.ui.settings.MyPageScreen
import com.glass.safeclip.ui.settings.SettingsPermissionItems
import com.glass.safeclip.ui.settings.SettingsScreen
import com.glass.safeclip.ui.status.LocalSubmissionRecord
import com.glass.safeclip.ui.status.SubmissionStatusScreen
import com.glass.safeclip.ui.submission.SubmissionFormScreen
import com.glass.safeclip.ui.theme.SafeClipTheme
import com.glass.safeclip.ui.video.VideoBrowserScreen
import com.glass.safeclip.ui.video.VideoBrowserSource
import com.glass.safeclip.ui.video.VideoListState
import com.glass.safeclip.ui.video.VideoPreviewScreen
import com.kakao.vectormap.KakaoMapSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    private var liveRecordingKeyHandler: ((Int, Long) -> Boolean)? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount == 0 &&
            liveRecordingKeyHandler?.invoke(keyCode, event.eventTime) == true
        ) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.SAFECLIP_KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoMapSdk.init(this, BuildConfig.SAFECLIP_KAKAO_NATIVE_APP_KEY)
        }
        enableEdgeToEdge()

        val source = AndroidDocumentTreeVideoSource(this)
        val scanner = VideoScanner()
        val managedFileScanner = ManagedFolderFileScanner()
        val folderLoadCoordinator = FolderLoadCoordinator(scanner, managedFileScanner)
        val folderStore = LastSelectedFolderStore(this)
        val eventFolderStore = LastSelectedEventFolderStore(this)
        val frameCaptureStore = AndroidFrameCaptureStore(this, eventFolderStore)
        val clipExporter = AndroidVideoClipExporter(this, eventFolderStore)
        val audioRemovalExporter = Media3AudioRemovalExporter(this)
        val savedMediaRepository = AndroidSafeClipSavedMediaRepository(
            context = this,
            eventFolderStore = eventFolderStore,
            useAccessibleSelectionFallback = {
                MediaLibraryPermissionPlan.accessLevel(Build.VERSION.SDK_INT, ::hasPermission) ==
                    MediaLibraryAccessLevel.Limited
            }
        )
        val managedFileOperator = AndroidManagedFileOperator(this)
        val guestIdentityStore = GuestIdentityStore(this)
        val guestId = guestIdentityStore.loadOrCreate()
        val reportWarningPreferences = getSharedPreferences(REPORT_WARNING_PREFERENCES, Context.MODE_PRIVATE)
        val firebaseAuthConnector = FirebaseAuthConnector(this)
        val askRepository = FirestoreAskRepository(
            askApiUrl = FirestoreAskRepository.askApiUrlFromUploadUrl(BuildConfig.SAFECLIP_NAS_UPLOAD_URL),
            uploadKey = BuildConfig.SAFECLIP_NAS_UPLOAD_KEY
        )
        val userProfileRepository = FirestoreUserProfileRepository()
        val submissionRepository = FirestoreSubmissionRepository()
        val submissionMetadataReader = AndroidSubmissionMetadataReader(this)
        val nasUploadClient = NasSubmissionUploadClient(
            context = this,
            uploadUrl = BuildConfig.SAFECLIP_NAS_UPLOAD_URL,
            uploadKey = BuildConfig.SAFECLIP_NAS_UPLOAD_KEY
        )
        @Suppress("DEPRECATION")
        val dcimDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)

        setContent {
            SafeClipTheme {
                var state by remember { mutableStateOf(VideoListState()) }
                var screen by remember { mutableStateOf<SafeClipScreen>(SafeClipScreen.first()) }
                var submissionRecords by remember { mutableStateOf<List<LocalSubmissionRecord>>(emptyList()) }
                var authMessage by remember { mutableStateOf<String?>(null) }
                var syncedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
                var askItems by remember { mutableStateOf<List<AskItem>>(emptyList()) }
                var askLoading by remember { mutableStateOf(false) }
                var folderManagerMessage by remember { mutableStateOf<String?>(null) }
                var folderManagerFiles by remember { mutableStateOf<List<ManagedFolderFile>>(emptyList()) }
                var currentFolderFiles by remember { mutableStateOf<List<ManagedFolderFile>>(emptyList()) }
                var eventFolderFiles by remember { mutableStateOf<List<ManagedFolderFile>>(emptyList()) }
                var folderManagerRefreshing by remember { mutableStateOf(false) }
                var hasMediaLibraryPermission by remember {
                    mutableStateOf(
                        MediaLibraryPermissionPlan.isGranted(Build.VERSION.SDK_INT, ::hasPermission)
                    )
                }
                var hasCameraPermission by remember { mutableStateOf(hasPermission(Manifest.permission.CAMERA)) }
                var hasMicrophonePermission by remember { mutableStateOf(hasPermission(Manifest.permission.RECORD_AUDIO)) }
                var hasLocationPermission by remember {
                    mutableStateOf(
                        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
                var pendingRecordingStart by remember { mutableStateOf<(() -> Unit)?>(null) }
                var pendingAudioDisable by remember { mutableStateOf<(() -> Unit)?>(null) }
                var showExitConfirmDialog by remember { mutableStateOf(false) }
                var showReportWarningDialog by remember {
                    mutableStateOf(!reportWarningPreferences.getBoolean(REPORT_WARNING_DISMISSED_KEY, false))
                }
                var hideReportWarningAgain by remember { mutableStateOf(false) }
                var nasUploadProgress by remember { mutableStateOf<NasSubmissionUploadProgress?>(null) }
                var nasUploadFailureMessage by remember { mutableStateOf<String?>(null) }
                var retryUploadSubmissionId by remember { mutableStateOf<String?>(null) }
                var pendingFileOperation by remember {
                    mutableStateOf<Pair<ManagedFolderFile, ManagedFileOperation>?>(null)
                }
                val scope = rememberCoroutineScope()

                fun persistedReadUris(): Set<String> {
                    return contentResolver.persistedUriPermissions
                        .filter { it.isReadPermission }
                        .map { it.uri.toString() }
                        .toSet()
                }

                fun persistedWriteUris(): Set<String> {
                    return contentResolver.persistedUriPermissions
                        .filter { it.isWritePermission }
                        .map { it.uri.toString() }
                        .toSet()
                }

                fun folderPermissionSnapshot(): FolderPermissionSnapshot {
                    return FolderPermissionSnapshot(
                        persistedReadUriStrings = persistedReadUris(),
                        persistedWriteUriStrings = persistedWriteUris()
                    )
                }

                fun hasFolderPermission(): Boolean {
                    return folderPermissionSnapshot().canRestore(state.selectedFolderUriString)
                }

                fun savedFolderRestorePlan(savedUriString: String?): SavedFolderRestorePlan {
                    return SavedFolderRestorePlanner.plan(
                        savedUriString = savedUriString,
                        permissionSnapshot = folderPermissionSnapshot()
                    )
                }

                fun refreshRuntimePermissions() {
                    hasMediaLibraryPermission = MediaLibraryPermissionPlan.isGranted(
                        sdkInt = Build.VERSION.SDK_INT,
                        hasPermission = ::hasPermission
                    )
                    hasCameraPermission = hasPermission(Manifest.permission.CAMERA)
                    hasMicrophonePermission = hasPermission(Manifest.permission.RECORD_AUDIO)
                    hasLocationPermission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                }

                fun refreshSavedMediaCountAsync() {
                    scope.launch {
                        val files = withContext(Dispatchers.IO) {
                            savedMediaRepository.listSavedItems()
                        }
                        state = state.copy(savedMediaItemCount = files.size)
                        eventFolderFiles = files
                        if ((screen as? SafeClipScreen.FolderManager)?.kind == FolderViewKind.SafeClipSaved) {
                            folderManagerFiles = files
                        }
                    }
                }

                suspend fun loadFolderData(
                    uri: Uri,
                    fallbackName: String = "선택한 폴더"
                ): Pair<VideoListState, List<ManagedFolderFile>> {
                    val savedMediaItemCount = state.savedMediaItemCount
                    return withContext(Dispatchers.IO) {
                        val root = source.loadTree(uri)
                        val result = folderLoadCoordinator.fromRoot(
                            root = root,
                            selectedUriString = uri.toString(),
                            fallbackName = fallbackName,
                            savedMediaItemCount = savedMediaItemCount
                        )
                        result.state to result.files
                    }
                }

                fun currentFolderFilesOrFallback(): List<ManagedFolderFile> {
                    return ManagedFolderFileList.preferScannedFiles(
                        scannedFiles = currentFolderFiles,
                        fallbackVideos = state.videos
                    )
                }

                fun submissionAttachmentSourceFiles(): SubmissionAttachmentSourceFiles {
                    return SubmissionAttachmentSourceFiles.from(
                        currentFolderFiles = currentFolderFiles,
                        eventFolderFiles = eventFolderFiles,
                        fallbackVideos = state.videos
                    )
                }

                fun folderManagerFilesFor(kind: FolderViewKind): List<ManagedFolderFile> {
                    return FolderManagerFileSelector.select(
                        kind = kind,
                        eventFolderFiles = eventFolderFiles,
                        currentFolderFiles = currentFolderFiles,
                        fallbackVideos = state.videos
                    )
                }

                suspend fun loadStartupDataBeforeHome() {
                    refreshRuntimePermissions()

                    val files = withContext(Dispatchers.IO) {
                        SafeClipMediaSaveLocation.ensurePublicDirectory(dcimDirectory)
                        savedMediaRepository.listSavedItems()
                    }
                    eventFolderFiles = files
                    state = state.copy(
                        savedMediaItemCount = files.size,
                        isLoading = false
                    )

                    val savedUri = folderStore.load()
                    val savedUriString = savedUri?.toString()
                    val restorePlan = savedFolderRestorePlan(savedUriString)
                    if (restorePlan == SavedFolderRestorePlan.Restore && savedUri != null) {
                        state = state.copy(isLoading = true, errorMessage = null)
                        val (loadedState, loadedFiles) = loadFolderData(savedUri, fallbackName = "이전 선택 폴더")
                        state = loadedState.copy(savedMediaItemCount = files.size)
                        currentFolderFiles = ManagedFolderFileList.preferScannedFiles(loadedFiles, loadedState.videos)
                        folderManagerFiles = currentFolderFiles
                    } else if (restorePlan == SavedFolderRestorePlan.PermissionLost) {
                        state = state.copy(
                            isLoading = false,
                            errorMessage = "이전 폴더에 연결 권한이 없어 다시 선택해야 삭제/이동할 수 있습니다."
                        )
                    } else {
                        state = state.copy(isLoading = false)
                    }
                }

                fun reloadCurrentFolderIfPossibleAsync() {
                    val folderUri = state.selectedFolderUriString?.let(Uri::parse) ?: return
                    scope.launch {
                        state = state.copy(isLoading = true, errorMessage = null)
                        val (loadedState, loadedFiles) = loadFolderData(
                            folderUri,
                            fallbackName = state.selectedFolderName ?: "선택한 폴더"
                        )
                        state = loadedState
                        currentFolderFiles = ManagedFolderFileList.preferScannedFiles(loadedFiles, loadedState.videos)
                        folderManagerFiles = currentFolderFiles
                    }
                }

                fun refreshFolderManagerFilesAsync(kind: FolderViewKind) {
                    scope.launch {
                        when (kind) {
                            FolderViewKind.SafeClipSaved -> {
                                folderManagerRefreshing = true
                                val refreshedFiles = withContext(Dispatchers.IO) {
                                    savedMediaRepository.listSavedItems()
                                }
                                eventFolderFiles = refreshedFiles
                                if ((screen as? SafeClipScreen.FolderManager)?.kind == FolderViewKind.SafeClipSaved) {
                                    folderManagerFiles = refreshedFiles
                                }
                                folderManagerRefreshing = false
                            }
                            FolderViewKind.CurrentFolder -> {
                                folderManagerRefreshing = false
                                folderManagerFiles = currentFolderFilesOrFallback()
                            }
                        }
                    }
                }

                fun openFolderManager(kind: FolderViewKind) {
                    folderManagerMessage = null
                    folderManagerRefreshing = kind == FolderViewKind.SafeClipSaved
                    folderManagerFiles = folderManagerFilesFor(kind)
                    screen = SafeClipScreen.FolderManager(kind)
                    refreshFolderManagerFilesAsync(kind)
                }

                fun isSafeClipBrowserFile(file: ManagedFolderFile, source: VideoBrowserSource): Boolean {
                    return source == VideoBrowserSource.SafeClip ||
                        (source == VideoBrowserSource.All && eventFolderFiles.any { it.uriString == file.uriString })
                }

                fun browserFolderPath(file: ManagedFolderFile, source: VideoBrowserSource): String {
                    return if (isSafeClipBrowserFile(file, source)) {
                        "SafeClip 폴더"
                    } else {
                        state.selectedFolderName ?: "블랙박스 폴더"
                    }
                }

                fun goBack() {
                    SafeClipBackNavigation.previousScreen(screen)?.let { previous ->
                        screen = previous
                    }
                }

                fun showAuthResult(result: AuthConnectionResult) {
                    authMessage = when (result) {
                        is AuthConnectionResult.SignedIn -> {
                            val name = result.displayName ?: result.email ?: "계정"
                            "$name 계정으로 연결되었습니다."
                        }
                        is AuthConnectionResult.NeedsFirebaseSetup -> result.message
                        is AuthConnectionResult.Failed -> result.message
                    }
                }

                suspend fun linkGuestSubmissionsToProfile(profile: UserProfile): String {
                    return when (val result = submissionRepository.linkGuestSubmissionsToUser(profile)) {
                        is SubmissionOwnerLinkResult.Success -> result.message
                        is SubmissionOwnerLinkResult.Failed -> result.message
                    }
                }

                suspend fun syncSignedInUser(result: AuthConnectionResult) {
                    if (result !is AuthConnectionResult.SignedIn) {
                        showAuthResult(result)
                        return
                    }
                    val profile = AuthUserProfileFactory.from(result, guestId) ?: return
                    when (val syncResult = userProfileRepository.saveLogin(profile)) {
                        is UserProfileSyncResult.Success -> {
                            syncedUserProfile = syncResult.profile
                            val name = result.displayName ?: result.email ?: "계정"
                            val ownerLinkMessage = linkGuestSubmissionsToProfile(syncResult.profile)
                            authMessage = "$name 계정으로 연결되었습니다. ${syncResult.message} $ownerLinkMessage"
                        }
                        is UserProfileSyncResult.Failed -> {
                            val name = result.displayName ?: result.email ?: "계정"
                            authMessage = "$name 계정 연결은 완료됐지만 ${syncResult.message}"
                        }
                    }
                }

                suspend fun loginExistingUser(result: AuthConnectionResult): Boolean {
                    if (result !is AuthConnectionResult.SignedIn) {
                        showAuthResult(result)
                        return false
                    }
                    when (val existing = ExistingUserLoginPolicy.evaluate(userProfileRepository.load(result.uid))) {
                        is ExistingUserLoginResult.Allowed -> {
                            val profile = AuthUserProfileFactory.from(result, guestId) ?: existing.profile
                            when (val syncResult = userProfileRepository.saveLogin(profile)) {
                                is UserProfileSyncResult.Success -> {
                                    syncedUserProfile = syncResult.profile
                                    val name = result.displayName ?: result.email ?: "계정"
                                    authMessage = "$name 계정으로 로그인되었습니다."
                                    return true
                                }
                                is UserProfileSyncResult.Failed -> {
                                    syncedUserProfile = existing.profile
                                    authMessage = syncResult.message
                                    return true
                                }
                            }
                        }
                        is ExistingUserLoginResult.Rejected -> {
                            firebaseAuthConnector.signOut()
                            authMessage = existing.message
                            return false
                        }
                    }
                }

                suspend fun loadCurrentUserProfile() {
                    val currentUser = firebaseAuthConnector.currentSignedInUser() ?: return
                    loginExistingUser(currentUser)
                }

                fun loadCurrentUserProfileAsync() {
                    scope.launch {
                        loadCurrentUserProfile()
                    }
                }

                fun currentAskId(): String {
                    return syncedUserProfile?.uid ?: guestId
                }

                suspend fun loadAskItems() {
                    askLoading = true
                    when (val result = askRepository.listById(currentAskId())) {
                        is AskListResult.Success -> {
                            askItems = result.items
                        }
                        is AskListResult.Failed -> {
                            authMessage = result.message
                        }
                    }
                    askLoading = false
                }

                fun loadAskItemsAsync() {
                    scope.launch {
                        loadAskItems()
                    }
                }

                suspend fun loadSubmissionRecords() {
                    val lookupKey = SubmissionLookupSelector.from(
                        ownerUid = firebaseAuthConnector.currentUserUid(),
                        guestId = guestId
                    )
                    when (val result = submissionRepository.findBy(lookupKey)) {
                        is SubmissionListResult.Success -> {
                            submissionRecords = result.records
                            authMessage = result.message
                        }
                        is SubmissionListResult.Failed -> {
                            authMessage = result.message
                        }
                    }
                }

                fun refreshSubmissionRecordsAsync() {
                    scope.launch {
                        loadSubmissionRecords()
                    }
                }

                fun continueStartupToHome() {
                    screen = SafeClipScreen.Connecting
                    refreshSubmissionRecordsAsync()
                    scope.launch {
                        loadStartupDataBeforeHome()
                        screen = SafeClipScreen.Home
                    }
                }

                BackHandler(enabled = screen == SafeClipScreen.Start) {
                    showExitConfirmDialog = true
                }

                BackHandler(
                    enabled = screen != SafeClipScreen.Start &&
                        screen != SafeClipScreen.LiveRecording &&
                        SafeClipBackNavigation.previousScreen(screen) != null
                ) {
                    goBack()
                }

                LaunchedEffect(screen) {
                    if (screen == SafeClipScreen.Home) {
                        loadAskItems()
                        refreshSavedMediaCountAsync()
                    }
                    if (screen !is SafeClipScreen.SubmissionForm) {
                        retryUploadSubmissionId = null
                        nasUploadFailureMessage = null
                    }
                }

                val settingsPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    refreshRuntimePermissions()
                    refreshSavedMediaCountAsync()
                }

                val permissionLifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(permissionLifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            refreshRuntimePermissions()
                            refreshSavedMediaCountAsync()
                        }
                    }
                    permissionLifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        permissionLifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val recordingPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    hasCameraPermission = hasPermission(Manifest.permission.CAMERA)
                    val cameraGranted = results[Manifest.permission.CAMERA] ?: hasCameraPermission
                    val storageGranted = Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
                        (results[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                            ?: hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    val locationGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (!cameraGranted || !storageGranted) {
                        Toast.makeText(this@MainActivity, "녹화에 필요한 권한이 없습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        val audioGranted = results[Manifest.permission.RECORD_AUDIO]
                            ?: hasPermission(Manifest.permission.RECORD_AUDIO)
                        if (!audioGranted) {
                            pendingAudioDisable?.invoke()
                            Toast.makeText(this@MainActivity, "마이크 권한이 없어 영상만 녹화합니다.", Toast.LENGTH_SHORT).show()
                        }
                        if (!locationGranted) {
                            Toast.makeText(
                                this@MainActivity,
                                "위치 권한이 없어 이벤트 영상에 위치정보를 넣지 않습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        pendingRecordingStart?.invoke()
                    }
                    pendingRecordingStart = null
                    pendingAudioDisable = null
                }

                val folderPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    if (uri == null) {
                        state = state.copy(errorMessage = "폴더 선택이 취소되었습니다.")
                        return@rememberLauncherForActivityResult
                    }

                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    folderStore.save(uri)

                    screen = SafeClipScreen.VideoBrowser(VideoBrowserSource.Blackbox)
                    scope.launch {
                        state = state.copy(isLoading = true, errorMessage = null)
                        val (loadedState, loadedFiles) = loadFolderData(uri)
                        state = loadedState
                        currentFolderFiles = ManagedFolderFileList.preferScannedFiles(loadedFiles, loadedState.videos)
                    }
                }

                LaunchedEffect(Unit) {
                    val bootStartedAt = System.currentTimeMillis()
                    withTimeoutOrNull(BOOT_MAX_DURATION_MS) {
                        refreshRuntimePermissions()
                        loadCurrentUserProfile()
                        loadSubmissionRecords()
                        val savedMediaFiles = withContext(Dispatchers.IO) {
                            SafeClipMediaSaveLocation.ensurePublicDirectory(dcimDirectory)
                            savedMediaRepository.listSavedItems()
                        }
                        state = state.copy(savedMediaItemCount = savedMediaFiles.size)
                        eventFolderFiles = savedMediaFiles
                        val savedUri = folderStore.load()
                        val savedUriString = savedUri?.toString()
                        val restorePlan = savedFolderRestorePlan(savedUriString)

                        if (restorePlan == SavedFolderRestorePlan.Restore && savedUri != null) {
                            state = state.copy(isLoading = true, errorMessage = null)
                            val (loadedState, loadedFiles) = loadFolderData(savedUri, fallbackName = "이전 선택 폴더")
                            state = loadedState
                            currentFolderFiles = ManagedFolderFileList.preferScannedFiles(loadedFiles, loadedState.videos)
                        } else if (restorePlan == SavedFolderRestorePlan.PermissionLost) {
                            state = state.copy(
                                errorMessage = "이전 폴더는 쓰기 권한이 없어 다시 선택해야 삭제/이동할 수 있습니다."
                            )
                        }
                    }
                    val elapsed = System.currentTimeMillis() - bootStartedAt
                    if (elapsed < BOOT_MIN_VISIBLE_MS) {
                        delay(BOOT_MIN_VISIBLE_MS - elapsed)
                    }
                    if (screen == SafeClipScreen.Boot) {
                        screen = SafeClipScreen.Start
                    }
                }

                val operationDestinationPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    val pending = pendingFileOperation
                    pendingFileOperation = null
                    if (uri == null || pending == null) {
                        folderManagerMessage = "대상 폴더 선택이 취소되었습니다."
                        return@rememberLauncherForActivityResult
                    }
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    scope.launch {
                        val (file, operation) = pending
                        val ok = withContext(Dispatchers.IO) {
                            when (operation) {
                                ManagedFileOperation.Copy -> managedFileOperator.copyToFolder(file, uri)
                                ManagedFileOperation.Move -> managedFileOperator.moveToFolder(file, uri)
                            }
                        }
                        folderManagerMessage = if (ok) {
                            if (operation == ManagedFileOperation.Copy) {
                                "복사 완료: ${file.displayName}"
                            } else {
                                "이동 완료: ${file.displayName}"
                            }
                        } else {
                            "파일 작업에 실패했습니다. 대상 폴더 권한을 확인해주세요."
                        }
                        refreshSavedMediaCountAsync()
                        reloadCurrentFolderIfPossibleAsync()
                        refreshFolderManagerFilesAsync(screen.let { (it as? SafeClipScreen.FolderManager)?.kind ?: FolderViewKind.SafeClipSaved })
                    }
                }

                if (showExitConfirmDialog) {
                    ExitConfirmDialog(
                        onCancel = { showExitConfirmDialog = false },
                        onConfirm = {
                            showExitConfirmDialog = false
                            finish()
                        }
                    )
                }

                if (showReportWarningDialog) {
                    ReportWarningDialog(
                        checked = hideReportWarningAgain,
                        onCheckedChange = { hideReportWarningAgain = it },
                        onConfirm = {
                            if (hideReportWarningAgain) {
                                reportWarningPreferences.edit()
                                    .putBoolean(REPORT_WARNING_DISMISSED_KEY, true)
                                    .apply()
                            }
                            showReportWarningDialog = false
                        }
                    )
                }

                when (val currentScreen = screen) {
                    SafeClipScreen.Boot -> BootLoadingScreen()

                    SafeClipScreen.Start -> StartScreen(
                        linkedDisplayName = syncedUserProfile?.displayName,
                        linkedEmail = syncedUserProfile?.email ?: firebaseAuthConnector.currentUserEmail(),
                        authMessage = authMessage,
                        onGoogleLogin = {
                            scope.launch {
                                if (loginExistingUser(firebaseAuthConnector.signInWithGoogle())) {
                                    refreshSubmissionRecordsAsync()
                                }
                            }
                        },
                        onEmailLogin = { email, password ->
                            scope.launch {
                                if (loginExistingUser(firebaseAuthConnector.signInWithEmail(email, password))) {
                                    refreshSubmissionRecordsAsync()
                                }
                            }
                        },
                        onStart = ::continueStartupToHome
                    )

                    SafeClipScreen.Connecting -> ConnectingScreen()

                    SafeClipScreen.Home -> MainHomeScreen(
                        state = state,
                        eventFolderFiles = eventFolderFiles,
                        currentFolderFiles = currentFolderFilesOrFallback(),
                        folderPermissionGranted = hasFolderPermission(),
                        cameraPermissionGranted = hasCameraPermission,
                        mediaLibraryPermissionGranted = hasMediaLibraryPermission,
                        submissionCount = submissionRecords.size,
                        askAnswerCount = askItems.count { it.answer.isNotBlank() },
                        onOpenFolderPermissionSettings = {
                            screen = SafeClipScreen.Settings()
                        },
                        onRequestMediaLibraryPermission = {
                            settingsPermissionLauncher.launch(
                                MediaLibraryPermissionPlan.requiredPermissions(Build.VERSION.SDK_INT)
                                    .toTypedArray()
                            )
                        },
                        onOpenRecentEvents = {
                            if (hasFolderPermission() && hasCameraPermission) {
                                screen = SafeClipScreen.VideoBrowser(VideoBrowserSource.Blackbox)
                            } else {
                                screen = SafeClipScreen.Settings()
                            }
                        },
                        onOpenFolder = {
                            if (it == FolderViewKind.SafeClipSaved) {
                                screen = SafeClipScreen.VideoBrowser(VideoBrowserSource.SafeClip)
                            } else if (it == FolderViewKind.CurrentFolder) {
                                screen = SafeClipScreen.VideoBrowser(VideoBrowserSource.Blackbox)
                            } else {
                                openFolderManager(it)
                            }
                        },
                        onOpenStatus = {
                            refreshSubmissionRecordsAsync()
                            screen = SafeClipScreen.SubmissionStatus
                        },
                        onOpenMyPage = {
                            screen = SafeClipScreen.MyPage
                            scope.launch {
                                loadCurrentUserProfile()
                                loadAskItems()
                            }
                        },
                        onOpenSettings = {
                            screen = SafeClipScreen.Settings()
                        },
                        onOpenLiveRecording = {
                            if (hasCameraPermission) {
                                screen = SafeClipScreen.LiveRecording
                            } else {
                                screen = SafeClipScreen.Settings()
                            }
                        },
                        onBack = ::goBack
                    )

                    SafeClipScreen.LiveRecording -> {
                        val liveViewModel: LiveRecordingViewModel = viewModel(
                            factory = remember { LiveRecordingViewModelFactory(this@MainActivity) }
                        )
                        val liveState by liveViewModel.state.collectAsState()
                        val lifecycleOwner = LocalLifecycleOwner.current

                        DisposableEffect(liveViewModel, lifecycleOwner) {
                            liveRecordingKeyHandler = liveViewModel::onRemoteKey
                            val observer = LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_STOP) liveViewModel.stopRecording()
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                                liveRecordingKeyHandler = null
                                liveViewModel.leaveScreen()
                            }
                        }

                        LiveRecordingScreen(
                            state = liveState,
                            onStart = startRecording@{
                                if (!hasPermission(Manifest.permission.CAMERA)) {
                                    screen = SafeClipScreen.Settings()
                                    return@startRecording
                                }
                                val missing = buildList {
                                    if (liveState.audioEnabled && !hasPermission(Manifest.permission.RECORD_AUDIO)) {
                                        add(Manifest.permission.RECORD_AUDIO)
                                    }
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                                        !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    ) {
                                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                    if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                                        !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    ) {
                                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
                                }
                                if (missing.isEmpty()) {
                                    liveViewModel.startRecording()
                                } else {
                                    pendingRecordingStart = liveViewModel::startRecording
                                    pendingAudioDisable = { liveViewModel.setAudioEnabled(false) }
                                    recordingPermissionLauncher.launch(missing.toTypedArray())
                                }
                            },
                            onStop = liveViewModel::stopRecording,
                            onEvent = { liveViewModel.requestEvent() },
                            onQualityChange = liveViewModel::setQuality,
                            onAudioChange = liveViewModel::setAudioEnabled,
                            onRemoteTestChange = liveViewModel::setRemoteTestEnabled,
                            onBack = {
                                liveViewModel.leaveScreen()
                                screen = SafeClipScreen.Home
                            },
                            onPreviewReady = { provider -> liveViewModel.bind(lifecycleOwner, provider) }
                        )
                    }

                    SafeClipScreen.MyPage -> MyPageScreen(
                        guestId = guestId,
                        linkedEmail = syncedUserProfile?.email ?: firebaseAuthConnector.currentUserEmail(),
                        linkedDisplayName = syncedUserProfile?.displayName,
                        linkedProvider = syncedUserProfile?.provider,
                        message = authMessage,
                        permissionItems = SettingsPermissionItems.from(
                            folderGranted = hasFolderPermission(),
                            mediaLibraryGranted = hasMediaLibraryPermission,
                            cameraGranted = hasCameraPermission,
                            microphoneGranted = hasMicrophonePermission,
                            locationGranted = hasLocationPermission
                        ),
                        inquiryCount = askItems.size,
                        answerCount = askItems.count { it.answer.isNotBlank() },
                        onBack = ::goBack,
                        onGoogleLogin = {
                            scope.launch {
                                if (loginExistingUser(firebaseAuthConnector.signInWithGoogle())) {
                                    refreshSubmissionRecordsAsync()
                                }
                            }
                        },
                        onEmailLogin = { email, password ->
                            scope.launch {
                                if (loginExistingUser(firebaseAuthConnector.signInWithEmail(email, password))) {
                                    refreshSubmissionRecordsAsync()
                                }
                            }
                        },
                        onSignOut = {
                            authMessage = firebaseAuthConnector.signOut()
                            syncedUserProfile = null
                            refreshSubmissionRecordsAsync()
                        },
                        onOpenSettings = {
                            screen = SafeClipScreen.Settings(SafeClipScreen.MyPage)
                        },
                        onOpenAsk = {
                            scope.launch {
                                loadAskItems()
                                screen = SafeClipScreen.Ask
                            }
                        },
                        onDeleteAccount = {
                            scope.launch {
                                authMessage = when (val reauth = firebaseAuthConnector.reauthenticateCurrentUser()) {
                                    is AuthConnectionResult.Failed -> reauth.message
                                    is AuthConnectionResult.NeedsFirebaseSetup -> reauth.message
                                    is AuthConnectionResult.SignedIn -> {
                                        when (val submissionDelete = submissionRepository.deleteByOwnerUid(reauth.uid)) {
                                            is SubmissionDeleteResult.Failed -> submissionDelete.message
                                            is SubmissionDeleteResult.Success -> when (val firestoreDelete = userProfileRepository.delete(reauth.uid)) {
                                            is UserProfileSyncResult.Failed -> firestoreDelete.message
                                            is UserProfileSyncResult.Success -> {
                                                when (val authDelete = firebaseAuthConnector.deleteCurrentUser()) {
                                                    is AuthConnectionResult.SignedIn -> {
                                                        syncedUserProfile = null
                                                        submissionRecords = emptyList()
                                                        "${authDelete.email ?: "계정"} 회원탈퇴가 완료되었습니다. ${submissionDelete.message} ${firestoreDelete.message}"
                                                    }
                                                    is AuthConnectionResult.NeedsFirebaseSetup -> authDelete.message
                                                    is AuthConnectionResult.Failed -> authDelete.message
                                                }
                                            }
                                        }
                                        }
                                    }
                                }
                            }
                        }
                    )

                    is SafeClipScreen.Settings -> SettingsScreen(
                        permissionItems = SettingsPermissionItems.from(
                            folderGranted = hasFolderPermission(),
                            mediaLibraryGranted = hasMediaLibraryPermission,
                            cameraGranted = hasCameraPermission,
                            microphoneGranted = hasMicrophonePermission,
                            locationGranted = hasLocationPermission
                        ),
                        onBack = ::goBack,
                        onSelectFolder = { folderPicker.launch(null) },
                        onRequestMediaLibrary = {
                            settingsPermissionLauncher.launch(
                                MediaLibraryPermissionPlan.requiredPermissions(Build.VERSION.SDK_INT)
                                    .toTypedArray()
                            )
                        },
                        onRequestCamera = {
                            settingsPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        },
                        onRequestMicrophone = {
                            settingsPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        },
                        onRequestLocation = {
                            settingsPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onOpenSystemSettings = {
                            startActivity(
                                Intent(
                                    AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        }
                    )

                    is SafeClipScreen.FolderManager -> FolderManagerScreen(
                        kind = currentScreen.kind,
                        files = folderManagerFiles,
                        isRefreshing = folderManagerRefreshing,
                        message = folderManagerMessage,
                        onBack = ::goBack,
                        onDelete = { file ->
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    managedFileOperator.delete(file)
                                }
                                folderManagerMessage = if (ok) {
                                    "삭제 완료: ${file.displayName}"
                                } else {
                                    "삭제하지 못했습니다. 블랙박스 폴더를 다시 선택해 쓰기 권한을 허용해주세요."
                                }
                                refreshSavedMediaCountAsync()
                                reloadCurrentFolderIfPossibleAsync()
                                refreshFolderManagerFilesAsync(currentScreen.kind)
                            }
                        },
                        onStartOperation = { file, operation ->
                            pendingFileOperation = file to operation
                            operationDestinationPicker.launch(null)
                        },
                        onPlayVideo = { file ->
                            val video = ManagedFolderVideoCandidate.from(
                                file = file,
                                folderPath = FolderManagerText.title(currentScreen.kind)
                            )
                            if (video != null) {
                                screen = SafeClipScreen.VideoPreview(video)
                            } else {
                                folderManagerMessage = "영상 파일만 재생할 수 있습니다."
                            }
                        },
                        onPreviewImage = { file ->
                            screen = SafeClipScreen.ImagePreview(file, currentScreen.kind)
                        },
                        onSubmitVideo = { file ->
                            val video = ManagedFolderVideoCandidate.fromSubmittableFile(
                                file = file,
                                folderPath = FolderManagerText.title(currentScreen.kind)
                            )
                            val attachment = SubmissionAttachment.fromManagedFile(
                                file = file,
                                folderPath = FolderManagerText.title(currentScreen.kind)
                            )
                            if (video != null) {
                                val sourceFiles = submissionAttachmentSourceFiles()
                                screen = SafeClipScreen.SubmissionForm(
                                    video = video,
                                    clip = null,
                                    initialAttachment = attachment ?: SubmissionAttachment.fromVideoCandidate(video),
                                    availableFiles = sourceFiles.selectedFolderFiles,
                                    availableFolderPath = FolderManagerText.title(currentScreen.kind),
                                    eventFiles = sourceFiles.eventFolderFiles,
                                    returnScreen = currentScreen
                                )
                            } else {
                                folderManagerMessage = "영상 또는 JPG 파일만 제출할 수 있습니다."
                            }
                        }
                    )

                    SafeClipScreen.Ask -> AskScreen(
                        askId = currentAskId(),
                        askItems = askItems,
                        isLoading = askLoading,
                        message = authMessage,
                        onBack = ::goBack,
                        onRefresh = {
                            loadAskItemsAsync()
                        },
                        onDelete = { item ->
                            scope.launch {
                                authMessage = when (val result = askRepository.delete(item.documentId)) {
                                    is AskDeleteResult.Success -> {
                                        loadAskItems()
                                        result.message
                                    }
                                    is AskDeleteResult.Failed -> result.message
                                }
                            }
                        },
                        onSubmit = { questionType, question ->
                            scope.launch {
                                authMessage = when (
                                    val result = askRepository.add(
                                        id = currentAskId(),
                                        questionType = questionType,
                                        question = question
                                    )
                                ) {
                                    is AskSaveResult.Success -> {
                                        loadAskItems()
                                        result.message
                                    }
                                    is AskSaveResult.Failed -> result.message
                                }
                            }
                        }
                    )

                    is SafeClipScreen.VideoBrowser -> VideoBrowserScreen(
                        state = state,
                        blackboxFiles = currentFolderFilesOrFallback(),
                        safeClipFiles = eventFolderFiles,
                        initialSource = currentScreen.initialSource,
                        onPlayVideo = { file, source ->
                            val folderPath = browserFolderPath(file, source)
                            val video = ManagedFolderVideoCandidate.from(
                                file = file,
                                folderPath = folderPath
                            )
                            if (video != null) {
                                screen = SafeClipScreen.VideoPreview(video, returnScreen = currentScreen)
                            }
                        },
                        onPreviewImage = { file, source ->
                            val sourceKind = if (isSafeClipBrowserFile(file, source)) {
                                FolderViewKind.SafeClipSaved
                            } else {
                                FolderViewKind.CurrentFolder
                            }
                            screen = SafeClipScreen.ImagePreview(
                                file = file,
                                sourceKind = sourceKind,
                                returnScreen = currentScreen
                            )
                        },
                        onSubmitFile = { file, source ->
                            val folderPath = browserFolderPath(file, source)
                            val video = ManagedFolderVideoCandidate.fromSubmittableFile(
                                file = file,
                                folderPath = folderPath
                            )
                            val attachment = SubmissionAttachment.fromManagedFile(
                                file = file,
                                folderPath = folderPath
                            )
                            if (video != null) {
                                val sourceFiles = submissionAttachmentSourceFiles()
                                screen = SafeClipScreen.SubmissionForm(
                                    video = video,
                                    clip = null,
                                    initialAttachment = attachment ?: SubmissionAttachment.fromVideoCandidate(video),
                                    availableFiles = sourceFiles.selectedFolderFiles,
                                    availableFolderPath = folderPath,
                                    eventFiles = sourceFiles.eventFolderFiles,
                                    returnScreen = currentScreen
                                )
                            }
                        },
                        onBackHome = ::goBack
                    )

                    is SafeClipScreen.VideoPreview -> VideoPreviewScreen(
                        video = currentScreen.video,
                        onBack = ::goBack,
                        onCaptureFrame = { uri, displayName, positionMs ->
                            val result = runCatching {
                                frameCaptureStore.captureFrame(uri, displayName, positionMs)
                            }
                            if (result.isSuccess) refreshSavedMediaCountAsync()
                            result
                        },
                        onExportClip = { selected, selection ->
                            val result = runCatching {
                                clipExporter.exportClip(selected, selection)
                            }
                            if (result.isSuccess) refreshSavedMediaCountAsync()
                            result
                        },
                        canRemoveAudio = currentScreen.video.folderPath.contains("SafeClip", ignoreCase = true),
                        onRemoveAudio = { uri, displayName ->
                            val result = runCatching {
                                when (val progress = audioRemovalExporter.export(uri, displayName)
                                    .first { it !is MediaExportProgress.Running }) {
                                    is MediaExportProgress.Completed -> progress.uri
                                    is MediaExportProgress.Failed -> error(progress.message)
                                    is MediaExportProgress.Running -> error("음성 제거 작업이 완료되지 않았습니다.")
                                }
                            }
                            if (result.isSuccess) refreshSavedMediaCountAsync()
                            result
                        },
                        onSubmit = { selected, clip ->
                            val sourceFiles = submissionAttachmentSourceFiles()
                            screen = SafeClipScreen.SubmissionForm(
                                video = selected,
                                clip = clip,
                                initialAttachment = SubmissionAttachment.fromVideoCandidate(selected),
                                availableFiles = sourceFiles.selectedFolderFiles,
                                availableFolderPath = selected.folderPath,
                                eventFiles = sourceFiles.eventFolderFiles,
                                returnScreen = currentScreen
                            )
                        }
                    )

                    is SafeClipScreen.ImagePreview -> ImagePreviewScreen(
                        file = currentScreen.file,
                        onBack = ::goBack
                    )

                    is SafeClipScreen.SubmissionForm -> SubmissionFormScreen(
                        clip = currentScreen.clip,
                        initialAttachment = currentScreen.initialAttachment,
                        availableFiles = currentScreen.availableFiles,
                        availableFolderPath = currentScreen.availableFolderPath,
                        eventFiles = currentScreen.eventFiles,
                        uploadProgress = nasUploadProgress,
                        uploadFailureMessage = nasUploadFailureMessage,
                        kakaoMapNativeAppKey = BuildConfig.SAFECLIP_KAKAO_NATIVE_APP_KEY,
                        kakaoRestApiKey = BuildConfig.SAFECLIP_KAKAO_REST_API_KEY,
                        onLoadRepresentativeMetadata = { attachment ->
                            submissionMetadataReader.read(attachment)
                        },
                        onBack = ::goBack,
                        onSubmit = { draft, representativeAttachment, attachments ->
                            scope.launch {
                                if (!nasUploadClient.isConfigured()) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "NAS 업로드 설정이 없습니다. local.properties에 safeclip.nasUploadUrl/key를 설정해주세요.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }
                                Toast.makeText(
                                    this@MainActivity,
                                    "NAS로 첨부 파일을 업로드합니다.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val ownerUid = firebaseAuthConnector.currentUserUid()
                                val submissionId = retryUploadSubmissionId ?: "safeclip-${System.currentTimeMillis()}".also {
                                    retryUploadSubmissionId = it
                                }
                                nasUploadFailureMessage = null
                                val submitterLabel = submissionSubmitterLabel(
                                    displayName = syncedUserProfile?.displayName,
                                    email = syncedUserProfile?.email ?: firebaseAuthConnector.currentUserEmail(),
                                    guestId = if (ownerUid == null) guestId else syncedUserProfile?.guestId
                                )
                                val uploadStartedAtMillis = System.currentTimeMillis()
                                nasUploadProgress = NasSubmissionUploadProgress(
                                    fileIndex = 1,
                                    totalFiles = attachments.size,
                                    fileName = attachments.firstOrNull()?.displayName ?: "첨부 파일",
                                    bytesSent = 0L,
                                    totalBytes = attachments.firstOrNull()?.sizeBytes,
                                    startedAtMillis = uploadStartedAtMillis,
                                    nowMillis = uploadStartedAtMillis
                                )
                                val uploadedAttachments = try {
                                    nasUploadClient.uploadAll(
                                        submissionId = submissionId,
                                        submitterLabel = submitterLabel,
                                        attachments = attachments,
                                        onProgress = { progress ->
                                            withContext(Dispatchers.Main) {
                                                nasUploadProgress = progress.copy(
                                                    startedAtMillis = uploadStartedAtMillis,
                                                    nowMillis = System.currentTimeMillis()
                                                )
                                            }
                                        }
                                    )
                                } catch (exception: Exception) {
                                    nasUploadProgress = null
                                    nasUploadFailureMessage = exception.localizedMessage ?: "NAS 업로드에 실패했습니다."
                                    Toast.makeText(
                                        this@MainActivity,
                                        "${nasUploadFailureMessage} 다시 제출하기를 누르면 재시도합니다.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }
                                nasUploadProgress = null
                                val uploadedRepresentative = uploadedAttachments.firstOrNull {
                                    it.uriString == representativeAttachment.uriString
                                } ?: representativeAttachment
                                val input = SubmissionInput(
                                    ownerUid = ownerUid,
                                    video = uploadedRepresentative.toRepresentativeVideoCandidate(),
                                    draft = draft,
                                    guestId = if (ownerUid == null) guestId else syncedUserProfile?.guestId,
                                    ownerDisplayName = syncedUserProfile?.displayName,
                                    nasSubmissionFolder = uploadedAttachments.firstOrNull()?.nasSubmissionFolder,
                                    submissionSequence = uploadedAttachments.firstOrNull()?.submissionSequence,
                                    submissionSequenceText = uploadedAttachments.firstOrNull()?.submissionSequenceText,
                                    attachments = uploadedAttachments
                                )
                                when (val result = submissionRepository.add(input)) {
                                    is SubmissionSaveResult.Success -> {
                                        retryUploadSubmissionId = null
                                        nasUploadFailureMessage = null
                                        Toast.makeText(
                                            this@MainActivity,
                                            result.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        refreshSubmissionRecordsAsync()
                                        screen = SafeClipScreen.Home
                                    }

                                    is SubmissionSaveResult.Failed -> {
                                        nasUploadFailureMessage = result.message
                                        Toast.makeText(
                                            this@MainActivity,
                                            "${result.message} 다시 제출하기를 누르면 저장을 재시도합니다.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    )

                    SafeClipScreen.SubmissionStatus -> SubmissionStatusScreen(
                        records = submissionRecords,
                        onBackHome = ::goBack,
                        onOpenSubmission = { },
                        onOpenSubmittedFile = { record ->
                            screen = SubmittedFilePreviewRoute.from(
                                record = record,
                                nasUploadUrl = BuildConfig.SAFECLIP_NAS_UPLOAD_URL
                            )
                        }
                    )
                }
            }
        }
    }

    private fun submissionSubmitterLabel(
        displayName: String?,
        email: String?,
        guestId: String?
    ): String {
        return listOf(
            displayName,
            email?.substringBefore("@"),
            guestId,
            "guest"
        ).first { !it.isNullOrBlank() }.orEmpty()
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val BOOT_MIN_VISIBLE_MS = 650L
        private const val BOOT_MAX_DURATION_MS = 1_500L
        private const val REPORT_WARNING_PREFERENCES = "safeclip_report_warning"
        private const val REPORT_WARNING_DISMISSED_KEY = "report_warning_dismissed"
    }
}
