package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.model.LegalCategory
import com.example.model.LegalReminder
import com.example.ui.DataUiState
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.EmeraldTrust
import com.example.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class OffsetOption(val label: String, val offsetMillis: Long)

@Composable
fun RemindersScreen(
    remindersState: DataUiState<List<LegalReminder>>,
    onCreateReminder: (String, LegalCategory, Long, String, Boolean) -> Unit,
    onToggleCompleted: (LegalReminder) -> Unit,
    onDeleteReminder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotifPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 800.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nhắc lịch & Thời hiệu",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Theo dõi hạn khiếu nại THADS, lịch hẹn Tòa án & tư vấn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_reminder_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.AddAlarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tạo lịch")
                }
            }
        }

        if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bật thông báo nhắc lịch & phản hồi mới",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Cho phép nhận thông báo tự động khi có giải đáp mới hoặc đến hạn hồ sơ.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        ) {
                            Text("Bật ngay")
                        }
                    }
                }
            }
        }

        when (remindersState) {
            is DataUiState.Loading -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is DataUiState.Error -> {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Schedule,
                        title = "Không thể tải lịch nhắc",
                        subtitle = remindersState.message
                    )
                }
            }

            is DataUiState.Success -> {
                val reminders = remindersState.data
                if (reminders.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.AddAlarm,
                            title = "Chưa có lịch nhắc pháp lý",
                            subtitle = "Lưu nhắc nhở cho thời hạn khiếu nại, thời hiệu thi hành án theo Luật 106/2025/QH15 hoặc lịch hẹn tư vấn với Luật gia Nguyễn Linh Giang.",
                            actionLabel = "Tạo lịch nhắc đầu tiên",
                            onActionClick = { showCreateDialog = true }
                        )
                    }
                } else {
                    items(reminders, key = { it.id }) { reminder ->
                        val cat = LegalCategory.fromKey(reminder.category)
                        val formattedDate = dateFormat.format(Date(reminder.dueDateMillis))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = { onToggleCompleted(reminder) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (reminder.isCompleted) {
                                            Icons.Filled.CheckBox
                                        } else {
                                            Icons.Filled.CheckBoxOutlineBlank
                                        },
                                        contentDescription = if (reminder.isCompleted) "Đã xong" else "Đánh dấu xong",
                                        tint = if (reminder.isCompleted) EmeraldTrust else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CategoryBadge(category = cat)
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = reminder.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        textDecoration = if (reminder.isCompleted) {
                                            TextDecoration.LineThrough
                                        } else {
                                            TextDecoration.None
                                        }
                                    )
                                    if (reminder.note.isNotBlank()) {
                                        Text(
                                            text = reminder.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteReminder(reminder.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Xóa lịch nhắc",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateReminderDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, cat, dueMillis, note, notify ->
                if (notify && !hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                onCreateReminder(title, cat, dueMillis, note, notify)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, LegalCategory, Long, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(LegalCategory.ENFORCEMENT) }
    var notifyEnabled by remember { mutableStateOf(true) }

    val offsets = remember {
        listOf(
            OffsetOption("1 giờ tới", 3_600_000L),
            OffsetOption("Ngày mai (24h)", 86_400_000L),
            OffsetOption("3 ngày tới", 3L * 86_400_000L),
            OffsetOption("7 ngày tới", 7L * 86_400_000L),
            OffsetOption("15 ngày (Hạn khiếu nại)", 15L * 86_400_000L)
        )
    }
    var selectedOffset by remember { mutableLongStateOf(offsets[1].offsetMillis) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo lịch nhắc pháp lý") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nội dung nhắc lịch *") },
                    placeholder = { Text("VD: Nộp đơn khiếu nại THA / Hẹn tư vấn Zalo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input")
                )
                Text("Lĩnh vực:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LegalCategory.lawDomains) { domain ->
                        FilterChip(
                            selected = selectedCategory == domain,
                            onClick = { selectedCategory = domain },
                            label = { Text(domain.shortName) }
                        )
                    }
                }
                Text("Thời điểm nhắc:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(offsets) { opt ->
                        FilterChip(
                            selected = selectedOffset == opt.offsetMillis,
                            onClick = { selectedOffset = opt.offsetMillis },
                            label = { Text(opt.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú hồ sơ (tùy chọn)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gửi thông báo thiết bị", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = notifyEnabled,
                        onCheckedChange = { notifyEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.trim(),
                        selectedCategory,
                        System.currentTimeMillis() + selectedOffset,
                        note.trim(),
                        notifyEnabled
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_reminder_button")
            ) {
                Text("Lưu lịch nhắc")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
