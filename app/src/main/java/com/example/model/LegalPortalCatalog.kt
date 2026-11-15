package com.example.model

import java.net.URLEncoder

enum class LegalPortalGroup(val title: String, val subtitle: String) {
    OFFICIAL_STATE(
        title = "1. Các Cổng thông tin chính thức của Nhà nước (Miễn phí)",
        subtitle = "Nguồn dữ liệu gốc, chuẩn xác từ Bộ Tư pháp, Chính phủ, Công báo và Quốc hội"
    ),
    SPECIALIZED(
        title = "2. Các trang tra cứu chuyên ngành (Tính năng nâng cao)",
        subtitle = "Xem lược đồ văn bản, phân tích hiệu lực, so sánh điểm mới và văn bản song ngữ"
    )
}

data class LegalPortalSource(
    val id: String,
    val group: LegalPortalGroup,
    val name: String,
    val domain: String,
    val baseUrl: String,
    val searchUrlTemplate: String,
    val badge: String,
    val description: String,
    val highlights: List<String>
) {
    fun buildSearchUrl(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return baseUrl
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        return searchUrlTemplate.replace("{QUERY}", encoded)
    }
}

data class PortalHeadline(
    val portalId: String,
    val portalDomain: String,
    val title: String,
    val url: String
)

data class PortalSyncStatus(
    val portalId: String,
    val portalName: String,
    val domain: String,
    val group: LegalPortalGroup,
    val isReachable: Boolean = false,
    val httpCode: Int? = null,
    val latencyMs: Long = 0L,
    val pageTitle: String = "",
    val headlines: List<PortalHeadline> = emptyList(),
    val syncedAtMillis: Long = 0L,
    val errorNote: String = ""
)

data class LegalSyncUiState(
    val autoSyncEnabled: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncMillis: Long = 0L,
    val portalStatuses: List<PortalSyncStatus> = emptyList(),
    val errorMessage: String? = null
) {
    val reachableCount: Int
        get() = portalStatuses.count { it.isReachable }

    val totalCount: Int
        get() = LegalPortalCatalog.portals.size

    val combinedHeadlines: List<PortalHeadline>
        get() = portalStatuses
            .flatMap { it.headlines }
            .distinctBy { it.title.lowercase() }
            .take(12)
}

object LegalPortalCatalog {
    val portals: List<LegalPortalSource> = listOf(
        LegalPortalSource(
            id = "vbpl",
            group = LegalPortalGroup.OFFICIAL_STATE,
            name = "Cơ sở dữ liệu quốc gia về pháp luật",
            domain = "vbpl.vn",
            baseUrl = "https://vbpl.vn/pages/portal.aspx",
            searchUrlTemplate = "https://vbpl.vn/TW/Pages/vanban.aspx?Keyword={QUERY}",
            badge = "Bộ Tư pháp • Nguồn gốc",
            description = "Nguồn dữ liệu gốc và chuẩn xác nhất do Bộ Tư pháp quản lý. Cập nhật đầy đủ văn bản từ Trung ương đến địa phương, trạng thái hiệu lực rõ ràng và văn bản hợp nhất.",
            highlights = listOf("Trạng thái hiệu lực", "Lược đồ văn bản", "Văn bản hợp nhất")
        ),
        LegalPortalSource(
            id = "chinhphu",
            group = LegalPortalGroup.OFFICIAL_STATE,
            name = "Cổng Thông tin điện tử Chính phủ (Văn bản)",
            domain = "chinhphu.vn",
            baseUrl = "https://vanban.chinhphu.vn/",
            searchUrlTemplate = "https://vanban.chinhphu.vn/?pageid=27160&search={QUERY}",
            badge = "Chính phủ & Thủ tướng",
            description = "Nơi đăng tải nhanh nhất các Nghị định, Quyết định, Chỉ thị mới ban hành của Chính phủ và Thủ tướng Chính phủ.",
            highlights = listOf("Nghị định mới nhất", "Quyết định Thủ tướng", "Chỉ thị điều hành")
        ),
        LegalPortalSource(
            id = "congbao",
            group = LegalPortalGroup.OFFICIAL_STATE,
            name = "Công báo nước CHXHCN Việt Nam",
            domain = "congbao.chinhphu.vn",
            baseUrl = "https://congbao.chinhphu.vn/",
            searchUrlTemplate = "https://congbao.chinhphu.vn/tim-kiem?keyword={QUERY}",
            badge = "Căn cứ hiệu lực",
            description = "Dùng để kiểm tra ngày đăng Công báo chính thức — căn cứ pháp lý quan trọng để xác định chính xác thời điểm có hiệu lực của văn bản.",
            highlights = listOf("Ngày đăng Công báo", "Số Công báo điện tử", "Xác định hiệu lực")
        ),
        LegalPortalSource(
            id = "quochoi",
            group = LegalPortalGroup.OFFICIAL_STATE,
            name = "Cổng Thông tin điện tử Quốc hội",
            domain = "quochoi.vn",
            baseUrl = "https://quochoi.vn/",
            searchUrlTemplate = "https://quochoi.vn/tim-kiem/Pages/home.aspx?k={QUERY}",
            badge = "Luật & Nghị quyết",
            description = "Nguồn tra cứu các dự thảo Luật, Nghị quyết đang lấy ý kiến nhân dân cũng như các đạo luật mới được Quốc hội thông qua.",
            highlights = listOf("Dự thảo Luật", "Nghị quyết Quốc hội", "Đạo luật mới thông qua")
        ),
        LegalPortalSource(
            id = "thuvienphapluat",
            group = LegalPortalGroup.SPECIALIZED,
            name = "Thư Viện Pháp Luật",
            domain = "thuvienphapluat.vn",
            baseUrl = "https://thuvienphapluat.vn/",
            searchUrlTemplate = "https://thuvienphapluat.vn/page/tim-van-ban.aspx?keyword={QUERY}",
            badge = "Lược đồ & Điểm mới",
            description = "Hệ thống phân tích hiệu lực trực quan, xem lược đồ văn bản (văn bản hướng dẫn, văn bản bị thay thế), cập nhật nhanh và thường xuyên có bài tổng hợp điểm mới của luật.",
            highlights = listOf("Tab Lược đồ trực quan", "Tổng hợp điểm mới", "Văn bản hướng dẫn")
        ),
        LegalPortalSource(
            id = "luatvietnam",
            group = LegalPortalGroup.SPECIALIZED,
            name = "Luật Việt Nam",
            domain = "luatvietnam.vn",
            baseUrl = "https://luatvietnam.vn/",
            searchUrlTemplate = "https://luatvietnam.vn/tim-van-ban.html?keywords={QUERY}",
            badge = "Song ngữ & Cảnh báo",
            description = "Hỗ trợ tốt việc tra cứu tình trạng hiệu lực, tải file văn bản song ngữ tiếng Anh và nhận thông báo tự động qua email khi văn bản quan tâm thay đổi.",
            highlights = listOf("Tình trạng hiệu lực", "Văn bản tiếng Anh", "Cảnh báo thay đổi")
        )
    )

    fun buildVbplLookupUrl(query: String): String {
        return portals.first { it.id == "vbpl" }.buildSearchUrl(query)
    }

    fun buildTvplLookupUrl(query: String): String {
        return portals.first { it.id == "thuvienphapluat" }.buildSearchUrl(query)
    }

    fun buildConsolidatedLookupUrl(documentTitleOrCode: String): String {
        return buildVbplLookupUrl("Văn bản hợp nhất $documentTitleOrCode")
    }
}
