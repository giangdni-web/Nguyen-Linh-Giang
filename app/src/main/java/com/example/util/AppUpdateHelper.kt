package com.example.util

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private val Context.appUpdateDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_update_prefs")

sealed interface AppUpdateStatus {
    data object Idle : AppUpdateStatus
    data object Checking : AppUpdateStatus
    data object UpToDate : AppUpdateStatus
    data class UpdateAvailable(
        val availableVersionCode: Int,
        val updatePriority: Int,
        val isImmediateAllowed: Boolean,
        val isFlexibleAllowed: Boolean
    ) : AppUpdateStatus
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : AppUpdateStatus
    data object DownloadedReadyToInstall : AppUpdateStatus
    data object Installing : AppUpdateStatus
    data class CancelledOrDeferred(val message: String) : AppUpdateStatus
    data class UnavailableOnDevice(val message: String) : AppUpdateStatus
    data class Error(val message: String) : AppUpdateStatus
}

data class AppUpdateUiState(
    val versionName: String = BuildConfig.VERSION_NAME,
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val autoUpdateEnabled: Boolean = true,
    val status: AppUpdateStatus = AppUpdateStatus.Idle,
    val lastCheckedMillis: Long = 0L
)

class AppUpdatePreferences(private val context: Context) {
    val autoUpdateEnabled: Flow<Boolean> = context.appUpdateDataStore.data
        .map { prefs -> prefs[KEY_AUTO_UPDATE] ?: true }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        context.appUpdateDataStore.edit { prefs ->
            prefs[KEY_AUTO_UPDATE] = enabled
        }
    }

    private companion object {
        val KEY_AUTO_UPDATE = booleanPreferencesKey("auto_update_enabled")
    }
}

class AppUpdateCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(appContext)
    }

    private var cachedUpdateInfo: AppUpdateInfo? = null
    private var isListenerRegistered = false

    private val _status = MutableStateFlow<AppUpdateStatus>(AppUpdateStatus.Idle)
    val status: StateFlow<AppUpdateStatus> = _status.asStateFlow()

    private val _lastCheckedMillis = MutableStateFlow(0L)
    val lastCheckedMillis: StateFlow<Long> = _lastCheckedMillis.asStateFlow()

    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                _status.value = AppUpdateStatus.Downloading(
                    bytesDownloaded = state.bytesDownloaded(),
                    totalBytes = state.totalBytesToDownload()
                )
            }
            InstallStatus.DOWNLOADED -> {
                _status.value = AppUpdateStatus.DownloadedReadyToInstall
            }
            InstallStatus.INSTALLING -> {
                _status.value = AppUpdateStatus.Installing
            }
            InstallStatus.INSTALLED -> {
                _status.value = AppUpdateStatus.UpToDate
            }
            InstallStatus.CANCELED -> {
                _status.value = AppUpdateStatus.CancelledOrDeferred(
                    "Bạn đã tạm hoãn cập nhật. Có thể kiểm tra lại bất cứ lúc nào."
                )
            }
            InstallStatus.FAILED -> {
                _status.value = AppUpdateStatus.Error(
                    "Quá trình tải bản cập nhật gặp sự cố (mã ${state.installErrorCode()})."
                )
            }
            else -> Unit
        }
    }

    fun registerListener() {
        if (!isListenerRegistered) {
            runCatching {
                appUpdateManager.registerListener(installListener)
                isListenerRegistered = true
            }.onFailure { e ->
                Log.w(TAG, "Unable to register Play update listener", e)
            }
        }
    }

    fun unregisterListener() {
        if (isListenerRegistered) {
            runCatching {
                appUpdateManager.unregisterListener(installListener)
            }
            isListenerRegistered = false
        }
    }

    fun checkForUpdate(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        automatic: Boolean
    ) {
        if (!automatic) {
            _status.value = AppUpdateStatus.Checking
        }

        runCatching {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    cachedUpdateInfo = info
                    _lastCheckedMillis.value = System.currentTimeMillis()

                    when {
                        info.installStatus() == InstallStatus.DOWNLOADED -> {
                            _status.value = AppUpdateStatus.DownloadedReadyToInstall
                        }
                        info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                            if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                                startUpdateFlow(info, AppUpdateType.IMMEDIATE, launcher)
                            }
                        }
                        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                            val priority = info.updatePriority()
                            val immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                            val flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                            _status.value = AppUpdateStatus.UpdateAvailable(
                                availableVersionCode = info.availableVersionCode(),
                                updatePriority = priority,
                                isImmediateAllowed = immediateAllowed,
                                isFlexibleAllowed = flexibleAllowed
                            )

                            if (automatic) {
                                val chosenType = when {
                                    priority >= PRIORITY_HIGH_IMMEDIATE && immediateAllowed -> AppUpdateType.IMMEDIATE
                                    flexibleAllowed -> AppUpdateType.FLEXIBLE
                                    immediateAllowed -> AppUpdateType.IMMEDIATE
                                    else -> null
                                }
                                if (chosenType != null) {
                                    startUpdateFlow(info, chosenType, launcher)
                                }
                            }
                        }
                        else -> {
                            _status.value = AppUpdateStatus.UpToDate
                        }
                    }
                }
                .addOnFailureListener { e ->
                    _lastCheckedMillis.value = System.currentTimeMillis()
                    handleCheckFailure(e, automatic)
                }
        }.onFailure { e ->
            handleCheckFailure(e, automatic)
        }
    }

    fun triggerAvailableUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = cachedUpdateInfo
        if (info == null) {
            checkForUpdate(launcher, automatic = false)
            return
        }
        val chosenType = when {
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            else -> null
        }
        if (chosenType != null) {
            startUpdateFlow(info, chosenType, launcher)
        } else {
            _status.value = AppUpdateStatus.Error("Không thể khởi chạy trình cập nhật trên thiết bị này.")
        }
    }

    fun completeFlexibleUpdate() {
        runCatching {
            appUpdateManager.completeUpdate()
        }.onFailure { e ->
            Log.e(TAG, "completeUpdate failed", e)
            _status.value = AppUpdateStatus.Error(
                e.localizedMessage ?: "Không thể hoàn tất cài đặt bản cập nhật."
            )
        }
    }

    fun onUpdateFlowResult(resultCode: Int, okCode: Int, cancelledCode: Int) {
        when (resultCode) {
            okCode -> {
                if (_status.value is AppUpdateStatus.UpdateAvailable) {
                    _status.update { AppUpdateStatus.Downloading(0L, 0L) }
                }
            }
            cancelledCode -> {
                _status.value = AppUpdateStatus.CancelledOrDeferred(
                    "Đã hoãn cập nhật phiên bản mới. Bạn có thể cập nhật lại trong mục Gói & Hồ sơ."
                )
            }
            else -> {
                _status.value = AppUpdateStatus.Error("Cập nhật chưa hoàn tất (mã $resultCode).")
            }
        }
    }

    private fun startUpdateFlow(
        info: AppUpdateInfo,
        updateType: Int,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        runCatching {
            appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(updateType).build()
            )
        }.onFailure { e ->
            Log.e(TAG, "startUpdateFlowForResult failed", e)
            _status.value = AppUpdateStatus.Error(
                e.localizedMessage ?: "Không thể mở hộp thoại cập nhật Google Play."
            )
        }
    }

    private fun handleCheckFailure(e: Throwable, automatic: Boolean) {
        val installException = e as? InstallException
        val unavailableCodes = setOf(
            InstallErrorCode.ERROR_APP_NOT_OWNED,
            InstallErrorCode.ERROR_API_NOT_AVAILABLE,
            InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND
        )
        if (installException != null && installException.errorCode in unavailableCodes) {
            Log.i(TAG, "Play In-App Updates unavailable on sideloaded/emulator environment: ${installException.errorCode}")
            _status.value = if (automatic) {
                AppUpdateStatus.Idle
            } else {
                AppUpdateStatus.UnavailableOnDevice(
                    "Bạn đang dùng phiên bản v${BuildConfig.VERSION_NAME}. Tính năng tự động cập nhật sẽ kích hoạt khi ứng dụng được cài đặt từ Google Play."
                )
            }
        } else {
            Log.w(TAG, "Failed to check for app updates", e)
            _status.value = if (automatic) {
                AppUpdateStatus.Idle
            } else {
                AppUpdateStatus.Error(
                    e.localizedMessage ?: "Không thể kiểm tra cập nhật lúc này. Vui lòng thử lại sau."
                )
            }
        }
    }

    private companion object {
        const val TAG = "AppUpdate"
        const val PRIORITY_HIGH_IMMEDIATE = 4
    }
}
