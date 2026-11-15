package com.example

import com.example.data.repository.LegalAiRepository
import com.example.model.ConsultationMessage
import com.example.model.ConsultationPackageCatalog
import com.example.model.LegalCategory
import com.example.model.LegalDocumentCatalog
import com.example.model.LegalPortalCatalog
import com.example.model.LegalPortalGroup
import com.example.util.LawyerContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogUnitTest {

    @Test
    fun legalCatalog_coversAllRequiredAndGeneralDomains() {
        val categoriesPresent = LegalDocumentCatalog.documents.map { it.category }.toSet()
        assertTrue(LegalCategory.CIVIL in categoriesPresent)
        assertTrue(LegalCategory.CRIMINAL in categoriesPresent)
        assertTrue(LegalCategory.LABOR in categoriesPresent)
        assertTrue(LegalCategory.ENFORCEMENT in categoriesPresent)
        assertTrue(LegalCategory.LAND in categoriesPresent)
        assertTrue(LegalCategory.FAMILY in categoriesPresent)
    }

    @Test
    fun legalCatalog_usesCurrent2025And2026ThadsLegislation() {
        val codes = LegalDocumentCatalog.documents.map { it.codeNumber }
        val ids = LegalDocumentCatalog.documents.map { it.id }
        assertTrue(codes.contains("Luật số 106/2025/QH15"))
        assertTrue(codes.contains("Nghị định 152/2026/NĐ-CP"))
        assertTrue(codes.contains("Luật số 102/2025/QH15"))
        assertTrue("lthads_2008_hn" !in ids)
        assertTrue("nd_33_2020" !in ids)
    }

    @Test
    fun legalPortals_includeAllOfficialAndSpecializedSources() {
        val domains = LegalPortalCatalog.portals.map { it.domain }.toSet()
        assertTrue("vbpl.vn" in domains)
        assertTrue("chinhphu.vn" in domains)
        assertTrue("congbao.chinhphu.vn" in domains)
        assertTrue("quochoi.vn" in domains)
        assertTrue("thuvienphapluat.vn" in domains)
        assertTrue("luatvietnam.vn" in domains)

        val official = LegalPortalCatalog.portals.filter { it.group == LegalPortalGroup.OFFICIAL_STATE }
        val specialized = LegalPortalCatalog.portals.filter { it.group == LegalPortalGroup.SPECIALIZED }
        assertEquals(4, official.size)
        assertEquals(2, specialized.size)

        val vbplSearch = LegalPortalCatalog.buildVbplLookupUrl("106/2025/QH15")
        assertTrue(vbplSearch.contains("vbpl.vn"))
        assertTrue(vbplSearch.contains("106"))

        val consolidatedSearch = LegalPortalCatalog.buildConsolidatedLookupUrl("Thi hành án")
        assertTrue(consolidatedSearch.contains("vbpl.vn"))
    }

    @Test
    fun consultationPackages_havePositivePrices() {
        assertTrue(ConsultationPackageCatalog.packages.isNotEmpty())
        assertTrue(ConsultationPackageCatalog.packages.all { it.priceVnd > 0L })
    }

    @Test
    fun agribankVietQrUrl_containsOfficialAccountAndTransferNote() {
        val url = LawyerContact.buildAgribankVietQrUrl(500_000L, "MGL123456")
        assertTrue(url.contains("4801215011146"))
        assertTrue(url.contains("970405"))
        assertTrue(url.contains("500000"))
        assertTrue(url.contains("MGL123456"))
        assertEquals("0915855111", LawyerContact.ZALO_PHONE)
    }

    @Test
    fun sanitizeConversation_handlesConsecutiveAndTrailingUserTurns() {
        val history = listOf(
            ConsultationMessage(id = "0", sender = "advisor", content = "Chào bạn"),
            ConsultationMessage(id = "1", sender = "user", content = "Câu 1"),
            ConsultationMessage(id = "2", sender = "advisor", content = "Trả lời 1"),
            ConsultationMessage(id = "3", sender = "user", content = "Câu 2 chưa được đáp")
        )

        val sanitized = LegalAiRepository.sanitizeConversation(
            history = history,
            newUserPrompt = "[Luật Dân sự] Câu hỏi mới",
            maxMessages = 6,
            maxChars = 600
        )

        assertEquals(2, sanitized.history.size)
        assertEquals("user", sanitized.history[0].role)
        assertEquals("model", sanitized.history[1].role)
        assertTrue(sanitized.userPrompt.contains("Câu 2 chưa được đáp"))
        assertTrue(sanitized.userPrompt.contains("Câu hỏi mới"))
    }
}
