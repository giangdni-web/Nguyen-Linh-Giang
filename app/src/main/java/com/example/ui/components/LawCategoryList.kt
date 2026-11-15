package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LegalCategory
import com.example.model.LegalDocumentItem
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.JudicialNavy
import com.example.ui.theme.OnGoldContainer

/**
 * Organizes legal documents by legal fields (Civil, Criminal, Labor, Enforcement, Land, Family)
 * into a scannable category grid so users can browse categories and inspect document counts easily.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LawCategoryList(
    documents: List<LegalDocumentItem>,
    selectedCategory: LegalCategory?,
    onCategorySelected: (LegalCategory?) -> Unit,
    onDocumentSelected: ((LegalDocumentItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val groupedDocuments = remember(documents) {
        LegalCategory.lawDomains.map { category ->
            category to documents.filter { it.category == category }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("law_category_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = "Danh mục lĩnh vực pháp luật",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Chọn lĩnh vực: Dân sự, Hình sự, Lao động, Thi hành án, Đất đai, Hôn nhân",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selectedCategory != null) {
                TextButton(
                    onClick = { onCategorySelected(null) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("law_category_show_all_button")
                ) {
                    Text("Tất cả (${documents.size})")
                }
            }
        }

        groupedDocuments.chunked(2).forEach { rowPairs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowPairs.forEach { (category, docsInCategory) ->
                    val isSelected = selectedCategory == category
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 122.dp)
                            .clickable {
                                val nextCategory = if (isSelected) null else category
                                onCategorySelected(nextCategory)
                                if (nextCategory != null) {
                                    docsInCategory.firstOrNull()?.let { firstDoc ->
                                        onDocumentSelected?.invoke(firstDoc)
                                    }
                                }
                            }
                            .testTag("law_category_item_${category.key}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) JudicialNavy else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                BurnishedGold
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            }
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
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) {
                                        Color.White.copy(alpha = 0.14f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(category),
                                        contentDescription = category.displayName,
                                        tint = if (isSelected) {
                                            BurnishedGold
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) BurnishedGold else GoldContainer
                                ) {
                                    Text(
                                        text = "${docsInCategory.size} VB",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) JudicialNavy else OnGoldContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = category.shortName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = category.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) {
                                    Color.White.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (docsInCategory.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    docsInCategory.take(2).forEach { doc ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) {
                                                Color.White.copy(alpha = 0.14f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                            }
                                        ) {
                                            Text(
                                                text = doc.codeNumber.removePrefix("Luật số ").removePrefix("Nghị định "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
}
