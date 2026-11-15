package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.LawBookmark
import com.example.model.LegalArticleExcerpt
import com.example.model.LegalCategory
import com.example.model.LegalDocumentCatalog
import com.example.model.LegalDocumentItem
import com.example.model.LegalPortalCatalog
import com.example.model.LegalPortalGroup
import com.example.model.LegalSyncUiState
import com.example.ui.DataUiState
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LawCategoryList
import com.example.ui.components.LegalPortalGroupSection
import com.example.ui.components.LegalValidityTipsCard
import com.example.ui.components.RealTimeLegalSyncCard
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.BurnishedGoldBright
import com.example.ui.theme.JudicialNavy
import com.example.util.openExternalUrl

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LawLibraryScreen(
    bookmarksState: DataUiState<List<LawBookmark>>,
    legalSyncState: LegalSyncUiState,
    onSyncLegalPortals: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onToggleBookmark: (LegalDocumentItem, LegalArticleExcerpt, LawBookmark?) -> Unit,
    onAskAboutArticle: (LegalCategory, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<LegalCategory?>(null) }
    var showBookmarksOnly by remember { mutableStateOf(false) }
    var expandedDocIds by remember { mutableStateOf(setOf("lthads_2025", "ldd_2024")) }

    val bookmarks = (bookmarksState as? DataUiState.Success)?.data ?: emptyList()
    val statusesById = remember(legalSyncState.portalStatuses) {
        legalSyncState.portalStatuses.associateBy { it.portalId }
    }
    val officialPortals = remember {
        LegalPortalCatalog.portals.filter { it.group == LegalPortalGroup.OFFICIAL_STATE }
    }
    val specializedPortals = remember {
        LegalPortalCatalog.portals.filter { it.group == LegalPortalGroup.SPECIALIZED }
    }

    val filteredDocuments = remember(searchQuery, selectedCategory, showBookmarksOnly, bookmarks) {
        val q = searchQuery.trim().lowercase()
        LegalDocumentCatalog.documents.filter { doc ->
            val matchesCat = selectedCategory == null || doc.category == selectedCategory
            val hasBookmarkedArticle = doc.keyArticles.any { art ->
                bookmarks.any { it.documentId == doc.id && it.articleCode == art.articleNumber }
            }
            val matchesBookmarkFilter = !showBookmarksOnly || hasBookmarkedArticle
            val matchesQuery = q.isEmpty() ||
                doc.title.lowercase().contains(q) ||
                doc.codeNumber.lowercase().contains(q) ||
                doc.summary.lowercase().contains(q) ||
                doc.keyArticles.any {
                    it.articleNumber.lowercase().contains(q) ||
                        it.heading.lowercase().contains(q) ||
                        it.content.lowercase().contains(q)
                }
            matchesCat && matchesBookmarkFilter && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 800.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_legal_library),
                        contentDescription = "Thư viện tra cứu văn bản pháp luật",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        JudicialNavy.copy(alpha = 0.92f),
                                        JudicialNavy.copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "TRA CỨU PHÁP LUẬT THỜI GIAN THỰC",
                            style = MaterialTheme.typography.labelSmall,
                            color = BurnishedGoldBright,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Văn bản Pháp luật Trọng tâm",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = "Đồng bộ trực tiếp với Cổng Nhà nước & tra cứu Lược đồ hiệu lực",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }
            }
        }

        item {
            RealTimeLegalSyncCard(
                syncState = legalSyncState,
                onSyncNow = onSyncLegalPortals,
                onToggleAutoSync = onToggleAutoSync
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("law_search_input"),
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                },
                placeholder = { Text("Tìm văn bản trong kho & tìm trên cổng Nhà nước (VD: 106/2025, Sổ đỏ)...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            LawCategoryList(
                documents = LegalDocumentCatalog.documents,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    selectedCategory = category
                    if (category != null) {
                        showBookmarksOnly = false
                    }
                },
                onDocumentSelected = { doc ->
                    expandedDocIds = expandedDocIds + doc.id
                    showBookmarksOnly = false
                }
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null && !showBookmarksOnly,
                        onClick = {
                            selectedCategory = null
                            showBookmarksOnly = false
                        },
                        label = { Text("Tất cả") }
                    )
                }
                item {
                    FilterChip(
                        selected = showBookmarksOnly,
                        onClick = { showBookmarksOnly = !showBookmarksOnly },
                        label = { Text("Đã lưu (${bookmarks.size})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                items(LegalCategory.lawDomains) { domain ->
                    FilterChip(
                        selected = selectedCategory == domain,
                        onClick = {
                            selectedCategory = if (selectedCategory == domain) null else domain
                        },
                        label = { Text(domain.shortName) }
                    )
                }
            }
        }

        if (filteredDocuments.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Không tìm thấy văn bản trong danh mục cục bộ",
                    subtitle = "Quý khách có thể nhấn 'Tra cứu nhanh' ở các Cổng thông tin Nhà nước bên dưới hoặc hỏi trực tiếp Luật gia."
                )
            }
        } else {
            items(filteredDocuments, key = { it.id }) { doc ->
                val isExpanded = doc.id in expandedDocIds || searchQuery.isNotBlank() || showBookmarksOnly
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedDocIds = if (doc.id in expandedDocIds) {
                                        expandedDocIds - doc.id
                                    } else {
                                        expandedDocIds + doc.id
                                    }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryBadge(category = doc.category)
                                    Text(
                                        text = doc.codeNumber,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BurnishedGold,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${doc.effectiveDate} • ${doc.summary}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    expandedDocIds = if (doc.id in expandedDocIds) {
                                        expandedDocIds - doc.id
                                    } else {
                                        expandedDocIds + doc.id
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng"
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AssistChip(
                                        onClick = {
                                            openExternalUrl(
                                                context,
                                                LegalPortalCatalog.buildVbplLookupUrl(doc.codeNumber)
                                            )
                                        },
                                        label = { Text("Lược đồ & Hợp nhất (VBPL)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                    AssistChip(
                                        onClick = {
                                            openExternalUrl(
                                                context,
                                                LegalPortalCatalog.buildTvplLookupUrl(doc.title)
                                            )
                                        },
                                        label = { Text("Điểm mới (TVPL)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                val visibleArticles = if (showBookmarksOnly) {
                                    doc.keyArticles.filter { art ->
                                        bookmarks.any { it.documentId == doc.id && it.articleCode == art.articleNumber }
                                    }
                                } else {
                                    doc.keyArticles
                                }
                                visibleArticles.forEach { article ->
                                    val existingBookmark = bookmarks.firstOrNull {
                                        it.documentId == doc.id && it.articleCode == article.articleNumber
                                    }
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.background
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${article.articleNumber}: ${article.heading}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        onToggleBookmark(doc, article, existingBookmark)
                                                    },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (existingBookmark != null) {
                                                            Icons.Filled.Bookmark
                                                        } else {
                                                            Icons.Filled.BookmarkBorder
                                                        },
                                                        contentDescription = if (existingBookmark != null) {
                                                            "Bỏ lưu điều luật"
                                                        } else {
                                                            "Lưu điều luật"
                                                        },
                                                        tint = if (existingBookmark != null) {
                                                            BurnishedGold
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                            }
                                            Text(
                                                text = article.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    onAskAboutArticle(
                                                        doc.category,
                                                        "Xin Luật gia giải thích và hướng dẫn áp dụng ${article.articleNumber} (${article.heading}) của ${doc.title} vào trường hợp thực tế."
                                                    )
                                                },
                                                modifier = Modifier.height(48.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.QuestionAnswer,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Hỏi Luật gia về điều luật này",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            LegalPortalGroupSection(
                group = LegalPortalGroup.OFFICIAL_STATE,
                portals = officialPortals,
                statusesById = statusesById,
                activeSearchQuery = searchQuery
            )
        }

        item {
            LegalPortalGroupSection(
                group = LegalPortalGroup.SPECIALIZED,
                portals = specializedPortals,
                statusesById = statusesById,
                activeSearchQuery = searchQuery
            )
        }

        item {
            LegalValidityTipsCard()
        }
    }
}
