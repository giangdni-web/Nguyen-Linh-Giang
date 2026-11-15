package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val notifyOnReply: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ConsultationSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = LegalCategory.GENERAL.key,
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ConsultationMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "user",
    val content: String = "",
    val legalCitations: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LawBookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val documentId: String = "",
    val articleCode: String = "",
    val title: String = "",
    val category: String = LegalCategory.CIVIL.key,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LegalReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = LegalCategory.GENERAL.key,
    val dueDateMillis: Long = 0L,
    val note: String = "",
    val isCompleted: Boolean = false,
    val notifyEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ConsultationOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packageId: String = "",
    val packageTitle: String = "",
    val amountVnd: Long = 0L,
    val paymentMethod: String = "agribank_vietqr",
    val transferCode: String = "",
    val contactPhone: String = "",
    val caseSummary: String = "",
    val status: String = "pending_transfer",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
