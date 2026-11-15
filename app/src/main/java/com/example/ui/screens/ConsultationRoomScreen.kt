package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.repository.AiResponseSpeed
import com.example.model.ConsultationMessage
import com.example.model.ConsultationSession
import com.example.model.LegalCategory
import com.example.ui.AiAnswerState
import com.example.ui.DataUiState
import com.example.ui.components.CategoryBadge
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.JudicialNavy
import com.example.ui.theme.ZaloBlue
import com.example.util.LawyerContact
import com.example.util.copyToClipboard
import com.example.util.openExternalUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationRoomScreen(
    session: ConsultationSession,
    messagesState: DataUiState<List<ConsultationMessage>>,
    aiState: AiAnswerState,
    aiSpeedMode: AiResponseSpeed = AiResponseSpeed.FAST,
    onSelectAiSpeedMode: (AiResponseSpeed) -> Unit = {},
    onBack: () -> Unit,
    onSendQuestion: (String, List<ConsultationMessage>) -> Unit,
    onRetryQuestion: () -> Unit = {},
    onMarkResolved: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val category = LegalCategory.fromKey(session.category)
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val displayedMessages = (messagesState as? DataUiState.Success)?.data ?: emptyList()

    LaunchedEffect(displayedMessages.size, aiState) {
        val hasFooterItem = aiState is AiAnswerState.Streaming ||
            aiState is AiAnswerState.Preparing ||
            aiState is AiAnswerState.Error
        val total = displayedMessages.size + if (hasFooterItem) 1 else 0
        if (total > 0) {
            if (aiState is AiAnswerState.Streaming) {
                listState.scrollToItem(total - 1)
            } else {
                listState.animateScrollToItem(total - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${category.displayName} • ${LawyerContact.ADVISOR_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    if (session.status == "active") {
                        TextButton(onClick = onMarkResolved) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hoàn tất")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { openExternalUrl(context, LawyerContact.ZALO_URL) },
                            label = {
                                Text(
                                    "Zalo 0915855111",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZaloBlue
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = ZaloBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {
                                    val next = if (aiSpeedMode == AiResponseSpeed.FAST) {
                                        AiResponseSpeed.DEEP
                                    } else {
                                        AiResponseSpeed.FAST
                                    }
                                    onSelectAiSpeedMode(next)
                                },
                                label = {
                                    Text(
                                        text = aiSpeedMode.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = "Chế độ tốc độ phản hồi",
                                        tint = BurnishedGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                modifier = Modifier.testTag("ai_speed_toggle_chip")
                            )
                            CategoryBadge(category = category)
                        }
                    }

                    if (session.status == "resolved") {
                        Text(
                            text = "Phiên tư vấn này đã được đánh dấu hoàn tất. Quý khách có thể tạo phiên mới hoặc nhắn Zalo 0915855111.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input"),
                                placeholder = { Text("Nhập câu hỏi pháp lý chi tiết...") },
                                maxLines = 4,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Button(
                                onClick = {
                                    val q = inputText.trim()
                                    if (q.isNotEmpty()) {
                                        inputText = ""
                                        onSendQuestion(q, displayedMessages)
                                    }
                                },
                                enabled = inputText.isNotBlank() &&
                                    aiState !is AiAnswerState.Preparing &&
                                    aiState !is AiAnswerState.Streaming,
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("chat_send_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Gửi câu hỏi"
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (displayedMessages.isEmpty() && aiState is AiAnswerState.Idle) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Gavel,
                                        contentDescription = null,
                                        tint = BurnishedGold
                                    )
                                    Text(
                                        text = "Phòng Hỏi Đáp ${category.displayName}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = "Mọi câu hỏi và nội dung giải đáp sẽ được lưu tự động vào lịch sử tư vấn của quý khách. Gợi ý câu hỏi nhanh:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    onClick = { onSendQuestion(category.samplePrompt, displayedMessages) },
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "“${category.samplePrompt}”",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                items(displayedMessages, key = { it.id }) { msg ->
                    val isUser = msg.sender == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 520.dp),
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isUser) 18.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 18.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) JudicialNavy else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isUser) null else BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!isUser) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Trợ lý Mr.Giang Law",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BurnishedGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = {
                                                copyToClipboard(context, "Nội dung tư vấn", msg.content)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = "Sao chép nội dung",
                                                modifier = Modifier.size(15.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = msg.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                when (aiState) {
                    is AiAnswerState.Preparing -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = "Đang tra cứu quy định pháp luật hiện hành...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    is AiAnswerState.Streaming -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Trợ lý Mr.Giang Law đang phản hồi...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BurnishedGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = aiState.partialText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    is AiAnswerState.Error -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = aiState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onRetryQuestion,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("retry_ai_question_button")
                                        ) {
                                            Text("Thử lại")
                                        }
                                        OutlinedButton(
                                            onClick = { openExternalUrl(context, LawyerContact.ZALO_URL) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Zalo 0915855111")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AiAnswerState.Idle -> Unit
                }
            }
        }
    }
}
