package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LegalPortalCatalog
import com.example.model.LegalPortalGroup
import com.example.model.LegalPortalSource
import com.example.model.LegalSyncUiState
import com.example.model.PortalSyncStatus
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.BurnishedGoldBright
import com.example.ui.theme.EmeraldTrust
import com.example.ui.theme.EmeraldTrustContainer
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.JudicialNavy
import com.example.ui.theme.OnGoldContainer
import com.example.util.openExternalUrl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RealTimeLegalSyncCard(
    syncState: LegalSyncUiState,
    onSyncNow: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeFormatter = remember {
        SimpleDateFormat("HH:mm • dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
    }
    val formattedTime = remember(syncState.lastSyncMillis) {
        if (syncState.lastSyncMillis > 0L) {
            timeFormatter.format(Date(syncState.lastSyncMillis))
        } else {
            "Chưa đồng bộ"
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(JudicialNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        if (syncState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = BurnishedGoldBright
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = "Đồng bộ cơ sở dữ liệu pháp luật",
                                tint = BurnishedGoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Đồng bộ dữ liệu pháp luật thời gian thực",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (syncState.isSyncing) {
                                "Đang kết nối tới ${syncState.totalCount} cổng thông tin..."
                            } else {
                                "Kết nối ${syncState.reachableCount}/${syncState.totalCount} cổng • $formattedTime"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onSyncNow,
                    enabled = !syncState.isSyncing,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("sync_legal_db_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JudicialNavy,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (syncState.isSyncing) "Đang tải" else "Đồng bộ ngay",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Tự động cập nhật khi mở ứng dụng & định kỳ",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Kiểm tra văn bản mới từ Cổng Nhà nước & trang chuyên ngành",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = syncState.autoSyncEnabled,
                    onCheckedChange = onToggleAutoSync,
                    modifier = Modifier.testTag("auto_sync_switch")
                )
            }

            if (!syncState.errorMessage.isNullOrBlank()) {
                Text(
                    text = syncState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            val headlines = syncState.combinedHeadlines
            if (headlines.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Text(
                    text = "Văn bản & Tin pháp luật vừa đồng bộ trực tiếp (${headlines.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    headlines.take(6).forEach { headline ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .clickable { openExternalUrl(context, headline.url) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = headline.portalDomain,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BurnishedGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = headline.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Mở ${headline.title}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegalPortalGroupSection(
    group: LegalPortalGroup,
    portals: List<LegalPortalSource>,
    statusesById: Map<String, PortalSyncStatus>,
    activeSearchQuery: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (group == LegalPortalGroup.OFFICIAL_STATE) {
                    Icons.Filled.AccountBalance
                } else {
                    Icons.Filled.Public
                },
                contentDescription = null,
                tint = if (group == LegalPortalGroup.OFFICIAL_STATE) {
                    MaterialTheme.colorScheme.primary
                } else {
                    BurnishedGold
                },
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = group.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        portals.forEach { portal ->
            val status = statusesById[portal.id]
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
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
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = portal.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = BurnishedGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${portal.name} (${portal.domain})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        PortalConnectionBadge(status = status)
                    }

                    Text(
                        text = portal.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (status != null && !status.isReachable && status.errorNote.isNotBlank()) {
                        Text(
                            text = status.errorNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        portal.highlights.forEach { highlight ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = highlight,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnGoldContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { openExternalUrl(context, portal.baseUrl) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("open_portal_${portal.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mở cổng")
                        }

                        val effectiveQuery = activeSearchQuery.trim().ifBlank { "Luật Thi hành án dân sự 2025" }
                        Button(
                            onClick = { openExternalUrl(context, portal.buildSearchUrl(effectiveQuery)) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("search_portal_${portal.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JudicialNavy,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeSearchQuery.isBlank()) "Tra cứu nhanh" else "Tìm từ khóa",
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalConnectionBadge(status: PortalSyncStatus?) {
    val isLive = status?.isReachable == true
    val bgColor = if (isLive) EmeraldTrustContainer else MaterialTheme.colorScheme.surfaceVariant
    val fgColor = if (isLive) EmeraldTrust else MaterialTheme.colorScheme.onSurfaceVariant
    val label = when {
        status == null || status.syncedAtMillis == 0L -> "Chờ đồng bộ"
        status.isReachable -> "HTTP ${status.httpCode ?: 200} • ${status.latencyMs}ms"
        status.httpCode != null -> "HTTP ${status.httpCode}"
        else -> "Ngoại tuyến"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isLive) Icons.Filled.CheckCircle else Icons.Filled.CloudOff,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegalValidityTipsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GoldContainer.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = BurnishedGold
                )
                Text(
                    text = "Mẹo kiểm tra hiệu lực văn bản chuẩn xác",
                    style = MaterialTheme.typography.titleSmall,
                    color = OnGoldContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "• Luôn kiểm tra \"Lược đồ văn bản\": Trước khi áp dụng một điều luật, hãy xem tab Lược đồ trên vbpl.vn hoặc thuvienphapluat.vn để chắc chắn điều khoản đó không bị sửa đổi, bổ sung hoặc bãi bỏ bởi một văn bản ban hành sau.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnGoldContainer
                )
                Text(
                    text = "• Ưu tiên \"Văn bản hợp nhất\": Đối với các luật đã qua nhiều lần sửa đổi, hãy đọc bản Văn bản hợp nhất do Văn phòng Quốc hội hoặc các Bộ ban hành để có cái nhìn liền mạch và tránh bỏ sót quy định.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnGoldContainer
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = {
                        openExternalUrl(
                            context,
                            LegalPortalCatalog.buildConsolidatedLookupUrl("Thi hành án dân sự")
                        )
                    },
                    label = { Text("Tìm Văn bản hợp nhất (VBPL)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("tip_consolidated_vbpl_chip")
                )
                AssistChip(
                    onClick = {
                        openExternalUrl(
                            context,
                            LegalPortalCatalog.buildTvplLookupUrl("Luật Thi hành án dân sự 106/2025/QH15")
                        )
                    },
                    label = { Text("Xem Lược đồ & Điểm mới (TVPL)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("tip_diagram_tvpl_chip")
                )
            }
        }
    }
}
