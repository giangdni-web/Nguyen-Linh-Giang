package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ConsultationSession
import com.example.model.LegalCategory
import com.example.ui.DataUiState
import com.example.ui.components.AdvisorCredentialsCard
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FanpagePromoCard
import com.example.ui.components.SecurityTrustBanner
import com.example.ui.components.categoryIcon
import com.example.ui.theme.EmeraldTrust
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeConsultTab(
    displayName: String,
    consultationsState: DataUiState<List<ConsultationSession>>,
    onStartNewConsultation: (LegalCategory, String) -> Unit,
    onOpenSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRequestSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<LegalCategory?>(null) }
    var quickQuestion by remember { mutableStateOf("") }
    var selectedAskCategory by remember { mutableStateOf(LegalCategory.CIVIL) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 800.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Xin chào, ${displayName.ifBlank { "Quý khách" }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tư vấn Pháp luật Mr.Giang Law",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                OutlinedButton(
                    onClick = onRequestSignOut,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("home_sign_out_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Đăng xuất",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Đăng xuất",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        item {
            AdvisorCredentialsCard(
                onAskAiClick = {
                    onStartNewConsultation(selectedAskCategory, "")
                }
            )
        }

        // Quick Ask Box
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Đặt câu hỏi pháp lý nhanh",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LegalCategory.lawDomains) { domain ->
                            FilterChip(
                                selected = selectedAskCategory == domain,
                                onClick = { selectedAskCategory = domain },
                                label = { Text(domain.shortName) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryIcon(domain),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = quickQuestion,
                        onValueChange = { quickQuestion = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_question_input"),
                        placeholder = {
                            Text("Nhập vấn đề Dân sự, Thi hành án, Đất đai hoặc Hôn nhân cần hỏi...")
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Button(
                        onClick = {
                            val q = quickQuestion.trim()
                            quickQuestion = ""
                            onStartNewConsultation(selectedAskCategory, q)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_consultation_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddComment,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gửi câu hỏi & Lưu lịch sử tư vấn")
                    }
                }
            }
        }

        // 4 Legal Domains Grid-like rows
        item {
            Text(
                text = "Lĩnh vực tư vấn chuyên sâu",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(LegalCategory.lawDomains.chunked(2)) { rowDomains ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowDomains.forEach { domain ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onStartNewConsultation(domain, "") }
                            .testTag("domain_card_${domain.key}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon(domain),
                                contentDescription = domain.displayName,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = domain.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = domain.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            FanpagePromoCard()
        }

        // Consultation History Header & Filter
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lịch sử tư vấn đã lưu",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("Tất cả") }
                        )
                    }
                    items(LegalCategory.lawDomains) { domain ->
                        FilterChip(
                            selected = selectedFilter == domain,
                            onClick = { selectedFilter = domain },
                            label = { Text(domain.shortName) }
                        )
                    }
                }
            }
        }

        when (consultationsState) {
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
                        icon = Icons.Filled.History,
                        title = "Không thể tải lịch sử tư vấn",
                        subtitle = consultationsState.message
                    )
                }
            }

            is DataUiState.Success -> {
                val filtered = consultationsState.data.filter { session ->
                    selectedFilter == null || session.category == selectedFilter?.key
                }
                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.AddComment,
                            title = "Chưa có phiên tư vấn nào",
                            subtitle = "Đặt câu hỏi đầu tiên ở khung phía trên hoặc chọn một lĩnh vực pháp luật để lưu lịch sử tư vấn.",
                            actionLabel = "Bắt đầu tư vấn mới",
                            onActionClick = { onStartNewConsultation(selectedAskCategory, "") }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { session ->
                        val cat = LegalCategory.fromKey(session.category)
                        val timeText = (session.updatedAt ?: session.createdAt)?.toDate()
                            ?.let { dateFormatter.format(it) }
                            ?: dateFormatter.format(Date())
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSession(session.id) }
                                .testTag("consultation_item_${session.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CategoryBadge(category = cat)
                                        if (session.status == "resolved") {
                                            Surface(
                                                color = EmeraldTrust.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = null,
                                                        tint = EmeraldTrust,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "Đã hoàn tất",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = EmeraldTrust
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteSession(session.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Xóa phiên tư vấn",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = session.lastMessagePreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$timeText • ${session.messageCount} tin nhắn",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Mở phiên tư vấn",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SecurityTrustBanner()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
