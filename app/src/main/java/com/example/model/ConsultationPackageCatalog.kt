package com.example.model

data class ConsultationPackage(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationLabel: String,
    val priceVnd: Long,
    val priceFormatted: String,
    val badge: String?,
    val features: List<String>
)

object ConsultationPackageCatalog {
    val packages: List<ConsultationPackage> = listOf(
        ConsultationPackage(
            id = "pkg_quick_30",
            title = "Tư vấn chuyên sâu 1-1 qua Zalo / Hotline",
            subtitle = "Trao đổi trực tiếp với Luật gia Nguyễn Linh Giang",
            durationLabel = "30 phút trực tuyến",
            priceVnd = 500_000L,
            priceFormatted = "500.000 đ",
            badge = "Phổ biến",
            features = listOf(
                "Phân tích trực tiếp tình huống Dân sự, Đất đai, Hôn nhân, THADS",
                "Định hướng phương án pháp lý & chứng cứ cần thu thập",
                "Ghi chú tóm tắt giải pháp gửi lại qua Zalo 0915855111"
            )
        ),
        ConsultationPackage(
            id = "pkg_land_contract",
            title = "Rà soát hồ sơ Đất đai & Hợp đồng Dân sự",
            subtitle = "Kiểm tra tính pháp lý Sổ đỏ, di chúc, hợp đồng đặt cọc/chuyển nhượng",
            durationLabel = "Trong 24 giờ làm việc",
            priceVnd = 1_500_000L,
            priceFormatted = "1.500.000 đ",
            badge = "An toàn giao dịch",
            features = listOf(
                "Đánh giá rủi ro pháp lý chi tiết từng điều khoản hợp đồng",
                "Kiểm tra điều kiện chuyển nhượng, thừa kế theo Luật Đất đai 2024",
                "Báo cáo chỉnh sửa bảo vệ tối đa quyền lợi khách hàng"
            )
        ),
        ConsultationPackage(
            id = "pkg_enforcement_complaint",
            title = "Soạn thảo Đơn Khiếu nại / Tố cáo THADS & Khởi kiện",
            subtitle = "Chuyên sâu Thi hành án dân sự từ Nguyên Trưởng phòng Kiểm tra & Giải quyết KN-TC",
            durationLabel = "Trọn gói hồ sơ đơn từ",
            priceVnd = 2_000_000L,
            priceFormatted = "2.000.000 đ",
            badge = "Chuyên môn cao",
            features = listOf(
                "Soạn đơn yêu cầu THA, đơn khiếu nại/tố cáo đúng quy định Luật THADS",
                "Lập danh mục hồ sơ chứng cứ kèm theo đúng trình tự cơ quan THADS",
                "Hướng dẫn gửi đơn và theo dõi thời hạn giải quyết theo luật định"
            )
        ),
        ConsultationPackage(
            id = "pkg_full_case",
            title = "Gói Đồng hành Giải quyết Tranh chấp Toàn diện",
            subtitle = "Nghiên cứu toàn bộ hồ sơ vụ việc & xây dựng chiến lược tố tụng",
            durationLabel = "Theo dõi 30 ngày",
            priceVnd = 5_000_000L,
            priceFormatted = "5.000.000 đ",
            badge = "Toàn diện",
            features = listOf(
                "Nghiên cứu toàn bộ tài liệu vụ án Đất đai, Ly hôn, Thừa kế hoặc THADS",
                "Xây dựng kịch bản hòa giải, làm việc với cơ quan chức năng & Tòa án",
                "Hỗ trợ tư vấn liên tục 30 ngày cùng Luật gia Nguyễn Linh Giang"
            )
        )
    )
}
