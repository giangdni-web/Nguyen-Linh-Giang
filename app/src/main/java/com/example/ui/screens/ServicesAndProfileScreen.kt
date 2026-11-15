package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.ConsultationOrder
import com.example.model.ConsultationPackage
import com.example.model.ConsultationPackageCatalog
import com.example.model.UserProfile
import com.example.ui.DataUiState
import com.example.ui.components.AdvisorCredentialsCard
import com.example.ui.components.FanpagePromoCard
import com.example.ui.components.SecurityTrustBanner
import com.example.ui.theme.AgribankBurgundy
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.EmeraldTrust
import com.example.ui.theme.JudicialNavy
import com.example.ui.theme.ZaloBlue
import com.example.util.AppUpdateStatus
import com.example.util.AppUpdateUiState
import com.example.util.LawyerContact
import com.example.util.copyToClipboard
import com.example.util.openExternalUrl
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ServicesAndProfileScreen(
    userProfile: UserProfile?,
    fallbackEmail: String,
    fallbackName: String,
    ordersState: DataUiState<List<ConsultationOrder>>,
    onCreateOrder: (ConsultationPackage, String, String, String, (String, String) -> Unit) -> Unit,
    onConfirmTransferSubmitted: (String) -> Unit,
    onUpdateProfile: (String, String, Boolean) -> Unit,
    onSignOut: () -> Unit,
    appUpdateState: AppUpdateUiState = AppUpdateUiState(),
    onToggleAutoUpdate: (Boolean) -> Unit = {},
    onCheckForUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPackageForPayment by remember { mutableStateOf<ConsultationPackage?>(null) }
    var activePaymentOrder by remember { mutableStateOf<Pair<ConsultationOrder, ConsultationPackage>?>(null) }

    var editName by remember(userProfile?.displayName) {
        mutableStateOf(userProfile?.displayName?.ifBlank { fallbackName } ?: fallbackName)
    }
    var editPhone by remember(userProfile?.phoneNumber) {
        mutableStateOf(userProfile?.phoneNumber ?: "")
    }
    var notifyOnReply by remember(userProfile?.notifyOnReply) {
        mutableStateOf(userProfile?.notifyOnReply ?: true)
    }

    val orders = (ordersState as? DataUiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 800.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Gói tư vấn & Thanh toán an toàn",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Thanh toán minh bạch qua Agribank VietQR - Chủ tài khoản Nguyễn Linh Giang",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Official Agribank Account Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, AgribankBurgundy.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = null,
                            tint = AgribankBurgundy
                        )
                        Text(
                            text = "Thông tin chuyển khoản chính thức",
                            style = MaterialTheme.typography.titleMedium,
                            color = AgribankBurgundy
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = LawyerContact.BANK_NAME,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "STK: ${LawyerContact.BANK_ACCOUNT_NUMBER}",
                                style = MaterialTheme.typography.titleLarge,
                                color = JudicialNavy,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Chủ TK: ${LawyerContact.BANK_ACCOUNT_HOLDER}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(
                            onClick = {
                                copyToClipboard(
                                    context,
                                    "Số tài khoản Agribank",
                                    LawyerContact.BANK_ACCOUNT_NUMBER
                                )
                            },
                            modifier = Modifier.testTag("copy_bank_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Sao chép số tài khoản"
                            )
                        }
                    }
                }
            }
        }

        // Consultation Packages
        items(ConsultationPackageCatalog.packages, key = { it.id }) { pkg ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (pkg.badge != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = pkg.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(text = pkg.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${pkg.durationLabel} • ${pkg.subtitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pkg.priceFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            color = BurnishedGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    pkg.features.forEach { feat ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldTrust,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(15.dp)
                            )
                            Text(text = feat, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        onClick = { selectedPackageForPayment = pkg },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("book_package_${pkg.id}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đăng ký & Thanh toán trực tuyến")
                    }
                }
            }
        }

        // Order History
        if (orders.isNotEmpty()) {
            item {
                Text(
                    text = "Lịch sử gói tư vấn đã đăng ký",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(orders, key = { it.id }) { order ->
                val vnd = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(order.amountVnd)
                val statusLabel = when (order.status) {
                    "pending_transfer" -> "Chờ chuyển khoản"
                    "submitted_receipt" -> "Đã gửi xác nhận CK"
                    "confirmed" -> "Đã xác nhận"
                    else -> "Đã hủy"
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Mã CK: ${order.transferCode}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(text = order.packageTitle, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Số tiền: $vnd đ • SĐT: ${order.contactPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (order.status == "pending_transfer") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val pkg = ConsultationPackageCatalog.packages
                                            .firstOrNull { it.id == order.packageId }
                                            ?: ConsultationPackageCatalog.packages.first()
                                        activePaymentOrder = order to pkg
                                    }
                                ) {
                                    Text("Xem mã QR Agribank")
                                }
                                Button(onClick = { onConfirmTransferSubmitted(order.id) }) {
                                    Text("Tôi đã chuyển khoản")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Author & Fanpage section
        item {
            Text(
                text = "Thông tin tác giả & Kênh chính thức",
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
            AdvisorCredentialsCard()
        }
        item {
            FanpagePromoCard()
        }

        // User Account & Security Settings
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = EmeraldTrust)
                        Text("Tài khoản & Bảo mật thông tin", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Email: ${userProfile?.email?.ifBlank { fallbackEmail } ?: fallbackEmail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Họ và tên khách hàng") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Số điện thoại / Zalo liên hệ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Thông báo tự động khi có phản hồi mới",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Nhận cảnh báo ngay trên thiết bị khi hệ thống trả lời",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifyOnReply,
                            onCheckedChange = { notifyOnReply = it }
                        )
                    }
                    Button(
                        onClick = { onUpdateProfile(editName, editPhone, notifyOnReply) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Lưu thông tin cá nhân")
                    }
                    SecurityTrustBanner()
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đăng xuất tài khoản")
                    }
                }
            }
        }

        // App Version & Automatic In-App Updates Card
        item {
            AppUpdateSettingsCard(
                appUpdateState = appUpdateState,
                onToggleAutoUpdate = onToggleAutoUpdate,
                onCheckForUpdate = onCheckForUpdate,
                onInstallUpdate = onInstallUpdate
            )
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    // Step 1: Package Booking Form Dialog
    selectedPackageForPayment?.let { pkg ->
        BookPackageDialog(
            pkg = pkg,
            defaultPhone = editPhone,
            onDismiss = { selectedPackageForPayment = null },
            onSubmit = { method, phone, summary ->
                onCreateOrder(pkg, method, phone, summary) { orderId, code ->
                    selectedPackageForPayment = null
                    val createdOrder = ConsultationOrder(
                        id = orderId,
                        packageId = pkg.id,
                        packageTitle = pkg.title,
                        amountVnd = pkg.priceVnd,
                        paymentMethod = method,
                        transferCode = code,
                        contactPhone = phone,
                        caseSummary = summary,
                        status = "pending_transfer"
                    )
                    activePaymentOrder = createdOrder to pkg
                }
            }
        )
    }

    // Step 2: Agribank VietQR Payment Dialog
    activePaymentOrder?.let { (order, pkg) ->
        VietQrPaymentDialog(
            order = order,
            pkg = pkg,
            onDismiss = { activePaymentOrder = null },
            onConfirmPaid = {
                onConfirmTransferSubmitted(order.id)
                activePaymentOrder = null
                openExternalUrl(context, LawyerContact.ZALO_URL)
            }
        )
    }
}

@Composable
private fun BookPackageDialog(
    pkg: ConsultationPackage,
    defaultPhone: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var phone by remember { mutableStateOf(defaultPhone) }
    var summary by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("agribank_vietqr") }

    val methods = listOf(
        "agribank_vietqr" to "Agribank VietQR 24/7",
        "napas_atm" to "Chuyển khoản Napas",
        "ewallet_qr" to "Ví điện tử (Quét VietQR)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đăng ký ${pkg.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Chi phí: ${pkg.priceFormatted} • ${pkg.durationLabel}",
                    style = MaterialTheme.typography.titleSmall,
                    color = BurnishedGold
                )
                Text("Phương thức thanh toán bảo mật:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(methods) { (key, label) ->
                        FilterChip(
                            selected = method == key,
                            onClick = { method = key },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại / Zalo nhận tư vấn *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Tóm tắt nội dung vụ việc *") },
                    placeholder = { Text("Mô tả ngắn vấn đề Dân sự, Đất đai, Hôn nhân hoặc THADS...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(method, phone.trim(), summary.trim()) },
                enabled = phone.isNotBlank() && summary.isNotBlank()
            ) {
                Text("Tạo mã thanh toán VietQR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}

@Composable
private fun VietQrPaymentDialog(
    order: ConsultationOrder,
    pkg: ConsultationPackage,
    onDismiss: () -> Unit,
    onConfirmPaid: () -> Unit
) {
    val context = LocalContext.current
    val qrUrl = remember(order.transferCode, order.amountVnd) {
        LawyerContact.buildAgribankVietQrUrl(order.amountVnd, order.transferCode)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Verified, contentDescription = null, tint = EmeraldTrust)
                Text("Thanh toán Agribank VietQR")
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = qrUrl,
                    contentDescription = "Mã QR chuyển khoản Agribank Nguyễn Linh Giang",
                    modifier = Modifier
                        .size(210.dp),
                    contentScale = ContentScale.Fit
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Ngân hàng: ${LawyerContact.BANK_NAME}", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "Số tài khoản: ${LawyerContact.BANK_ACCOUNT_NUMBER}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Chủ tài khoản: ${LawyerContact.BANK_ACCOUNT_HOLDER}", style = MaterialTheme.typography.bodySmall)
                        Text("Số tiền: ${pkg.priceFormatted}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Nội dung CK: ${order.transferCode}",
                            style = MaterialTheme.typography.labelLarge,
                            color = AgribankBurgundy
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(context, "Số tài khoản Agribank", LawyerContact.BANK_ACCOUNT_NUMBER)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Chép STK", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(context, "Nội dung chuyển khoản", order.transferCode)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Chép mã CK", maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPaid,
                colors = ButtonDefaults.buttonColors(containerColor = ZaloBlue, contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Đã CK & Gửi Zalo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Để sau") }
        }
    )
}

@Composable
fun AppUpdateSettingsCard(
    appUpdateState: AppUpdateUiState,
    onToggleAutoUpdate: (Boolean) -> Unit,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = appUpdateState.status
    val statusDescription = when (status) {
        AppUpdateStatus.Idle -> "Hệ thống tự động kiểm tra bản mới mỗi khi mở ứng dụng."
        AppUpdateStatus.Checking -> "Đang kiểm tra phiên bản mới từ Google Play..."
        AppUpdateStatus.UpToDate -> "Bạn đang sử dụng phiên bản mới nhất (v${appUpdateState.versionName})."
        is AppUpdateStatus.UpdateAvailable ->
            "Có phiên bản mới (mã ${status.availableVersionCode}). Chạm để tải và cập nhật ngay."
        is AppUpdateStatus.Downloading -> {
            val percent = if (status.totalBytes > 0L) {
                ((status.bytesDownloaded * 100) / status.totalBytes).toInt().coerceIn(0, 100)
            } else {
                0
            }
            if (status.totalBytes > 0L) "Đang tải bản cập nhật ($percent%)..." else "Đang tải bản cập nhật..."
        }
        AppUpdateStatus.DownloadedReadyToInstall ->
            "Bản cập nhật mới đã tải xong và sẵn sàng cài đặt."
        AppUpdateStatus.Installing -> "Đang cài đặt phiên bản mới..."
        is AppUpdateStatus.CancelledOrDeferred -> status.message
        is AppUpdateStatus.UnavailableOnDevice -> status.message
        is AppUpdateStatus.Error -> status.message
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_update_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = JudicialNavy
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cập nhật ứng dụng tự động",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Phiên bản hiện tại: v${appUpdateState.versionName} (Build ${appUpdateState.versionCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tự động cập nhật khi có phiên bản mới",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Tự động tải nền qua Google Play In-App Updates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appUpdateState.autoUpdateEnabled,
                    onCheckedChange = onToggleAutoUpdate,
                    modifier = Modifier.testTag("auto_update_switch")
                )
            }

            if (status is AppUpdateStatus.Downloading) {
                if (status.totalBytes > 0L) {
                    val progress = (status.bytesDownloaded.toFloat() / status.totalBytes.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (status == AppUpdateStatus.DownloadedReadyToInstall) {
                Button(
                    onClick = onInstallUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("complete_app_update_button")
                ) {
                    Text("Cài đặt & Khởi động lại ứng dụng")
                }
            } else {
                OutlinedButton(
                    onClick = onCheckForUpdate,
                    enabled = status != AppUpdateStatus.Checking && status != AppUpdateStatus.Installing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_app_update_button")
                ) {
                    if (status == AppUpdateStatus.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang kiểm tra...")
                    } else if (status is AppUpdateStatus.UpdateAvailable) {
                        Text("Tải & Cập nhật phiên bản mới ngay")
                    } else {
                        Text("Kiểm tra phiên bản mới")
                    }
                }
            }
        }
    }
}

