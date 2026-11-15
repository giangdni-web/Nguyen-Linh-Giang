package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AiResponseSpeed
import com.example.data.repository.ConsultationRepository
import com.example.data.repository.LegalAiRepository
import com.example.data.repository.LegalSyncRepository
import com.example.model.ConsultationMessage
import com.example.model.ConsultationOrder
import com.example.model.ConsultationPackage
import com.example.model.ConsultationSession
import com.example.model.LawBookmark
import com.example.model.LegalCategory
import com.example.model.LegalReminder
import com.example.model.LegalSyncUiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface AiAnswerState {
    data object Idle : AiAnswerState
    data object Preparing : AiAnswerState
    data class Streaming(val partialText: String) : AiAnswerState
    data class Error(val message: String) : AiAnswerState
}

data class PendingUserMessage(
    val sessionId: String,
    val message: ConsultationMessage,
    val minMessageCountToHide: Int
)

private data class LastConsultationTurn(
    val sessionId: String,
    val category: LegalCategory,
    val question: String,
    val historyBeforeQuestion: List<ConsultationMessage>,
    val sessionTitle: String,
    val isFirstTurn: Boolean,
    val userPersisted: Boolean
)

sealed interface AppEvent {
    data class NewAdvisorReply(val sessionTitle: String, val preview: String) : AppEvent
    data class LegalDatabaseUpdated(val newItemsCount: Int, val reachablePortals: Int) : AppEvent
    data class ScheduleReminderNotification(
        val reminderId: String,
        val title: String,
        val categoryLabel: String,
        val dueDateMillis: Long
    ) : AppEvent
    data class CancelReminderNotification(val reminderId: String) : AppEvent
    data class ToastMessage(val message: String) : AppEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: ConsultationRepository,
    private val aiRepository: LegalAiRepository,
    private val legalSyncRepository: LegalSyncRepository,
    private val currentUserId: String,
    initialDisplayName: String,
    initialEmail: String
) : ViewModel() {

    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    private val _selectedSessionId = MutableStateFlow<String?>(null)
    val selectedSessionId: StateFlow<String?> = _selectedSessionId.asStateFlow()

    private val _pendingSession = MutableStateFlow<ConsultationSession?>(null)
    val pendingSession: StateFlow<ConsultationSession?> = _pendingSession.asStateFlow()

    private val _pendingMessages = MutableStateFlow<List<PendingUserMessage>>(emptyList())

    private val _aiState = MutableStateFlow<AiAnswerState>(AiAnswerState.Idle)
    val aiState: StateFlow<AiAnswerState> = _aiState.asStateFlow()

    private val _aiSpeedMode = MutableStateFlow(AiResponseSpeed.FAST)
    val aiSpeedMode: StateFlow<AiResponseSpeed> = _aiSpeedMode.asStateFlow()

    private var lastAttemptedTurn: LastConsultationTurn? = null

    private val _isSyncingPortals = MutableStateFlow(false)
    private val _portalSyncError = MutableStateFlow<String?>(null)

    val legalSyncState: StateFlow<LegalSyncUiState> = combine(
        legalSyncRepository.observeSnapshot(),
        _isSyncingPortals,
        _portalSyncError
    ) { snapshot, isSyncing, errorMsg ->
        LegalSyncUiState(
            autoSyncEnabled = snapshot.autoSyncEnabled,
            isSyncing = isSyncing,
            lastSyncMillis = snapshot.lastSyncMillis,
            portalStatuses = snapshot.statuses,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = LegalSyncUiState()
    )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapAndLog(error, "profile")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = null
        )

    val consultationsState: StateFlow<DataUiState<List<ConsultationSession>>> =
        repository.observeConsultations(currentUserId)
            .map<List<ConsultationSession>, DataUiState<List<ConsultationSession>>> { DataUiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "consultations")
                emit(DataUiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DataUiState.Loading
            )

    val activeMessagesState: StateFlow<DataUiState<List<ConsultationMessage>>> =
        combine(_selectedSessionId, _pendingMessages) { sessionId, pendingList ->
            sessionId to pendingList.filter { it.sessionId == sessionId }
        }.flatMapLatest { (sessionId, sessionPending) ->
            if (sessionId.isNullOrBlank()) {
                flowOf(DataUiState.Success(emptyList()))
            } else {
                repository.observeMessages(currentUserId, sessionId)
                    .map<List<ConsultationMessage>, DataUiState<List<ConsultationMessage>>> { persisted ->
                        val remainingPending = sessionPending.filter { pending ->
                            persisted.size < pending.minMessageCountToHide &&
                                persisted.none {
                                    it.sender == pending.message.sender &&
                                        it.content.trim() == pending.message.content.trim()
                                }
                        }.map { it.message }
                        DataUiState.Success(persisted + remainingPending)
                    }
                    .catch { error ->
                        if (sessionPending.isNotEmpty()) {
                            emit(DataUiState.Success(sessionPending.map { it.message }))
                        } else {
                            val msg = unwrapAndLog(error, "messages")
                            emit(DataUiState.Error(msg))
                        }
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = DataUiState.Success(emptyList())
        )

    val bookmarksState: StateFlow<DataUiState<List<LawBookmark>>> =
        repository.observeBookmarks(currentUserId)
            .map<List<LawBookmark>, DataUiState<List<LawBookmark>>> { DataUiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "bookmarks")
                emit(DataUiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DataUiState.Loading
            )

    val remindersState: StateFlow<DataUiState<List<LegalReminder>>> =
        repository.observeReminders(currentUserId)
            .map<List<LegalReminder>, DataUiState<List<LegalReminder>>> { DataUiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "reminders")
                emit(DataUiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DataUiState.Loading
            )

    val ordersState: StateFlow<DataUiState<List<ConsultationOrder>>> =
        repository.observeOrders(currentUserId)
            .map<List<ConsultationOrder>, DataUiState<List<ConsultationOrder>>> { DataUiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "orders")
                emit(DataUiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DataUiState.Loading
            )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(initialDisplayName, initialEmail)
                .onFailure { e ->
                    Log.w(TAG, "Could not ensure user profile", e)
                }
        }
        viewModelScope.launch {
            val initialSnapshot = legalSyncRepository.observeSnapshot().first()
            if (initialSnapshot.autoSyncEnabled &&
                (initialSnapshot.lastSyncMillis == 0L ||
                    System.currentTimeMillis() - initialSnapshot.lastSyncMillis > INITIAL_STALE_MS)
            ) {
                syncLegalPortalsNow(isManual = false)
            }
            while (isActive) {
                delay(AUTO_SYNC_INTERVAL_MS)
                if (legalSyncState.value.autoSyncEnabled) {
                    syncLegalPortalsNow(isManual = false)
                }
            }
        }
    }

    fun syncLegalPortalsNow(isManual: Boolean = true) {
        if (_isSyncingPortals.value) return
        viewModelScope.launch {
            _isSyncingPortals.value = true
            _portalSyncError.value = null
            try {
                val existingHeadlines = legalSyncState.value.combinedHeadlines.map { it.title }.toSet()
                val result = legalSyncRepository.syncAllPortals(previousHeadlines = existingHeadlines)
                val reachable = result.statuses.count { it.isReachable }
                if (isManual) {
                    _events.tryEmit(
                        AppEvent.ToastMessage("Đã đồng bộ $reachable/${result.statuses.size} cổng thông tin pháp luật")
                    )
                } else if (result.newlyDetectedCount > 0 && (userProfile.value?.notifyOnReply ?: true)) {
                    _events.tryEmit(
                        AppEvent.LegalDatabaseUpdated(
                            newItemsCount = result.newlyDetectedCount,
                            reachablePortals = reachable
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Không thể đồng bộ dữ liệu pháp luật"
                _portalSyncError.value = msg
                if (isManual) {
                    _events.tryEmit(AppEvent.ToastMessage(msg))
                }
            } finally {
                _isSyncingPortals.value = false
            }
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            legalSyncRepository.setAutoSyncEnabled(enabled)
            if (enabled) {
                _events.tryEmit(AppEvent.ToastMessage("Đã bật tự động cập nhật dữ liệu pháp luật"))
                syncLegalPortalsNow(isManual = false)
            } else {
                _events.tryEmit(AppEvent.ToastMessage("Đã tạm dừng tự động đồng bộ"))
            }
        }
    }

    fun setAiSpeedMode(mode: AiResponseSpeed) {
        _aiSpeedMode.value = mode
        _events.tryEmit(
            AppEvent.ToastMessage(
                if (mode == AiResponseSpeed.FAST) {
                    "Đã chọn chế độ phản hồi Siêu tốc"
                } else {
                    "Đã chọn chế độ phân tích Chuyên sâu"
                }
            )
        )
    }

    fun selectSession(sessionId: String?) {
        _selectedSessionId.value = sessionId
        _aiState.value = AiAnswerState.Idle
        if (sessionId == null) {
            _pendingSession.value = null
        }
    }

    fun startNewConsultationAndAsk(
        category: LegalCategory,
        question: String,
        onSessionCreated: (String) -> Unit
    ) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) {
            openOrCreateConsultation(
                category = category,
                initialTitle = "Tư vấn ${category.shortName}",
                onSessionReady = onSessionCreated
            )
            return
        }

        val sessionIdResult = runCatching { repository.generateNewConsultationId() }
        val sessionId = sessionIdResult.getOrNull()
        if (sessionId == null) {
            val errMsg = sessionIdResult.exceptionOrNull()?.localizedMessage ?: "Lỗi xác thực"
            _events.tryEmit(AppEvent.ToastMessage("Không thể tạo phiên tư vấn: $errMsg"))
            return
        }

        val title = trimmed.take(65)
        _pendingSession.value = ConsultationSession(
            id = sessionId,
            userId = currentUserId,
            title = title,
            category = category.key,
            lastMessagePreview = trimmed.take(120),
            messageCount = 1L,
            status = "active"
        )
        addPendingMessage(
            sessionId = sessionId,
            sender = "user",
            content = trimmed,
            minMessageCountToHide = 1
        )
        _selectedSessionId.value = sessionId
        _aiState.value = AiAnswerState.Preparing
        onSessionCreated(sessionId)

        lastAttemptedTurn = LastConsultationTurn(
            sessionId = sessionId,
            category = category,
            question = trimmed,
            historyBeforeQuestion = emptyList(),
            sessionTitle = title,
            isFirstTurn = true,
            userPersisted = false
        )

        viewModelScope.launch {
            val persistFirstTurnDeferred = async {
                repository.createConsultationWithFirstMessage(
                    consultationId = sessionId,
                    title = title,
                    categoryKey = category.key,
                    userQuestion = trimmed
                )
            }

            var finalReply = ""
            try {
                aiRepository.streamLegalAnswer(
                    category = category,
                    history = emptyList(),
                    userQuestion = trimmed,
                    speedMode = _aiSpeedMode.value
                ).collect { partial ->
                    finalReply = partial
                    _aiState.value = AiAnswerState.Streaming(partial)
                }

                if (finalReply.isBlank()) {
                    _aiState.value = AiAnswerState.Error("Không nhận được nội dung phản hồi từ trợ lý.")
                    return@launch
                }

                // Lock advisor reply into UI immediately so it never disappears while persisting
                addPendingMessage(
                    sessionId = sessionId,
                    sender = "advisor",
                    content = finalReply,
                    legalCitations = category.displayName,
                    minMessageCountToHide = 2
                )
                _aiState.value = AiAnswerState.Idle

                val notifyEnabled = userProfile.value?.notifyOnReply ?: true
                if (notifyEnabled) {
                    _events.tryEmit(
                        AppEvent.NewAdvisorReply(
                            sessionTitle = title,
                            preview = finalReply.replace("\n", " ").take(120)
                        )
                    )
                }

                val persistFirst = persistFirstTurnDeferred.await()
                if (persistFirst.isSuccess) {
                    lastAttemptedTurn = lastAttemptedTurn?.copy(userPersisted = true)
                    repository.addMessageToSession(
                        consultationId = sessionId,
                        sender = "advisor",
                        content = finalReply,
                        legalCitations = category.displayName,
                        updatedMessageCount = 2L
                    ).onFailure { err ->
                        Log.w(TAG, "Failed to persist advisor reply", err)
                        _events.tryEmit(
                            AppEvent.ToastMessage("Đã trả lời nhưng chưa lưu lên đám mây: ${err.localizedMessage ?: "Lỗi mạng"}")
                        )
                    }.onSuccess {
                        _pendingSession.value = null
                    }
                } else {
                    val err = persistFirst.exceptionOrNull()
                    Log.w(TAG, "Failed to persist initial session", err)
                    _events.tryEmit(
                        AppEvent.ToastMessage("Đã trả lời nhưng chưa lưu phiên tư vấn: ${err?.localizedMessage ?: "Lỗi mạng"}")
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI consultation error", e)
                val firstPersist = runCatching { persistFirstTurnDeferred.await() }.getOrNull()
                if (firstPersist?.isSuccess == true) {
                    lastAttemptedTurn = lastAttemptedTurn?.copy(userPersisted = true)
                }
                _aiState.value = AiAnswerState.Error(
                    e.localizedMessage ?: "Không thể kết nối trợ lý pháp luật lúc này. Quý khách có thể thử lại hoặc nhắn Zalo 0915855111."
                )
            }
        }
    }

    fun openOrCreateConsultation(
        category: LegalCategory,
        initialTitle: String,
        onSessionReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.createConsultation(initialTitle, category.key)
                .onSuccess { sessionId ->
                    _selectedSessionId.value = sessionId
                    onSessionReady(sessionId)
                }
                .onFailure { e ->
                    _events.tryEmit(
                        AppEvent.ToastMessage("Không thể tạo phiên tư vấn: ${e.localizedMessage ?: "Lỗi kết nối"}")
                    )
                }
        }
    }

    fun sendQuestionInSession(
        sessionId: String,
        category: LegalCategory,
        question: String,
        currentMessages: List<ConsultationMessage>,
        sessionTitle: String
    ) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return

        val nextCountAfterUser = currentMessages.size.toLong() + 1L
        val titleUpdate = if (currentMessages.isEmpty()) trimmed.take(70) else null

        addPendingMessage(
            sessionId = sessionId,
            sender = "user",
            content = trimmed,
            minMessageCountToHide = nextCountAfterUser.toInt()
        )
        _aiState.value = AiAnswerState.Preparing

        lastAttemptedTurn = LastConsultationTurn(
            sessionId = sessionId,
            category = category,
            question = trimmed,
            historyBeforeQuestion = currentMessages,
            sessionTitle = titleUpdate ?: sessionTitle,
            isFirstTurn = false,
            userPersisted = false
        )

        viewModelScope.launch {
            val userWriteDeferred = async {
                repository.addMessageToSession(
                    consultationId = sessionId,
                    sender = "user",
                    content = trimmed,
                    updatedMessageCount = nextCountAfterUser,
                    sessionTitleUpdate = titleUpdate
                )
            }

            var finalReply = ""
            try {
                aiRepository.streamLegalAnswer(
                    category = category,
                    history = currentMessages,
                    userQuestion = trimmed,
                    speedMode = _aiSpeedMode.value
                ).collect { partial ->
                    finalReply = partial
                    _aiState.value = AiAnswerState.Streaming(partial)
                }

                if (finalReply.isBlank()) {
                    _aiState.value = AiAnswerState.Error("Không nhận được nội dung phản hồi từ trợ lý.")
                    return@launch
                }

                // Lock advisor reply into UI immediately so it stays visible even if Firestore save fails/lags
                addPendingMessage(
                    sessionId = sessionId,
                    sender = "advisor",
                    content = finalReply,
                    legalCitations = category.displayName,
                    minMessageCountToHide = (nextCountAfterUser + 1L).toInt()
                )
                _aiState.value = AiAnswerState.Idle

                val notifyEnabled = userProfile.value?.notifyOnReply ?: true
                if (notifyEnabled) {
                    _events.tryEmit(
                        AppEvent.NewAdvisorReply(
                            sessionTitle = titleUpdate ?: sessionTitle,
                            preview = finalReply.replace("\n", " ").take(120)
                        )
                    )
                }

                val userWrite = userWriteDeferred.await()
                if (userWrite.isSuccess) {
                    lastAttemptedTurn = lastAttemptedTurn?.copy(userPersisted = true)
                    repository.addMessageToSession(
                        consultationId = sessionId,
                        sender = "advisor",
                        content = finalReply,
                        legalCitations = category.displayName,
                        updatedMessageCount = nextCountAfterUser + 1L
                    ).onFailure { err ->
                        Log.w(TAG, "Failed to persist advisor reply", err)
                        _events.tryEmit(
                            AppEvent.ToastMessage("Đã trả lời nhưng chưa lưu lên đám mây: ${err.localizedMessage ?: "Lỗi mạng"}")
                        )
                    }
                } else {
                    val err = userWrite.exceptionOrNull()
                    Log.w(TAG, "Failed to persist user question", err)
                    _events.tryEmit(
                        AppEvent.ToastMessage("Đã trả lời nhưng chưa lưu lịch sử: ${err?.localizedMessage ?: "Lỗi mạng"}")
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI consultation error", e)
                val userWrite = runCatching { userWriteDeferred.await() }.getOrNull()
                if (userWrite?.isSuccess == true) {
                    lastAttemptedTurn = lastAttemptedTurn?.copy(userPersisted = true)
                }
                _aiState.value = AiAnswerState.Error(
                    e.localizedMessage ?: "Không thể kết nối trợ lý pháp luật lúc này. Quý khách có thể thử lại hoặc nhắn Zalo 0915855111."
                )
            }
        }
    }

    fun retryLastQuestion(sessionId: String) {
        val turn = lastAttemptedTurn?.takeIf { it.sessionId == sessionId } ?: return
        _aiState.value = AiAnswerState.Preparing

        viewModelScope.launch {
            var finalReply = ""
            try {
                aiRepository.streamLegalAnswer(
                    category = turn.category,
                    history = turn.historyBeforeQuestion,
                    userQuestion = turn.question,
                    speedMode = _aiSpeedMode.value
                ).collect { partial ->
                    finalReply = partial
                    _aiState.value = AiAnswerState.Streaming(partial)
                }

                if (finalReply.isBlank()) {
                    _aiState.value = AiAnswerState.Error("Không nhận được nội dung phản hồi từ trợ lý.")
                    return@launch
                }

                val expectedTotalCount = turn.historyBeforeQuestion.size + 2
                addPendingMessage(
                    sessionId = turn.sessionId,
                    sender = "advisor",
                    content = finalReply,
                    legalCitations = turn.category.displayName,
                    minMessageCountToHide = expectedTotalCount
                )
                _aiState.value = AiAnswerState.Idle

                val userSaved = if (turn.userPersisted) {
                    true
                } else if (turn.isFirstTurn) {
                    repository.createConsultationWithFirstMessage(
                        consultationId = turn.sessionId,
                        title = turn.sessionTitle,
                        categoryKey = turn.category.key,
                        userQuestion = turn.question
                    ).isSuccess
                } else {
                    repository.addMessageToSession(
                        consultationId = turn.sessionId,
                        sender = "user",
                        content = turn.question,
                        updatedMessageCount = (turn.historyBeforeQuestion.size + 1).toLong()
                    ).isSuccess
                }

                if (userSaved) {
                    lastAttemptedTurn = turn.copy(userPersisted = true)
                    repository.addMessageToSession(
                        consultationId = turn.sessionId,
                        sender = "advisor",
                        content = finalReply,
                        legalCitations = turn.category.displayName,
                        updatedMessageCount = expectedTotalCount.toLong()
                    ).onSuccess {
                        _pendingSession.value = null
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Retry AI consultation error", e)
                _aiState.value = AiAnswerState.Error(
                    e.localizedMessage ?: "Không thể kết nối trợ lý pháp luật lúc này. Quý khách có thể nhắn trực tiếp Zalo 0915855111."
                )
            }
        }
    }

    private fun addPendingMessage(
        sessionId: String,
        sender: String,
        content: String,
        legalCitations: String = "",
        minMessageCountToHide: Int
    ) {
        val item = PendingUserMessage(
            sessionId = sessionId,
            message = ConsultationMessage(
                id = "pending_${sender}_${System.nanoTime()}",
                userId = currentUserId,
                sender = sender,
                content = content,
                legalCitations = legalCitations
            ),
            minMessageCountToHide = minMessageCountToHide
        )
        _pendingMessages.update { existing ->
            val filtered = existing.filterNot {
                it.sessionId == sessionId &&
                    it.message.sender == sender &&
                    it.message.content.trim() == content.trim()
            }
            filtered + item
        }
    }

    fun markConsultationResolved(sessionId: String) {
        viewModelScope.launch {
            repository.markConsultationResolved(sessionId)
                .onSuccess {
                    _events.tryEmit(AppEvent.ToastMessage("Đã đánh dấu hoàn tất hồ sơ tư vấn"))
                }
                .onFailure {
                    _events.tryEmit(AppEvent.ToastMessage("Không thể cập nhật trạng thái phiên"))
                }
        }
    }

    fun deleteConsultation(sessionId: String) {
        viewModelScope.launch {
            if (_selectedSessionId.value == sessionId) {
                _selectedSessionId.value = null
            }
            _pendingMessages.update { list -> list.filterNot { it.sessionId == sessionId } }
            repository.deleteConsultation(sessionId)
                .onSuccess {
                    _events.tryEmit(AppEvent.ToastMessage("Đã xóa phiên tư vấn khỏi lịch sử"))
                }
                .onFailure {
                    _events.tryEmit(AppEvent.ToastMessage("Không thể xóa phiên tư vấn"))
                }
        }
    }

    fun toggleBookmark(
        documentId: String,
        articleCode: String,
        title: String,
        category: LegalCategory,
        existingBookmark: LawBookmark?
    ) {
        viewModelScope.launch {
            if (existingBookmark != null) {
                repository.deleteBookmark(existingBookmark.id)
                    .onSuccess { _events.tryEmit(AppEvent.ToastMessage("Đã bỏ lưu điều luật")) }
            } else {
                repository.addBookmark(documentId, articleCode, title, category.key)
                    .onSuccess { _events.tryEmit(AppEvent.ToastMessage("Đã lưu vào mục Tra cứu của tôi")) }
            }
        }
    }

    fun createReminder(
        title: String,
        category: LegalCategory,
        dueDateMillis: Long,
        note: String,
        notifyEnabled: Boolean
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createReminder(title, category.key, dueDateMillis, note, notifyEnabled)
                .onSuccess { reminderId ->
                    if (notifyEnabled) {
                        _events.tryEmit(
                            AppEvent.ScheduleReminderNotification(
                                reminderId = reminderId,
                                title = title.trim(),
                                categoryLabel = category.shortName,
                                dueDateMillis = dueDateMillis
                            )
                        )
                    }
                    _events.tryEmit(AppEvent.ToastMessage("Đã lưu lịch nhắc pháp lý"))
                }
                .onFailure {
                    _events.tryEmit(AppEvent.ToastMessage("Không thể lưu lịch nhắc"))
                }
        }
    }

    fun toggleReminderCompleted(reminder: LegalReminder) {
        viewModelScope.launch {
            repository.toggleReminderCompleted(reminder)
                .onSuccess {
                    if (!reminder.isCompleted) {
                        _events.tryEmit(AppEvent.CancelReminderNotification(reminder.id))
                    }
                }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
                .onSuccess {
                    _events.tryEmit(AppEvent.CancelReminderNotification(reminderId))
                    _events.tryEmit(AppEvent.ToastMessage("Đã xóa lịch nhắc"))
                }
        }
    }

    fun createConsultationOrder(
        pkg: ConsultationPackage,
        paymentMethod: String,
        contactPhone: String,
        caseSummary: String,
        onCreated: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            val transferCode = "MGL${(System.currentTimeMillis() % 1000000).toString().padStart(6, '0')}"
            repository.createOrder(
                packageId = pkg.id,
                packageTitle = pkg.title,
                amountVnd = pkg.priceVnd,
                paymentMethod = paymentMethod,
                transferCode = transferCode,
                contactPhone = contactPhone,
                caseSummary = caseSummary
            ).onSuccess { orderId ->
                _events.tryEmit(AppEvent.ToastMessage("Đã tạo yêu cầu tư vấn mã $transferCode"))
                onCreated(orderId, transferCode)
            }.onFailure {
                _events.tryEmit(AppEvent.ToastMessage("Không thể tạo đơn tư vấn. Vui lòng thử lại."))
            }
        }
    }

    fun confirmTransferSubmitted(orderId: String) {
        viewModelScope.launch {
            repository.markOrderReceiptSubmitted(orderId)
                .onSuccess {
                    _events.tryEmit(AppEvent.ToastMessage("Đã ghi nhận thông báo chuyển khoản. Vui lòng gửi biên lai qua Zalo 0915855111."))
                }
                .onFailure {
                    _events.tryEmit(AppEvent.ToastMessage("Không thể cập nhật trạng thái thanh toán"))
                }
        }
    }

    fun updateProfileSettings(displayName: String, phoneNumber: String, notifyOnReply: Boolean) {
        viewModelScope.launch {
            repository.updateUserProfile(displayName, phoneNumber, notifyOnReply)
                .onSuccess {
                    _events.tryEmit(AppEvent.ToastMessage("Đã cập nhật thông tin bảo mật & thông báo"))
                }
                .onFailure {
                    _events.tryEmit(AppEvent.ToastMessage("Không thể cập nhật hồ sơ"))
                }
        }
    }

    private fun unwrapAndLog(error: Throwable, label: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        return rootError.localizedMessage ?: "Không thể tải dữ liệu $label"
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MS = 5000L
        const val INITIAL_STALE_MS = 5 * 60 * 1000L
        const val AUTO_SYNC_INTERVAL_MS = 10 * 60 * 1000L
    }
}
