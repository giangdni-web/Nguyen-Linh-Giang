package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.model.ConsultationMessage
import com.example.model.ConsultationOrder
import com.example.model.ConsultationSession
import com.example.model.LawBookmark
import com.example.model.LegalReminder
import com.example.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ConsultationRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Vui lòng đăng nhập bằng Google trước khi truy cập dữ liệu.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = userDoc(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        val existing = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            throw e
        }

        if (!existing.exists()) {
            val safeName = displayName.ifBlank { "Khách hàng Mr.Giang Law" }.take(100)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "email" to email.take(160),
                "phoneNumber" to "",
                "notifyOnReply" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
                throw e
            }
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        phoneNumber: String,
        notifyOnReply: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        val payload = mapOf(
            "displayName" to displayName.trim().ifBlank { "Khách hàng" }.take(100),
            "phoneNumber" to phoneNumber.trim().take(25),
            "notifyOnReply" to notifyOnReply,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    // --- Consultation Sessions & Messages ---

    fun observeConsultations(userId: String): Flow<List<ConsultationSession>> = flow {
        val colRef = userDoc(userId).collection("consultations")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ConsultationSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun observeMessages(userId: String, consultationId: String): Flow<List<ConsultationMessage>> = flow {
        val colRef = userDoc(userId)
            .collection("consultations")
            .document(consultationId)
            .collection("messages")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ConsultationMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(Long.MAX_VALUE / 2, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    fun generateNewConsultationId(): String {
        val uid = requireUserId()
        return userDoc(uid).collection("consultations").document().id
    }

    suspend fun createConsultation(
        title: String,
        categoryKey: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("consultations").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().ifBlank { "Phiên tư vấn mới" }.take(140),
            "category" to categoryKey,
            "lastMessagePreview" to "Bắt đầu phiên tư vấn cùng Mr.Giang Law",
            "messageCount" to 0L,
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun createConsultationWithFirstMessage(
        consultationId: String,
        title: String,
        categoryKey: String,
        userQuestion: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val sessionRef = userDoc(uid).collection("consultations").document(consultationId)
        val msgRef = sessionRef.collection("messages").document()
        val trimmedQuestion = userQuestion.trim().take(12000)
        val preview = trimmedQuestion.replace("\n", " ").take(280)

        val sessionPayload = mapOf(
            "userId" to uid,
            "title" to title.trim().ifBlank { "Phiên tư vấn mới" }.take(140),
            "category" to categoryKey,
            "lastMessagePreview" to preview,
            "messageCount" to 1L,
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val msgPayload = mapOf(
            "userId" to uid,
            "sender" to "user",
            "content" to trimmedQuestion,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            val batch = db.batch()
            batch.set(sessionRef, sessionPayload)
            batch.set(msgRef, msgPayload)
            batch.commit().await()
            msgRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, sessionRef.path, auth)
            throw e
        }
    }

    suspend fun addMessageToSession(
        consultationId: String,
        sender: String,
        content: String,
        legalCitations: String = "",
        updatedMessageCount: Long,
        sessionTitleUpdate: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val sessionRef = userDoc(uid).collection("consultations").document(consultationId)
        val msgRef = sessionRef.collection("messages").document()

        val msgPayload = buildMap<String, Any> {
            put("userId", uid)
            put("sender", sender)
            put("content", content.take(12000))
            if (legalCitations.isNotBlank()) {
                put("legalCitations", legalCitations.take(1000))
            }
            put("createdAt", FieldValue.serverTimestamp())
        }

        val preview = content.replace("\n", " ").trim().take(280)
        val sessionUpdate = buildMap<String, Any> {
            put("lastMessagePreview", preview)
            put("messageCount", updatedMessageCount)
            put("updatedAt", FieldValue.serverTimestamp())
            if (!sessionTitleUpdate.isNullOrBlank()) {
                put("title", sessionTitleUpdate.trim().take(140))
            }
        }

        try {
            val batch = db.batch()
            batch.set(msgRef, msgPayload)
            batch.update(sessionRef, sessionUpdate)
            batch.commit().await()
            msgRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun markConsultationResolved(consultationId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("consultations").document(consultationId)
        try {
            docRef.update(
                mapOf(
                    "status" to "resolved",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteConsultation(consultationId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val sessionRef = userDoc(uid).collection("consultations").document(consultationId)
        try {
            val messages = sessionRef.collection("messages").get().await()
            val batch = db.batch()
            for (doc in messages.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(sessionRef)
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, sessionRef.path, auth)
            throw e
        }
    }

    // --- Bookmarks ---

    fun observeBookmarks(userId: String): Flow<List<LawBookmark>> = flow {
        val colRef = userDoc(userId).collection("bookmarks")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LawBookmark::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun addBookmark(
        documentId: String,
        articleCode: String,
        title: String,
        categoryKey: String,
        note: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("bookmarks").document()
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("documentId", documentId.take(80))
            put("articleCode", articleCode.take(100))
            put("title", title.take(200))
            put("category", categoryKey)
            if (note.isNotBlank()) put("note", note.take(500))
            put("createdAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteBookmark(bookmarkId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("bookmarks").document(bookmarkId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- Reminders ---

    fun observeReminders(userId: String): Flow<List<LegalReminder>> = flow {
        val colRef = userDoc(userId).collection("reminders")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LegalReminder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.dueDateMillis }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun createReminder(
        title: String,
        categoryKey: String,
        dueDateMillis: Long,
        note: String,
        notifyEnabled: Boolean
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("reminders").document()
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("title", title.trim().take(140))
            put("category", categoryKey)
            put("dueDateMillis", dueDateMillis)
            if (note.isNotBlank()) put("note", note.trim().take(500))
            put("isCompleted", false)
            put("notifyEnabled", notifyEnabled)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun toggleReminderCompleted(reminder: LegalReminder): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("reminders").document(reminder.id)
        try {
            docRef.update(
                mapOf(
                    "isCompleted" to !reminder.isCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteReminder(reminderId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("reminders").document(reminderId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- Consultation Package Orders ---

    fun observeOrders(userId: String): Flow<List<ConsultationOrder>> = flow {
        val colRef = userDoc(userId).collection("orders")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ConsultationOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, colRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun createOrder(
        packageId: String,
        packageTitle: String,
        amountVnd: Long,
        paymentMethod: String,
        transferCode: String,
        contactPhone: String,
        caseSummary: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("orders").document()
        val payload = mapOf(
            "userId" to uid,
            "packageId" to packageId.take(80),
            "packageTitle" to packageTitle.take(140),
            "amountVnd" to amountVnd,
            "paymentMethod" to paymentMethod,
            "transferCode" to transferCode.take(40),
            "contactPhone" to contactPhone.trim().take(25),
            "caseSummary" to caseSummary.trim().take(2000),
            "status" to "pending_transfer",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun markOrderReceiptSubmitted(orderId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("orders").document(orderId)
        try {
            docRef.update(
                mapOf(
                    "status" to "submitted_receipt",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    private fun logStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path, auth)
        }
    }
}
