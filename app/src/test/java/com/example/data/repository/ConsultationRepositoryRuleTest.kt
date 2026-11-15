package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.LegalCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ConsultationRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfileAndCreateConsultation_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ConsultationRepository(firestore)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile("Nguyen Alice", ALICE_EMAIL)
        }
        assertTrue(profileResult.isSuccess)

        val sessionId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createConsultation("Hỏi về thời hiệu thi hành án", LegalCategory.ENFORCEMENT.key)
                .getOrThrow()
        }
        assertTrue(sessionId.isNotBlank())

        val msgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMessageToSession(
                consultationId = sessionId,
                sender = "user",
                content = "Thời hiệu yêu cầu thi hành án dân sự là bao nhiêu năm?",
                updatedMessageCount = 1L
            )
        }
        assertTrue(msgResult.isSuccess)

        val consultations = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeConsultations(uid).first { list -> list.any { it.id == sessionId } }
        }
        assertTrue(consultations.any { it.id == sessionId })
    }

    @Test
    fun createReminderAndOrder_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ConsultationRepository(firestore)

        val reminderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createReminder(
                title = "Nộp đơn khiếu nại quyết định kê biên",
                categoryKey = LegalCategory.ENFORCEMENT.key,
                dueDateMillis = System.currentTimeMillis() + 86_400_000L,
                note = "Mang theo bản sao bản án",
                notifyEnabled = true
            ).getOrThrow()
        }
        assertTrue(reminderId.isNotBlank())

        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrder(
                packageId = "pkg_quick_30",
                packageTitle = "Tư vấn chuyên sâu 1-1 qua Zalo / Hotline",
                amountVnd = 500_000L,
                paymentMethod = "agribank_vietqr",
                transferCode = "MGL123456",
                contactPhone = "0915855111",
                caseSummary = "Tư vấn hồ sơ đất đai tại Hà Nội"
            ).getOrThrow()
        }
        assertTrue(orderId.isNotBlank())

        val orders = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeOrders(uid).first { list -> list.any { it.id == orderId } }
        }
        assertEquals("pending_transfer", orders.first { it.id == orderId }.status)
    }

    @Test
    fun observeConsultations_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = ConsultationRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createConsultation("Hồ sơ riêng tư", LegalCategory.CIVIL.key).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ConsultationRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeConsultations(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user consultation observation")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    @Test
    fun observeReminders_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = ConsultationRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeReminders("some_uid").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_law@test.com"
        const val BOB_EMAIL = "bob_law@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
