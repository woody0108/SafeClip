package com.glass.safeclip

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.glass.safeclip.data.auth.AuthConnectionResult
import com.glass.safeclip.app.AuthUserProfileFactory
import com.glass.safeclip.app.FolderLoadCoordinator
import com.glass.safeclip.app.FolderManagerFileSelector
import com.glass.safeclip.app.FolderPermissionSnapshot
import com.glass.safeclip.app.SavedFolderRestorePlan
import com.glass.safeclip.app.SavedFolderRestorePlanner
import com.glass.safeclip.app.SubmittedFilePreviewRoute
import com.glass.safeclip.app.SubmissionLookupSelector
import com.glass.safeclip.data.auth.FirebaseAuthConnector
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
import com.glass.safeclip.data.media.SafeClipEventFolder
import com.glass.safeclip.data.profile.FirestoreUserProfileRepository
import com.glass.safeclip.data.profile.UserProfile
import com.glass.safeclip.data.profile.UserProfileSyncResult
import com.glass.safeclip.data.submission.FirestoreSubmissionRepository
import com.glass.safeclip.data.submission.SubmissionInput
import com.glass.safeclip.data.submission.SubmissionListResult
import com.glass.safeclip.data.submission.SubmissionOwnerLinkResult
import com.glass.safeclip.data.submission.SubmissionSaveResult
import com.glass.safeclip.ui.folder.FolderManagerScreen
import com.glass.safeclip.ui.folder.FolderManagerText
import com.glass.safeclip.ui.folder.FolderViewKind
import com.glass.safeclip.ui.folder.ImagePreviewScreen
import com.glass.safeclip.ui.home.MainHomeScreen
import com.glass.safeclip.ui.navigation.SafeClipBackNavigation
import com.glass.safeclip.ui.navigation.SafeClipScreen
import com.glass.safeclip.ui.onboarding.BootLoadingScreen
import com.glass.safeclip.ui.onboarding.ConnectingScreen
import com.glass.safeclip.ui.onboarding.StartScreen
import com.glass.safeclip.ui.settings.SettingsScreen
import com.glass.safeclip.ui.status.LocalSubmissionRecord
import com.glass.safeclip.ui.status.SubmissionStatusScreen
import com.glass.safeclip.ui.submission.SubmissionFormScreen
import com.glass.safeclip.ui.theme.SafeClipTheme
import com.glass.safeclip.ui.video.VideoBrowserScreen
import com.glass.safeclip.ui.video.VideoListState
import com.glass.safeclip.ui.video.VideoPreviewScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val source = AndroidDocumentTreeVideoSource(this)
        val scanner = VideoScanner()
        val managedFileScanner = ManagedFolderFileScanner()
        val folderLoadCoordinator = FolderLoadCoordinator(scanner, managedFileScanner)
        val folderStore = LastSelectedFolderStore(this)
        val eventFolderStore = LastSelectedEventFolderStore(this)
        val safeClipEventFolder = SafeClipEventFolder(this, eventFolderStore)
        val frameCaptureStore = AndroidFrameCaptureStore(this, eventFolderStore)
        val clipExporter = AndroidVideoClipExporter(this, eventFolderStore)
        val savedMediaRepository = AndroidSafeClipSavedMediaRepository(this, eventFolderStore)
        val managedFileOperator = AndroidManagedFileOperator(this)
        val guestIdentityStore = GuestIdentityStore(this)
        val guestId = guestIdentityStore.loadOrCreate()
        val firebaseAuthConnector = FirebaseAuthConnector(this)
        val userProfileRepository = FirestoreUserProfileRepository()
        val submissionRepository = FirestoreSubmissionRepository()

        setContent {
            SafeClipTheme {
                var state by remember { mutableStateOf(VideoListState()) }
                var screen by remember { mutableStateOf<SafeClipScreen>(SafeClipScreen.first()) }
                var submissionRecords by remember { mutableStateOf<List<LocalSubmissionRecord>>(emptyList()) }
                var authMessage by remember { mutableStateOf<String?>(null) }
                var syncedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
                var folderManagerMessage by remember { mutableStateOf<String?>(null) }
                var folderManagerFiles by remember { mutableStateOf<List<ManagedFolderFile>>(emptyList()) }
                var currentFolderFiles by remember { mutableStateOf<List<ManagedFolderFile>>(emptyList()) }
                var eventFolderFiles by remember { mutableStateOf<List<ManagedFolderFile>>(emptyList()) }
                var folderManagerRefreshing by remember { mutableStateOf(false) }
                var hasCameraPermission by remember { mutableStateOf(hasPermission(Manifest.permission.CAMERA)) }
                var showExitConfirmDialog by remember { mutableStateOf(false) }
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

                fun hasEventFolderPermission(): Boolean {
                    return folderPermissionSnapshot().canRestore(eventFolderStore.load()?.toString())
                }

                fun savedFolderRestorePlan(savedUriString: String?): SavedFolderRestorePlan {
                    return SavedFolderRestorePlanner.plan(
                        savedUriString = savedUriString,
                        permissionSnapshot = folderPermissionSnapshot()
                    )
                }

                fun refreshRuntimePermissions() {
                    hasCameraPermission = hasPermission(Manifest.permission.CAMERA)
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
                        if (safeClipEventFolder.loadOrCreate() == null) {
                            null
                        } else {
                            savedMediaRepository.listSavedItems()
                        }
                    }
                    if (files == null) {
                        eventFolderFiles = emptyList()
                        state = state.copy(
                            savedMediaItemCount = 0,
                            isLoading = false,
                            errorMessage = "이벤트 폴더를 확인하지 못했습니다. 폴더를 다시 선택해주세요."
                        )
                        return
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

                suspend fun loadCurrentUserProfile() {
                    val currentUser = firebaseAuthConnector.currentSignedInUser() ?: return
                    syncSignedInUser(currentUser)
                    when (val result = userProfileRepository.load(currentUser.uid)) {
                        is UserProfileSyncResult.Success -> {
                            syncedUserProfile = result.profile
                        }
                        is UserProfileSyncResult.Failed -> {
                            authMessage = result.message
                        }
                    }
                }

                fun loadCurrentUserProfileAsync() {
                    scope.launch {
                        loadCurrentUserProfile()
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

                BackHandler(enabled = screen == SafeClipScreen.Start) {
                    showExitConfirmDialog = true
                }

                BackHandler(
                    enabled = screen != SafeClipScreen.Start && SafeClipBackNavigation.previousScreen(screen) != null
                ) {
                    goBack()
                }

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasCameraPermission = granted
                    refreshRuntimePermissions()
                }

                val eventFolderPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    if (uri == null) {
                        state = state.copy(errorMessage = "이벤트 폴더 연결이 취소되었습니다.")
                        screen = SafeClipScreen.Home
                        return@rememberLauncherForActivityResult
                    }

                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    eventFolderStore.save(uri)
                    state = state.copy(errorMessage = null)
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            safeClipEventFolder.ensureIn(uri)
                        }
                        loadStartupDataBeforeHome()
                        screen = SafeClipScreen.Home
                    }
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

                    screen = SafeClipScreen.VideoBrowser
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
                            safeClipEventFolder.loadOrCreate()
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
                    AlertDialog(
                        onDismissRequest = { showExitConfirmDialog = false },
                        title = { Text("앱을 종료하시겠습니까?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showExitConfirmDialog = false
                                    finish()
                                }
                            ) {
                                Text("예")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitConfirmDialog = false }) {
                                Text("아니오")
                            }
                        }
                    )
                }

                when (val currentScreen = screen) {
                    SafeClipScreen.Boot -> BootLoadingScreen()

                    SafeClipScreen.Start -> StartScreen(
                        guestId = guestId,
                        linkedDisplayName = syncedUserProfile?.displayName,
                        linkedEmail = syncedUserProfile?.email ?: firebaseAuthConnector.currentUserEmail(),
                        authMessage = authMessage,
                        onGoogleSignUp = {
                            scope.launch {
                                syncSignedInUser(firebaseAuthConnector.signInWithGoogle())
                                refreshSubmissionRecordsAsync()
                            }
                        },
                        onEmailSignUp = { email, password ->
                            scope.launch {
                                syncSignedInUser(firebaseAuthConnector.signUpWithEmail(email, password))
                                refreshSubmissionRecordsAsync()
                            }
                        },
                        onStart = {
                            screen = SafeClipScreen.Connecting
                            refreshSubmissionRecordsAsync()
                            if (!hasEventFolderPermission()) {
                                eventFolderPicker.launch(null)
                            } else {
                                scope.launch {
                                    loadStartupDataBeforeHome()
                                    screen = SafeClipScreen.Home
                                }
                            }
                        }
                    )

                    SafeClipScreen.Connecting -> ConnectingScreen()

                    SafeClipScreen.Home -> MainHomeScreen(
                        state = state,
                        eventFolderFiles = eventFolderFiles,
                        currentFolderFiles = currentFolderFilesOrFallback(),
                        folderPermissionGranted = hasFolderPermission(),
                        cameraPermissionGranted = hasCameraPermission,
                        submissionCount = submissionRecords.size,
                        onLoadVideos = { folderPicker.launch(null) },
                        onRequestCameraPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onOpenRecentEvents = {
                            if (hasFolderPermission() && hasCameraPermission) {
                                screen = SafeClipScreen.VideoBrowser
                            } else {
                                state = state.copy(errorMessage = "폴더 권한과 카메라 권한을 먼저 켜주세요.")
                            }
                        },
                        onOpenFolder = {
                            if (it == FolderViewKind.SafeClipSaved && !hasEventFolderPermission()) {
                                eventFolderPicker.launch(null)
                            } else if (it == FolderViewKind.SafeClipSaved && !(hasFolderPermission() && hasCameraPermission)) {
                                state = state.copy(errorMessage = "폴더 권한과 카메라 권한을 먼저 켜주세요.")
                            } else {
                                openFolderManager(it)
                            }
                        },
                        onOpenStatus = {
                            refreshSubmissionRecordsAsync()
                            screen = SafeClipScreen.SubmissionStatus
                        },
                        onOpenSettings = {
                            screen = SafeClipScreen.Settings
                            loadCurrentUserProfileAsync()
                        },
                        onBack = ::goBack
                    )

                    SafeClipScreen.Settings -> SettingsScreen(
                        guestId = guestId,
                        linkedEmail = syncedUserProfile?.email ?: firebaseAuthConnector.currentUserEmail(),
                        linkedDisplayName = syncedUserProfile?.displayName,
                        linkedProvider = syncedUserProfile?.provider,
                        message = authMessage,
                        onBack = ::goBack,
                        onSignOut = {
                            authMessage = firebaseAuthConnector.signOut()
                            syncedUserProfile = null
                            refreshSubmissionRecordsAsync()
                        },
                        onDeleteAccount = {
                            scope.launch {
                                authMessage = when (val reauth = firebaseAuthConnector.reauthenticateCurrentUser()) {
                                    is AuthConnectionResult.Failed -> reauth.message
                                    is AuthConnectionResult.NeedsFirebaseSetup -> reauth.message
                                    is AuthConnectionResult.SignedIn -> {
                                        when (val firestoreDelete = userProfileRepository.delete(reauth.uid)) {
                                            is UserProfileSyncResult.Failed -> firestoreDelete.message
                                            is UserProfileSyncResult.Success -> {
                                                when (val authDelete = firebaseAuthConnector.deleteCurrentUser()) {
                                                    is AuthConnectionResult.SignedIn -> {
                                                        syncedUserProfile = null
                                                        "${authDelete.email ?: "계정"} 회원탈퇴가 완료되었습니다. ${firestoreDelete.message}"
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
                                    "삭제하지 못했습니다. 현재 폴더를 다시 선택해 쓰기 권한을 허용해주세요."
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
                            if (video != null) {
                                screen = SafeClipScreen.SubmissionForm(video, null)
                            } else {
                                folderManagerMessage = "영상 또는 JPG 파일만 제출할 수 있습니다."
                            }
                        }
                    )

                    SafeClipScreen.VideoBrowser -> VideoBrowserScreen(
                        state = state,
                        files = currentFolderFilesOrFallback(),
                        onPlayVideo = { file ->
                            val video = ManagedFolderVideoCandidate.from(
                                file = file,
                                folderPath = state.selectedFolderName ?: "현재 폴더"
                            )
                            if (video != null) {
                                screen = SafeClipScreen.VideoPreview(video)
                            }
                        },
                        onPreviewImage = { file ->
                            screen = SafeClipScreen.ImagePreview(file, FolderViewKind.CurrentFolder)
                        },
                        onSubmitFile = { file ->
                            val video = ManagedFolderVideoCandidate.fromSubmittableFile(
                                file = file,
                                folderPath = state.selectedFolderName ?: "현재 폴더"
                            )
                            if (video != null) {
                                screen = SafeClipScreen.SubmissionForm(video, null)
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
                        onSubmit = { selected, clip ->
                            screen = SafeClipScreen.SubmissionForm(selected, clip)
                        }
                    )

                    is SafeClipScreen.ImagePreview -> ImagePreviewScreen(
                        file = currentScreen.file,
                        onBack = ::goBack
                    )

                    is SafeClipScreen.SubmissionForm -> SubmissionFormScreen(
                        video = currentScreen.video,
                        clip = currentScreen.clip,
                        onBack = ::goBack,
                        onSubmit = { draft ->
                            scope.launch {
                                val ownerUid = firebaseAuthConnector.currentUserUid()
                                val input = SubmissionInput(
                                    ownerUid = ownerUid,
                                    video = currentScreen.video,
                                    draft = draft,
                                    guestId = if (ownerUid == null) guestId else syncedUserProfile?.guestId,
                                    ownerDisplayName = syncedUserProfile?.displayName,
                                    ownerEmail = syncedUserProfile?.email ?: firebaseAuthConnector.currentUserEmail()
                                )
                                when (val result = submissionRepository.add(input)) {
                                    is SubmissionSaveResult.Success -> {
                                        Toast.makeText(
                                            this@MainActivity,
                                            result.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        refreshSubmissionRecordsAsync()
                                        screen = SafeClipScreen.SubmissionStatus
                                    }

                                    is SubmissionSaveResult.Failed -> {
                                        Toast.makeText(
                                            this@MainActivity,
                                            result.message,
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
                            screen = SubmittedFilePreviewRoute.from(record)
                        }
                    )
                }
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val BOOT_MIN_VISIBLE_MS = 650L
        private const val BOOT_MAX_DURATION_MS = 1_500L
    }
}
