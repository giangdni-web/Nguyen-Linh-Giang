package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.ConsultationRepository
import com.example.data.repository.LegalAiRepository
import com.example.data.repository.LegalSyncRepository
import com.example.model.LegalCategory
import com.example.ui.AppEvent
import com.example.ui.DataUiState
import com.example.ui.MainViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ConsultationRoomScreen
import com.example.ui.screens.HomeConsultTab
import com.example.ui.screens.LawLibraryScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.ServicesAndProfileScreen
import com.example.ui.theme.BurnishedGoldBright
import com.example.ui.theme.JudicialNavy
import com.example.ui.theme.MrGiangLawTheme
import com.example.util.AppUpdateCoordinator
import com.example.util.AppUpdatePreferences
import com.example.util.AppUpdateStatus
import com.example.util.AppUpdateUiState
import com.example.util.NotificationHelper
import com.example.util.configureAppCheck
import com.example.util.signOutUser
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        // Step 2: Enable Edge-to-Edge and ensure notification channel exists
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)

        setContent {
            MrGiangLawTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private data class NavTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun AppRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var suppressAutoSignIn by rememberSaveable { mutableStateOf(false) }

    val updatePreferences = remember(context) { AppUpdatePreferences(context) }
    val updateCoordinator = remember(context) { AppUpdateCoordinator(context) }
    val autoUpdateEnabled by updatePreferences.autoUpdateEnabled.collectAsStateWithLifecycle(initialValue = true)
    val updateStatus by updateCoordinator.status.collectAsStateWithLifecycle()
    val lastCheckedMillis by updateCoordinator.lastCheckedMillis.collectAsStateWithLifecycle()

    val updateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        updateCoordinator.onUpdateFlowResult(
            resultCode = result.resultCode,
            okCode = Activity.RESULT_OK,
            cancelledCode = Activity.RESULT_CANCELED
        )
    }

    DisposableEffect(updateCoordinator) {
        updateCoordinator.registerListener()
        onDispose {
            updateCoordinator.unregisterListener()
        }
    }

    LaunchedEffect(autoUpdateEnabled) {
        if (autoUpdateEnabled) {
            updateCoordinator.checkForUpdate(launcher = updateLauncher, automatic = true)
        }
    }

    DisposableEffect(lifecycleOwner, autoUpdateEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && autoUpdateEnabled) {
                updateCoordinator.checkForUpdate(launcher = updateLauncher, automatic = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val appUpdateUiState = remember(autoUpdateEnabled, updateStatus, lastCheckedMillis) {
        AppUpdateUiState(
            autoUpdateEnabled = autoUpdateEnabled,
            status = updateStatus,
            lastCheckedMillis = lastCheckedMillis
        )
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            autoSignInEnabled = !suppressAutoSignIn,
            onAuthSuccess = { suppressAutoSignIn = false }
        )
    } else {
        key(user.uid) {
            MainAuthenticatedShell(
                currentUserId = user.uid,
                displayName = user.displayName ?: "Khách hàng",
                email = user.email ?: "",
                appUpdateState = appUpdateUiState,
                onToggleAutoUpdate = { enabled ->
                    coroutineScope.launch {
                        updatePreferences.setAutoUpdateEnabled(enabled)
                        val msg = if (enabled) {
                            "Đã bật tự động cập nhật phiên bản mới"
                        } else {
                            "Đã tắt tự động kiểm tra cập nhật"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        if (enabled) {
                            updateCoordinator.checkForUpdate(launcher = updateLauncher, automatic = true)
                        }
                    }
                },
                onCheckForUpdate = {
                    if (updateStatus is AppUpdateStatus.UpdateAvailable) {
                        updateCoordinator.triggerAvailableUpdate(updateLauncher)
                    } else {
                        updateCoordinator.checkForUpdate(launcher = updateLauncher, automatic = false)
                    }
                },
                onInstallUpdate = {
                    updateCoordinator.completeFlexibleUpdate()
                },
                onSignedOut = {
                    suppressAutoSignIn = true
                }
            )
        }
    }
}

@Composable
private fun MainAuthenticatedShell(
    currentUserId: String,
    displayName: String,
    email: String,
    appUpdateState: AppUpdateUiState,
    onToggleAutoUpdate: (Boolean) -> Unit,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    repository = ConsultationRepository(db),
                    aiRepository = LegalAiRepository(),
                    legalSyncRepository = LegalSyncRepository(app.applicationContext),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email
                )
            }
        }
    )

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val consultationsState by viewModel.consultationsState.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val selectedSessionId by viewModel.selectedSessionId.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val remindersState by viewModel.remindersState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val legalSyncState by viewModel.legalSyncState.collectAsStateWithLifecycle()
    val pendingSession by viewModel.pendingSession.collectAsStateWithLifecycle()
    val aiSpeedMode by viewModel.aiSpeedMode.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AppEvent.NewAdvisorReply -> {
                    NotificationHelper.showNotification(
                        context = context,
                        notificationId = event.sessionTitle.hashCode(),
                        title = "Mr.Giang Law có phản hồi mới",
                        body = "${event.sessionTitle}: ${event.preview}"
                    )
                }

                is AppEvent.LegalDatabaseUpdated -> {
                    NotificationHelper.showNotification(
                        context = context,
                        notificationId = 9001,
                        title = "Cập nhật dữ liệu pháp luật mới",
                        body = "Đã đồng bộ ${event.newItemsCount} văn bản/tin mới từ ${event.reachablePortals} cổng thông tin chính thống."
                    )
                }

                is AppEvent.ScheduleReminderNotification -> {
                    NotificationHelper.scheduleReminderAlarm(
                        context = context,
                        reminderId = event.reminderId,
                        title = event.title,
                        categoryLabel = event.categoryLabel,
                        triggerAtMillis = event.dueDateMillis
                    )
                }

                is AppEvent.CancelReminderNotification -> {
                    NotificationHelper.cancelReminderAlarm(context, event.reminderId)
                }

                is AppEvent.ToastMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            modifier = Modifier.testTag("sign_out_confirm_dialog"),
            title = { Text("Đăng xuất khỏi ứng dụng?") },
            text = {
                Text(
                    "Lịch sử tư vấn, văn bản đã lưu và lịch nhắc của bạn vẫn được bảo mật trên tài khoản ${email.ifBlank { displayName }}. Bạn có chắc chắn muốn đăng xuất?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirmDialog = false
                        onSignedOut()
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = coroutineScope,
                            onSignOutComplete = {
                                Toast.makeText(context, "Đã đăng xuất an toàn", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.testTag("confirm_sign_out_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text("Ở lại")
                }
            }
        )
    }

    val activeSession = remember(selectedSessionId, consultationsState, pendingSession) {
        val list = (consultationsState as? DataUiState.Success)?.data ?: emptyList()
        list.firstOrNull { it.id == selectedSessionId }
            ?: pendingSession?.takeIf { it.id == selectedSessionId }
    }

    if (selectedSessionId != null && activeSession != null) {
        ConsultationRoomScreen(
            session = activeSession,
            messagesState = activeMessagesState,
            aiState = aiState,
            aiSpeedMode = aiSpeedMode,
            onSelectAiSpeedMode = { mode -> viewModel.setAiSpeedMode(mode) },
            onBack = { viewModel.selectSession(null) },
            onSendQuestion = { question, currentMsgs ->
                viewModel.sendQuestionInSession(
                    sessionId = activeSession.id,
                    category = LegalCategory.fromKey(activeSession.category),
                    question = question,
                    currentMessages = currentMsgs,
                    sessionTitle = activeSession.title
                )
            },
            onRetryQuestion = { viewModel.retryLastQuestion(activeSession.id) },
            onMarkResolved = { viewModel.markConsultationResolved(activeSession.id) }
        )
        return
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val navItems = remember {
        listOf(
            NavTabItem("Tư vấn", Icons.Filled.Gavel, Icons.Outlined.Gavel, "nav_consult"),
            NavTabItem("Văn bản", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_library"),
            NavTabItem("Nhắc lịch", Icons.Filled.Alarm, Icons.Outlined.Alarm, "nav_reminders"),
            NavTabItem("Gói & Hồ sơ", Icons.Filled.VerifiedUser, Icons.Outlined.VerifiedUser, "nav_services")
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        navItems.forEachIndexed { index, item ->
                            NavigationRailItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    InAppUpdateBanner(
                        status = appUpdateState.status,
                        onTriggerUpdate = onCheckForUpdate,
                        onInstallUpdate = onInstallUpdate
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (selectedTab) {
                            0 -> HomeConsultTab(
                                displayName = userProfile?.displayName ?: displayName,
                                consultationsState = consultationsState,
                                onStartNewConsultation = { category, question ->
                                    if (question.isBlank()) {
                                        viewModel.openOrCreateConsultation(
                                            category = category,
                                            initialTitle = "Tư vấn ${category.shortName}",
                                            onSessionReady = {}
                                        )
                                    } else {
                                        viewModel.startNewConsultationAndAsk(
                                            category = category,
                                            question = question,
                                            onSessionCreated = {}
                                        )
                                    }
                                },
                                onOpenSession = { sessionId -> viewModel.selectSession(sessionId) },
                                onDeleteSession = { sessionId -> viewModel.deleteConsultation(sessionId) },
                                onRequestSignOut = { showSignOutConfirmDialog = true }
                            )

                            1 -> LawLibraryScreen(
                                bookmarksState = bookmarksState,
                                legalSyncState = legalSyncState,
                                onSyncLegalPortals = { viewModel.syncLegalPortalsNow(isManual = true) },
                                onToggleAutoSync = { enabled -> viewModel.setAutoSyncEnabled(enabled) },
                                onToggleBookmark = { doc, article, existing ->
                                    viewModel.toggleBookmark(
                                        documentId = doc.id,
                                        articleCode = article.articleNumber,
                                        title = article.heading,
                                        category = doc.category,
                                        existingBookmark = existing
                                    )
                                },
                                onAskAboutArticle = { category, prompt ->
                                    viewModel.startNewConsultationAndAsk(
                                        category = category,
                                        question = prompt,
                                        onSessionCreated = {}
                                    )
                                }
                            )

                            2 -> RemindersScreen(
                                remindersState = remindersState,
                                onCreateReminder = { title, cat, dueMillis, note, notify ->
                                    viewModel.createReminder(title, cat, dueMillis, note, notify)
                                },
                                onToggleCompleted = { reminder ->
                                    viewModel.toggleReminderCompleted(reminder)
                                },
                                onDeleteReminder = { reminderId ->
                                    viewModel.deleteReminder(reminderId)
                                }
                            )

                            3 -> ServicesAndProfileScreen(
                                userProfile = userProfile,
                                fallbackEmail = email,
                                fallbackName = displayName,
                                ordersState = ordersState,
                                onCreateOrder = { pkg, method, phone, summary, onCreated ->
                                    viewModel.createConsultationOrder(pkg, method, phone, summary, onCreated)
                                },
                                onConfirmTransferSubmitted = { orderId ->
                                    viewModel.confirmTransferSubmitted(orderId)
                                },
                                onUpdateProfile = { name, phone, notify ->
                                    viewModel.updateProfileSettings(name, phone, notify)
                                },
                                onSignOut = { showSignOutConfirmDialog = true },
                                appUpdateState = appUpdateState,
                                onToggleAutoUpdate = onToggleAutoUpdate,
                                onCheckForUpdate = onCheckForUpdate,
                                onInstallUpdate = onInstallUpdate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InAppUpdateBanner(
    status: AppUpdateStatus,
    onTriggerUpdate: () -> Unit,
    onInstallUpdate: () -> Unit
) {
    when (status) {
        is AppUpdateStatus.Downloading -> {
            Surface(
                color = JudicialNavy,
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = BurnishedGoldBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đang tải phiên bản ứng dụng mới...",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (status.totalBytes > 0L) {
                        val progress = (status.bytesDownloaded.toFloat() / status.totalBytes.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = BurnishedGoldBright
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = BurnishedGoldBright
                        )
                    }
                }
            }
        }

        AppUpdateStatus.DownloadedReadyToInstall -> {
            Surface(
                color = JudicialNavy,
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bản cập nhật mới đã sẵn sàng",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onInstallUpdate) {
                        Text("Cài đặt ngay")
                    }
                }
            }
        }

        is AppUpdateStatus.UpdateAvailable -> {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Đã có phiên bản Mr.Giang Law mới",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onTriggerUpdate) {
                        Text("Cập nhật")
                    }
                }
            }
        }

        else -> Unit
    }
}
