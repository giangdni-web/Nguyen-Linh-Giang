package com.example.model

data class LegalDocumentItem(
    val id: String,
    val category: LegalCategory,
    val codeNumber: String,
    val title: String,
    val effectiveDate: String,
    val summary: String,
    val keyArticles: List<LegalArticleExcerpt>
)

data class LegalArticleExcerpt(
    val articleNumber: String,
    val heading: String,
    val content: String
)

object LegalDocumentCatalog {
    val documents: List<LegalDocumentItem> = listOf(
        LegalDocumentItem(
            id = "blds_2015",
            category = LegalCategory.CIVIL,
            codeNumber = "Luật số 91/2015/QH13",
            title = "Bộ luật Dân sự năm 2015",
            effectiveDate = "Hiệu lực từ 01/01/2017",
            summary = "Quy định địa vị pháp lý, chuẩn mực pháp lý về cách ứng xử của cá nhân, pháp nhân; quyền, nghĩa vụ về nhân thân và tài sản trong quan hệ dân sự.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Điều 117",
                    heading = "Điều kiện có hiệu lực của giao dịch dân sự",
                    content = "Giao dịch dân sự có hiệu lực khi có đủ các điều kiện: Chủ thể có năng lực pháp luật dân sự, năng lực hành vi dân sự phù hợp; chủ thể tham gia hoàn toàn tự nguyện; mục đích và nội dung không vi phạm điều cấm của luật, không trái đạo đức xã hội; hình thức là điều kiện có hiệu lực nếu luật có quy định."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 429",
                    heading = "Thời hiệu khởi kiện về hợp đồng",
                    content = "Thời hiệu khởi kiện để yêu cầu Tòa án giải quyết tranh chấp hợp đồng là 03 năm, kể từ ngày người có quyền yêu cầu biết hoặc phải biết quyền và lợi ích hợp pháp của mình bị xâm phạm."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 623",
                    heading = "Thời hiệu về thừa kế",
                    content = "Thời hiệu để người thừa kế yêu cầu chia di sản là 30 năm đối với bất động sản, 10 năm đối với động sản, kể từ thời điểm mở thừa kế. Thời hiệu yêu cầu xác nhận quyền thừa kế của mình hoặc bác bỏ quyền thừa kế của người khác là 10 năm."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 468",
                    heading = "Lãi suất trong hợp đồng vay tài sản",
                    content = "Trường hợp các bên có thỏa thuận về lãi suất thì lãi suất theo thỏa thuận không được vượt quá 20%/năm của khoản tiền vay, trừ trường hợp luật khác có liên quan quy định khác."
                )
            )
        ),
        LegalDocumentItem(
            id = "blttds_2015",
            category = LegalCategory.CIVIL,
            codeNumber = "Luật số 92/2015/QH13",
            title = "Bộ luật Tố tụng dân sự năm 2015",
            effectiveDate = "Hiệu lực từ 01/07/2016",
            summary = "Quy định trình tự, thủ tục giải quyết các vụ án dân sự, hôn nhân và gia đình, kinh doanh, thương mại, lao động tại Tòa án nhân dân.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Điều 189",
                    heading = "Hình thức, nội dung đơn khởi kiện",
                    content = "Người khởi kiện phải làm đơn khởi kiện theo mẫu quy định, nêu rõ ngày tháng năm, Tòa án nhận đơn, thông tin người khởi kiện, người bị kiện, quyền và lợi ích hợp pháp bị xâm phạm và yêu cầu cụ thể kèm tài liệu chứng cứ."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 273",
                    heading = "Thời hạn kháng cáo bản án sơ thẩm",
                    content = "Thời hạn kháng cáo đối với bản án của Tòa án cấp sơ thẩm là 15 ngày, kể từ ngày tuyên án; đối với đương sự không có mặt tại phiên tòa thì tính từ ngày nhận được bản án hoặc ngày bản án được niêm yết."
                )
            )
        ),
        LegalDocumentItem(
            id = "lthads_2025",
            category = LegalCategory.ENFORCEMENT,
            codeNumber = "Luật số 106/2025/QH15",
            title = "Luật Thi hành án dân sự năm 2025",
            effectiveDate = "Thông qua 05/12/2025 • Hiệu lực từ 01/07/2026",
            summary = "Chính thức thay thế Luật Thi hành án dân sự năm 2008 và các luật sửa đổi, bổ sung trước đó. Quy định nguyên tắc, trình tự, thủ tục thi hành bản án, quyết định dân sự; nhiệm vụ, quyền hạn của Chấp hành viên và cơ chế khiếu nại, tố cáo trong thi hành án dân sự.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Phạm vi & Hiệu lực áp dụng",
                    heading = "Thay thế Luật THADS 2008 từ ngày 01/07/2026",
                    content = "Luật Thi hành án dân sự số 106/2025/QH15 được Quốc hội thông qua ngày 05/12/2025 và có hiệu lực thi hành từ ngày 01/07/2026, thay thế toàn bộ Luật THADS năm 2008 và các luật sửa đổi, bổ sung trước đó nhằm đồng bộ hóa thủ tục thi hành án dân sự."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Yêu cầu & Thời hiệu THA",
                    heading = "Quyền yêu cầu thi hành án và thời hạn tự nguyện",
                    content = "Người được thi hành án và người phải thi hành án có quyền nộp đơn yêu cầu cơ quan thi hành án dân sự có thẩm quyền tổ chức thi hành bản án, quyết định đã có hiệu lực pháp luật theo đúng thời hiệu và thời hạn tự nguyện thi hành án luật định."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Xác minh & Cưỡng chế kê biên",
                    heading = "Xác minh điều kiện thi hành án và xử lý tài sản",
                    content = "Cơ quan THADS và Chấp hành viên thực hiện xác minh điều kiện thi hành án, áp dụng biện pháp bảo đảm, cưỡng chế kê biên, định giá, xử lý tài sản của người phải thi hành án và giải quyết tranh chấp tài sản thi hành án theo Luật số 106/2025/QH15."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Khiếu nại & Tố cáo THADS",
                    heading = "Cơ chế khiếu nại, tố cáo quyết định và hành vi thi hành án",
                    content = "Đương sự, người có quyền lợi, nghĩa vụ liên quan có quyền khiếu nại quyết định, hành vi trái pháp luật của Thủ trưởng cơ quan THADS, Chấp hành viên; công dân có quyền tố cáo vi phạm pháp luật trong thi hành án dân sự để bảo vệ quyền, lợi ích hợp pháp."
                )
            )
        ),
        LegalDocumentItem(
            id = "nd_152_2026",
            category = LegalCategory.ENFORCEMENT,
            codeNumber = "Nghị định 152/2026/NĐ-CP",
            title = "Hướng dẫn thi hành một số điều của Luật THADS năm 2025",
            effectiveDate = "Hiệu lực từ 01/07/2026",
            summary = "Quy định chi tiết và hướng dẫn thi hành Luật Thi hành án dân sự số 106/2025/QH15 về tổ chức thi hành án, xác minh tài sản, định giá, bán đấu giá và xử lý tài sản chung.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Xác minh & Phối hợp liên ngành",
                    heading = "Cung cấp thông tin tài sản, tài khoản của người phải thi hành án",
                    content = "Quy định chi tiết trách nhiệm phối hợp của cơ quan đăng ký đất đai, tổ chức tín dụng, ngân hàng và các cơ quan liên quan trong việc cung cấp thông tin, phong tỏa tài khoản và ngăn chặn tẩu tán tài sản thi hành án."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Xử lý tài sản chung",
                    heading = "Thủ tục kê biên, phân chia phần sở hữu chung vợ chồng, hộ gia đình",
                    content = "Hướng dẫn trình tự thông báo cho đồng sở hữu thỏa thuận phân chia hoặc yêu cầu Tòa án xác định phần quyền sở hữu tài sản chung trước khi tiến hành cưỡng chế, định giá và đấu giá tài sản theo Nghị định 152/2026/NĐ-CP."
                )
            )
        ),
        LegalDocumentItem(
            id = "ltttp_2025",
            category = LegalCategory.ENFORCEMENT,
            codeNumber = "Luật số 102/2025/QH15",
            title = "Luật Tương trợ tư pháp về dân sự năm 2025",
            effectiveDate = "Hiệu lực từ 01/07/2026",
            summary = "Văn bản liên quan trực tiếp đến tố tụng dân sự, tương trợ tư pháp về dân sự và phối hợp liên ngành trong thi hành bản án, quyết định dân sự có yếu tố nước ngoài.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Phối hợp & Tương trợ tư pháp",
                    heading = "Tống đạt văn bản, thu thập chứng cứ và thi hành án có yếu tố nước ngoài",
                    content = "Luật Tương trợ tư pháp về dân sự số 102/2025/QH15 có hiệu lực từ 01/07/2026 quy định nguyên tắc, thẩm quyền và trình tự phối hợp liên ngành khi tống đạt giấy tờ tố tụng, thu thập chứng cứ và ủy thác tư pháp phục vụ giải quyết, thi hành các vụ việc dân sự."
                )
            )
        ),
        LegalDocumentItem(
            id = "ldd_2024",
            category = LegalCategory.LAND,
            codeNumber = "Luật số 31/2024/QH15",
            title = "Luật Đất đai năm 2024",
            effectiveDate = "Hiệu lực từ 01/08/2024",
            summary = "Luật Đất đai mới nhất quy định quyền và nghĩa vụ người sử dụng đất, cấp Giấy chứng nhận (Sổ đỏ), bảng giá đất sát thị trường, thu hồi bồi thường và giải quyết tranh chấp đất đai.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Điều 137 & 138",
                    heading = "Cấp Giấy chứng nhận đối với đất sử dụng ổn định không có giấy tờ",
                    content = "Hộ gia đình, cá nhân đang sử dụng đất ổn định từ trước ngày 01/07/2014 không có giấy tờ về quyền sử dụng đất mà không vi phạm pháp luật đất đai, không thuộc trường hợp giao đất trái thẩm quyền được xem xét cấp Sổ đỏ theo hạn mức và điều kiện quy định."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 235",
                    heading = "Hòa giải tranh chấp đất đai",
                    content = "Nhà nước khuyến khích các bên tranh chấp đất đai tự hòa giải hoặc thông qua hòa giải ở cơ sở. Trước khi cơ quan có thẩm quyền giải quyết, các bên tranh chấp phải thực hiện hòa giải tại UBND cấp xã nơi có đất tranh chấp (thời hạn giải quyết không quá 30 ngày)."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 236",
                    heading = "Thẩm quyền giải quyết tranh chấp đất đai",
                    content = "Tranh chấp đất đai mà đương sự có Giấy chứng nhận hoặc có giấy tờ theo Điều 137 và tranh chấp tài sản gắn liền với đất do Tòa án nhân dân giải quyết. Trường hợp không có giấy tờ, đương sự được lựa chọn nộp đơn đề nghị UBND cấp có thẩm quyền giải quyết hoặc khởi kiện tại Tòa án."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 91 & 95",
                    heading = "Nguyên tắc bồi thường về đất khi Nhà nước thu hồi đất",
                    content = "Việc bồi thường khi Nhà nước thu hồi đất phải bảo đảm người có đất thu hồi có chỗ ở, thu nhập và điều kiện sống bằng hoặc tốt hơn nơi ở cũ; giá đất tính bồi thường là giá đất cụ thể theo thị trường tại thời điểm quyết định thu hồi."
                )
            )
        ),
        LegalDocumentItem(
            id = "nd_101_2024",
            category = LegalCategory.LAND,
            codeNumber = "Nghị định 101/2024/NĐ-CP",
            title = "Đăng ký đất đai, tài sản gắn liền với đất & Cấp Sổ đỏ",
            effectiveDate = "Hiệu lực từ 01/08/2024",
            summary = "Quy định hồ sơ, trình tự, thủ tục đăng ký biến động đất đai, tách thửa, hợp thửa, sang tên chuyển nhượng và cấp mới Giấy chứng nhận quyền sử dụng đất.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Điều 22",
                    heading = "Thời gian thực hiện thủ tục đăng ký đất đai, cấp Giấy chứng nhận",
                    content = "Đăng ký đất đai, tài sản gắn liền với đất lần đầu không quá 20 ngày làm việc; cấp Giấy chứng nhận lần đầu không quá 03 ngày làm việc; đăng ký biến động chuyển nhượng, tặng cho, thừa kế không quá 10 ngày làm việc."
                )
            )
        ),
        LegalDocumentItem(
            id = "lhngd_2014",
            category = LegalCategory.FAMILY,
            codeNumber = "Luật số 52/2014/QH13",
            title = "Luật Hôn nhân và Gia đình năm 2014",
            effectiveDate = "Hiệu lực từ 01/01/2015",
            summary = "Điều chỉnh chế độ tài sản vợ chồng, điều kiện kết hôn, ly hôn thuận tình và đơn phương, quyền nuôi con và nghĩa vụ cấp dưỡng sau ly hôn.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Điều 33 & 43",
                    heading = "Tài sản chung và tài sản riêng của vợ, chồng",
                    content = "Tài sản chung gồm tài sản do vợ, chồng tạo ra, thu nhập do lao động, sản xuất kinh doanh trong thời kỳ hôn nhân; tài sản được thừa kế chung hoặc tặng cho chung. Tài sản riêng gồm tài sản có trước kết hôn, được thừa kế riêng, tặng cho riêng."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 55 & 56",
                    heading = "Thuận tình ly hôn và Ly hôn theo yêu cầu của một bên",
                    content = "Khi vợ chồng cùng yêu cầu ly hôn và đã thỏa thuận về chia tài sản, trông nom, nuôi dưỡng con trên cơ sở bảo đảm quyền lợi chính đáng thì Tòa án công nhận thuận tình ly hôn. Ly hôn đơn phương được giải quyết khi hôn nhân lâm vào tình trạng trầm trọng, đời sống chung không thể kéo dài."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 59",
                    heading = "Nguyên tắc giải quyết tài sản của vợ chồng khi ly hôn",
                    content = "Tài sản chung của vợ chồng được chia đôi nhưng có tính đến các yếu tố: hoàn cảnh của gia đình và của vợ, chồng; công sức đóng góp vào việc tạo lập, duy trì và phát triển khối tài sản chung; bảo vệ lợi ích chính đáng; lỗi của mỗi bên trong vi phạm quyền, nghĩa vụ."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 81 - 84",
                    heading = "Việc trông nom, chăm sóc, nuôi dưỡng, giáo dục con sau khi ly hôn",
                    content = "Con dưới 36 tháng tuổi được giao cho mẹ trực tiếp nuôi, trừ trường hợp người mẹ không đủ điều kiện. Con từ đủ 07 tuổi trở lên phải xem xét nguyện vọng của con. Người không trực tiếp nuôi con có nghĩa vụ cấp dưỡng cho con."
                )
            )
        ),
        LegalDocumentItem(
            id = "nq_01_2024",
            category = LegalCategory.FAMILY,
            codeNumber = "Nghị quyết 01/2024/NQ-HĐTP",
            title = "Hướng dẫn áp dụng quy định pháp luật giải quyết vụ việc Hôn nhân & Gia đình",
            effectiveDate = "Hiệu lực từ 01/07/2024",
            summary = "Hướng dẫn chi tiết của Hội đồng Thẩm phán TANDTC về căn cứ cho ly hôn, xác định nghĩa vụ cấp dưỡng, chia tài sản chung và bảo vệ phụ nữ, trẻ em.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Điều 7",
                    heading = "Mức cấp dưỡng cho con chưa thành niên",
                    content = "Mức cấp dưỡng do các bên thỏa thuận căn cứ vào thu nhập, khả năng thực tế của người có nghĩa vụ cấp dưỡng và nhu cầu thiết yếu của người được cấp dưỡng; nếu không thỏa thuận được, Tòa án quyết định mức cấp dưỡng cụ thể hàng tháng bảo đảm nhu cầu thiết yếu của con."
                )
            )
        ),
        LegalDocumentItem(
            id = "blhs_2015",
            category = LegalCategory.CRIMINAL,
            codeNumber = "Luật số 100/2015/QH13",
            title = "Bộ luật Hình sự năm 2015 (sửa đổi, bổ sung 2017)",
            effectiveDate = "Hiệu lực từ 01/01/2018",
            summary = "Quy định về tội phạm và hình phạt; các tội xâm phạm sở hữu, trật tự quản lý kinh tế, chức vụ và căn cứ miễn, giảm trách nhiệm hình sự.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Điều 174",
                    heading = "Tội lừa đảo chiếm đoạt tài sản",
                    content = "Người nào bằng thủ đoạn gian dối chiếm đoạt tài sản của người khác trị giá từ 2.000.000 đồng trở lên hoặc dưới 2.000.000 đồng nhưng thuộc trường hợp luật định thì bị truy cứu trách nhiệm hình sự với khung hình phạt tương ứng theo giá trị tài sản và tình tiết tăng nặng."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 175",
                    heading = "Tội lạm dụng tín nhiệm chiếm đoạt tài sản",
                    content = "Vay, mượn, thuê tài sản hoặc nhận được tài sản bằng hình thức hợp đồng rồi dùng thủ đoạn gian dối hoặc bỏ trốn để chiếm đoạt tài sản đó, hoặc sử dụng tài sản vào mục đích bất hợp pháp dẫn đến không có khả năng trả lại."
                )
            )
        ),
        LegalDocumentItem(
            id = "bltths_2015",
            category = LegalCategory.CRIMINAL,
            codeNumber = "Luật số 101/2015/QH13",
            title = "Bộ luật Tố tụng hình sự năm 2015",
            effectiveDate = "Hiệu lực từ 01/01/2018",
            summary = "Quy định trình tự, thủ tục tiếp nhận, giải quyết tố giác, tin báo về tội phạm; khởi tố, điều tra, truy tố, xét xử và quyền của bị hại, đương sự.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Điều 145 - 147",
                    heading = "Tiếp nhận và thời hạn giải quyết tố giác, tin báo về tội phạm",
                    content = "Cơ quan điều tra, Công an xã, phường, thị trấn có trách nhiệm tiếp nhận mọi tố giác, tin báo về tội phạm. Thời hạn kiểm tra, xác minh tố giác là 20 ngày (có thể gia hạn tối đa không quá 02 tháng đối với vụ việc phức tạp)."
                )
            )
        ),
        LegalDocumentItem(
            id = "blld_2019",
            category = LegalCategory.LABOR,
            codeNumber = "Luật số 45/2019/QH14",
            title = "Bộ luật Lao động năm 2019",
            effectiveDate = "Hiệu lực từ 01/01/2021",
            summary = "Điều chỉnh hợp đồng lao động, tiền lương, thời giờ làm việc, kỷ luật lao động, chấm dứt hợp đồng lao động và giải quyết tranh chấp lao động.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Điều 35 & 36",
                    heading = "Quyền đơn phương chấm dứt hợp đồng lao động",
                    content = "Người lao động có quyền đơn phương chấm dứt HĐLĐ khi báo trước theo thời hạn luật định (45 ngày với HĐLĐ không xác định thời hạn, 30 ngày với HĐLĐ 12-36 tháng) hoặc nghỉ ngay không cần báo trước trong một số trường hợp bị ngược đãi, không trả đủ lương."
                ),
                LegalArticleExcerpt(
                    articleNumber = "Điều 41",
                    heading = "Nghĩa vụ khi đơn phương chấm dứt HĐLĐ trái pháp luật",
                    content = "Người sử dụng lao động chấm dứt HĐLĐ trái pháp luật phải nhận người lao động trở lại làm việc, trả tiền lương, đóng BHXH, BHYT, BHTN trong những ngày không được làm việc và trả thêm ít nhất 02 tháng tiền lương theo hợp đồng."
                )
            )
        ),
        LegalDocumentItem(
            id = "lbhxh_2024",
            category = LegalCategory.LABOR,
            codeNumber = "Luật số 41/2024/QH15",
            title = "Luật Bảo hiểm xã hội năm 2024",
            effectiveDate = "Hiệu lực từ 01/07/2025",
            summary = "Quy định chế độ ốm đau, thai sản, hưu trí, tử tuất, điều kiện rút BHXH một lần và xử lý hành vi chậm đóng, trốn đóng BHXH bắt buộc.",
            keyArticles = listOf(
                LegalArticleExcerpt(
                    articleNumber = "Điều 64 & 70",
                    heading = "Điều kiện hưởng lương hưu và BHXH một lần",
                    content = "Giảm điều kiện số năm đóng BHXH tối thiểu để hưởng lương hưu xuống 15 năm nhằm mở rộng cơ hội hưởng chế độ hưu trí cho người lao động tham gia muộn hoặc không liên tục."
                )
            )
        )
    )
}
