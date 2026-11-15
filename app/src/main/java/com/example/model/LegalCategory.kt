package com.example.model

enum class LegalCategory(
    val key: String,
    val displayName: String,
    val shortName: String,
    val subtitle: String,
    val samplePrompt: String
) {
    CIVIL(
        key = "civil",
        displayName = "Luật Dân sự",
        shortName = "Dân sự",
        subtitle = "Hợp đồng, thừa kế, vay mượn, bồi thường thiệt hại",
        samplePrompt = "Tôi muốn hỏi về thời hiệu khởi kiện đòi nợ theo hợp đồng vay tài sản và thừa kế di sản không có di chúc."
    ),
    CRIMINAL(
        key = "criminal",
        displayName = "Luật Hình sự",
        shortName = "Hình sự",
        subtitle = "Bộ luật Hình sự 2015 (sửa đổi 2017), tố tụng hình sự, bảo lãnh & tố giác",
        samplePrompt = "Quy trình trình báo tố giác tội phạm lừa đảo chiếm đoạt tài sản và quyền có người bảo vệ quyền lợi hợp pháp theo Bộ luật Tố tụng hình sự thế nào?"
    ),
    LABOR(
        key = "labor",
        displayName = "Luật Lao động",
        shortName = "Lao động",
        subtitle = "Bộ luật Lao động 2019, hợp đồng, chấm dứt HĐLĐ, BHXH 2024",
        samplePrompt = "Người sử dụng lao động đơn phương chấm dứt hợp đồng trái pháp luật thì người lao động được bồi thường và trợ cấp những khoản nào?"
    ),
    ENFORCEMENT(
        key = "enforcement",
        displayName = "Luật Thi hành án dân sự",
        shortName = "Thi hành án",
        subtitle = "Luật 106/2025/QH15, NĐ 152/2026/NĐ-CP, khiếu nại THADS",
        samplePrompt = "Theo Luật Thi hành án dân sự số 106/2025/QH15 và Nghị định 152/2026/NĐ-CP, thủ tục yêu cầu thi hành án và xử lý khi bên phải thi hành án tẩu tán tài sản thực hiện thế nào?"
    ),
    LAND(
        key = "land",
        displayName = "Luật Đất đai",
        shortName = "Đất đai",
        subtitle = "Sổ đỏ, tranh chấp ranh giới, thu hồi & bồi thường 2024",
        samplePrompt = "Thủ tục hòa giải tranh chấp ranh giới đất đai tại UBND xã theo Luật Đất đai 2024 như thế nào?"
    ),
    FAMILY(
        key = "family",
        displayName = "Luật Hôn nhân & Gia đình",
        shortName = "Hôn nhân",
        subtitle = "Ly hôn, chia tài sản chung, quyền nuôi con & cấp dưỡng",
        samplePrompt = "Khi ly hôn đơn phương, tài sản hình thành trước hôn nhân nhưng sửa chữa bằng tiền chung được chia thế nào?"
    ),
    GENERAL(
        key = "general",
        displayName = "Tư vấn tổng hợp",
        shortName = "Tổng hợp",
        subtitle = "Hồ sơ pháp lý liên ngành & thủ tục tố tụng",
        samplePrompt = "Tôi cần Luật gia tư vấn lộ trình giải quyết hồ sơ pháp lý liên quan nhiều lĩnh vực."
    );

    companion object {
        fun fromKey(key: String): LegalCategory =
            entries.firstOrNull { it.key == key } ?: GENERAL

        val lawDomains: List<LegalCategory> = listOf(
            CIVIL,
            CRIMINAL,
            LABOR,
            ENFORCEMENT,
            LAND,
            FAMILY
        )
    }
}
