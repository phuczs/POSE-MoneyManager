package com.example.moneymanager.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents one of the 16 Financial DNA archetypes (MBTI-style 4-letter code).
 *
 * Axis 1 (Savings vs Indulgence): S (>= 50) vs I (< 50)
 * Axis 2 (Planner vs Spontaneous): P (>= 50) vs F (< 50)
 * Axis 3 (Consistent vs Volatile): C (>= 50) vs V (< 50)
 * Axis 4 (Targeted vs Diverse):    T (>= 50) vs D (< 50)
 */
enum class FinancialArchetype(
    val code: String,
    val emoji: String,
    val accentColorHex: Long,
    val titleVi: String,
    val titleEn: String,
    val taglineVi: String,
    val taglineEn: String,
    val defaultStrengthsVi: List<String>,
    val defaultStrengthsEn: List<String>,
    val defaultImprovementsVi: List<String>,
    val defaultImprovementsEn: List<String>
) {
    SPCT(
        code = "SPCT",
        emoji = "♟️",
        accentColorHex = 0xFF00796B,
        titleVi = "Chiến lược gia Thận trọng",
        titleEn = "The Cautious Strategist",
        taglineVi = "Kỷ luật thép, chi tiêu có kế hoạch và tối ưu từng đồng tiết kiệm.",
        taglineEn = "Iron discipline, deliberate planning, and laser-focused savings.",
        defaultStrengthsVi = listOf(
            "Tỷ lệ tiết kiệm ổn định và bám sát ngân sách rất tốt",
            "Ít bị chi phối bởi cảm xúc khi mua sắm bốc đồng"
        ),
        defaultStrengthsEn = listOf(
            "Consistent savings rate with strong budget adherence",
            "Rarely swayed by emotional or impulse spending"
        ),
        defaultImprovementsVi = listOf(
            "Cân nhắc dành một phần nhỏ quỹ thưởng cho trải nghiệm cá nhân",
            "Khám phá kênh đầu tư sinh lời thay vì chỉ tích lũy nhàn rỗi"
        ),
        defaultImprovementsEn = listOf(
            "Set aside a small guilt-free fund for personal joy",
            "Put surplus savings to work through diversified investments"
        )
    ),
    SPCD(
        code = "SPCD",
        emoji = "🏛️",
        accentColorHex = 0xFF00897B,
        titleVi = "Kiến trúc sư Cân bằng",
        titleEn = "The Balanced Architect",
        taglineVi = "Giữ vững tiết kiệm trong khi phân bổ hài hòa mọi khía cạnh cuộc sống.",
        taglineEn = "Protects savings while gracefully funding every area of life.",
        defaultStrengthsVi = listOf(
            "Phân bổ chi tiêu đa dạng nhưng vẫn duy trì dư giả đều đặn",
            "Nhịp độ chi tiêu hàng tuần cực kỳ ổn định"
        ),
        defaultStrengthsEn = listOf(
            "Well-rounded spending across categories without breaking savings",
            "Remarkably steady week-to-week cashflow rhythm"
        ),
        defaultImprovementsVi = listOf(
            "Rà soát các danh mục nhỏ lẻ để gom gọn khoản chi định kỳ",
            "Đặt mục tiêu tiết kiệm dài hạn rõ ràng hơn cho khoản thặng dư"
        ),
        defaultImprovementsEn = listOf(
            "Audit smaller recurring categories for hidden subscription leaks",
            "Tie your monthly surplus to concrete long-term milestones"
        )
    ),
    SPVT(
        code = "SPVT",
        emoji = "🎯",
        accentColorHex = 0xFF1565C0,
        titleVi = "Thợ săn Mục tiêu",
        titleEn = "The Goal Hunter",
        taglineVi = "Tích lũy bền bỉ rồi mạnh tay chi cho những cột mốc thật sự xứng đáng.",
        taglineEn = "Saves patiently, then deploys capital boldly on big milestones.",
        defaultStrengthsVi = listOf(
            "Có kế hoạch rõ ràng và biết tập trung nguồn lực vào ưu tiên lớn",
            "Duy trì thặng dư tốt bất chấp các đợt chi tiêu lớn"
        ),
        defaultStrengthsEn = listOf(
            "Clear prioritization with purposeful spending spikes",
            "Maintains healthy net savings despite lumpy expenses"
        ),
        defaultImprovementsVi = listOf(
            "Tạo quỹ dự phòng riêng (sinking fund) để làm mượt các tuần chi mạnh",
            "Theo dõi lịch sử ngân sách quý để dự trù các đợt chi định kỳ"
        ),
        defaultImprovementsEn = listOf(
            "Use sinking funds to smooth out high-spend weeks",
            "Track quarterly budget history to anticipate major outlays"
        )
    ),
    SPVD(
        code = "SPVD",
        emoji = "🧭",
        accentColorHex = 0xFF283593,
        titleVi = "Nhà điều phối Linh hoạt",
        titleEn = "The Agile Orchestrator",
        taglineVi = "Biết kiểm soát tổng thể nhưng sẵn sàng xoay chuyển theo từng giai đoạn.",
        taglineEn = "Masters the big picture while adapting fluidly to life's shifts.",
        defaultStrengthsVi = listOf(
            "Kỷ luật tiết kiệm tốt dù nhu cầu chi tiêu thay đổi linh hoạt",
            "Chủ động thiết lập ngân sách để phòng ngừa rủi ro"
        ),
        defaultStrengthsEn = listOf(
            "Strong savings discipline even with shifting priorities",
            "Proactively uses budgets as financial guardrails"
        ),
        defaultImprovementsVi = listOf(
            "Đặt giới hạn chi tiêu theo tuần cho các danh mục dễ biến động",
            "Bật tự động gia hạn ngân sách để duy trì nhịp kiểm soát"
        ),
        defaultImprovementsEn = listOf(
            "Use weekly budget caps on your most variable categories",
            "Enable auto-renewing budgets to stabilize monthly swings"
        )
    ),
    SFCT(
        code = "SFCT",
        emoji = "🌿",
        accentColorHex = 0xFF2E7D32,
        titleVi = "Người Tối giản Tự nhiên",
        titleEn = "The Natural Minimalist",
        taglineVi = "Không cần lập bảng phức tạp vẫn sống gọn gàng, đều đặn và dư giả.",
        taglineEn = "Effortlessly frugal, steady, and focused without rigid spreadsheets.",
        defaultStrengthsVi = listOf(
            "Thói quen chi tiêu tối giản tự nhiên, ít phát sinh rò rỉ",
            "Dòng tiền hàng tuần đều đặn và dễ dự đoán"
        ),
        defaultStrengthsEn = listOf(
            "Naturally streamlined habits with minimal lifestyle creep",
            "Calm, predictable weekly spending flow"
        ),
        defaultImprovementsVi = listOf(
            "Tạo thêm 2–3 ngân sách cốt lõi để theo dõi tiến độ trực quan hơn",
            "Tận dụng tính năng chuyển dư (rollover) để tối ưu quỹ tiết kiệm"
        ),
        defaultImprovementsEn = listOf(
            "Set up 2–3 core budgets to visualize your healthy margin",
            "Use rollover budgets to channel surplus into goals"
        )
    ),
    SFCD(
        code = "SFCD",
        emoji = "🕊️",
        accentColorHex = 0xFF00838F,
        titleVi = "Người Quản lý Điềm tĩnh",
        titleEn = "The Calm Steward",
        taglineVi = "Tận hưởng đa dạng nhưng luôn giữ bản năng tiết kiệm tỉnh táo.",
        taglineEn = "Enjoys variety with an intuitive, unhurried sense of balance.",
        defaultStrengthsVi = listOf(
            "Cân bằng tự nhiên giữa chi tiêu thường ngày và tích lũy",
            "Nhịp chi tiêu êm ả, ít cú sốc tài chính bất ngờ"
        ),
        defaultStrengthsEn = listOf(
            "Intuitive balance between everyday enjoyment and saving",
            "Smooth spending cadence free of sudden shocks"
        ),
        defaultImprovementsVi = listOf(
            "Thiết lập ngân sách cho 2 danh mục chi lớn nhất để tối ưu thêm 5–10%",
            "Kiểm tra báo cáo dòng tiền hàng tháng để phát hiện xu hướng tăng nhẹ"
        ),
        defaultImprovementsEn = listOf(
            "Add budgets to your top 2 categories to unlock extra 5–10% savings",
            "Review monthly cashflow deltas to catch gradual creep early"
        )
    ),
    SFVT(
        code = "SFVT",
        emoji = "⚡",
        accentColorHex = 0xFF0277BD,
        titleVi = "Nhà Tích lũy Bản năng",
        titleEn = "The Instinctive Saver",
        taglineVi = "Chi tiêu ngẫu hứng theo đợt nhưng luôn dừng đúng lúc để giữ khoản dư.",
        taglineEn = "Spends in bursts on favorite things yet instinctively protects the reserve.",
        defaultStrengthsVi = listOf(
            "Giữ được tỷ lệ tiết kiệm tích cực nhờ tập trung ít danh mục",
            "Nhanh chóng tự điều chỉnh sau các đợt chi tiêu cao"
        ),
        defaultStrengthsEn = listOf(
            "Protects net savings by keeping spending concentrated",
            "Naturally self-corrects after high-spend bursts"
        ),
        defaultImprovementsVi = listOf(
            "Áp dụng quy tắc chờ 24 giờ trước các khoản mua lớn ngẫu hứng",
            "Đặt hạn mức cảnh báo 75% cho danh mục chiếm tỷ trọng cao nhất"
        ),
        defaultImprovementsEn = listOf(
            "Try a 24-hour cooling-off rule before unplanned big purchases",
            "Set a 75% alert threshold on your dominant spending category"
        )
    ),
    SFVD(
        code = "SFVD",
        emoji = "🌊",
        accentColorHex = 0xFF5E35B1,
        titleVi = "Người Tự do Tỉnh thức",
        titleEn = "The Mindful Free-Spirit",
        taglineVi = "Ưa khám phá nhiều trải nghiệm mới nhưng vẫn giữ ví luôn có đệm an toàn.",
        taglineEn = "Explores freely across categories while keeping a safety cushion.",
        defaultStrengthsVi = listOf(
            "Vẫn bảo toàn được khoản tiết kiệm dù lối sống phong phú, linh hoạt",
            "Cởi mở với nhiều trải nghiệm và thích nghi nhanh"
        ),
        defaultStrengthsEn = listOf(
            "Preserves positive savings despite a dynamic, varied lifestyle",
            "Adaptable and open to diverse life experiences"
        ),
        defaultImprovementsVi = listOf(
            "Đặt một hạn mức tổng hàng tuần để giảm biên độ dao động chi tiêu",
            "Tách riêng khoản tiết kiệm ngay đầu tháng trước khi chi tiêu"
        ),
        defaultImprovementsEn = listOf(
            "Adopt a simple weekly spending cap to smooth out volatility",
            "Pay yourself first by setting aside savings at payday"
        )
    ),
    IPCT(
        code = "IPCT",
        emoji = "👑",
        accentColorHex = 0xFFD84315,
        titleVi = "Người Tận hưởng Có Kế hoạch",
        titleEn = "The Curated Connoisseur",
        taglineVi = "Đầu tư mạnh tay vào những đam mê cốt lõi một cách có trật tự và ổn định.",
        taglineEn = "Invests generously in core passions with structure and poise.",
        defaultStrengthsVi = listOf(
            "Biết rõ mình thích gì và quản lý chi tiêu rất nhất quán",
            "Dùng ngân sách bài bản để kiểm soát trải nghiệm sống"
        ),
        defaultStrengthsEn = listOf(
            "Crystal clear on personal priorities with consistent execution",
            "Uses structured budgets to fund a high-quality lifestyle"
        ),
        defaultImprovementsVi = listOf(
            "Thử cắt nhẹ 10% ở danh mục chi lớn nhất để xây quỹ dự phòng",
            "Đặt mục tiêu tỷ lệ tiết kiệm tối thiểu 15% mỗi tháng"
        ),
        defaultImprovementsEn = listOf(
            "Trim 10% from your top category to build a stronger buffer",
            "Aim for a baseline 15% monthly savings rate"
        )
    ),
    IPCD(
        code = "IPCD",
        emoji = "🎨",
        accentColorHex = 0xFFE65100,
        titleVi = "Nghệ sĩ Lối sống",
        titleEn = "The Lifestyle Curator",
        taglineVi = "Chăm chút đều đặn cho nhiều khía cạnh sống phong phú theo nhịp riêng.",
        taglineEn = "Crafted, steady enjoyment spread across a rich array of interests.",
        defaultStrengthsVi = listOf(
            "Chi tiêu đều đặn, ít cú sốc và có ý thức lập kế hoạch",
            "Phân bổ cân đối giữa nhiều nhu cầu cuộc sống"
        ),
        defaultStrengthsEn = listOf(
            "Steady, predictable outflow backed by conscious planning",
            "Balanced attention across many facets of daily life"
        ),
        defaultImprovementsVi = listOf(
            "Chọn ra 2 danh mục 'không thiết yếu' để tối ưu hóa hàng tháng",
            "Theo dõi hạn mức chi tiêu mỗi ngày trên màn hình Ngân sách"
        ),
        defaultImprovementsEn = listOf(
            "Pick 2 discretionary categories to gently trim each month",
            "Lean on your daily safe allowance in the Budget dashboard"
        )
    ),
    IPVT(
        code = "IPVT",
        emoji = "🚀",
        accentColorHex = 0xFFC62828,
        titleVi = "Nhà Đầu tư Trải nghiệm",
        titleEn = "The Bold Experience Seeker",
        taglineVi = "Có khung kế hoạch nhưng sẵn sàng bùng nổ mạnh vào những sở thích lớn.",
        taglineEn = "Plans ahead yet goes all-in when an exciting opportunity appears.",
        defaultStrengthsVi = listOf(
            "Dám chi cho những trải nghiệm giá trị cao và biết tập trung trọng tâm",
            "Đã hình thành thói quen theo dõi ngân sách"
        ),
        defaultStrengthsEn = listOf(
            "Decisive about funding memorable, high-impact priorities",
            "Already practices tracking with active budgets"
        ),
        defaultImprovementsVi = listOf(
            "Thiết lập hạn mức cảnh báo sớm 70% để kiểm soát các tuần bùng chi",
            "Cân bằng lại giữa khoản chi lớn và quỹ dự phòng khẩn cấp"
        ),
        defaultImprovementsEn = listOf(
            "Lower warning alerts to 70% to catch runaway spend weeks early",
            "Pair big-ticket spending with an emergency reserve target"
        )
    ),
    IPVD(
        code = "IPVD",
        emoji = "🌟",
        accentColorHex = 0xFFAD1457,
        titleVi = "Người Dẫn dắt Xu hướng",
        titleEn = "The Trendsetter",
        taglineVi = "Năng động, đa trải nghiệm và luôn sôi nổi với nhiều kế hoạch mới mẻ.",
        taglineEn = "Energetic, multifaceted, and always ready for what's next.",
        defaultStrengthsVi = listOf(
            "Chủ động ghi chép và sử dụng công cụ quản lý ngân sách",
            "Nhạy bén với các trải nghiệm và nhu cầu đa dạng"
        ),
        defaultStrengthsEn = listOf(
            "Actively engages with budgeting tools to stay aware",
            "Dynamic lifestyle spanning many enriching categories"
        ),
        defaultImprovementsVi = listOf(
            "Chuyển sang ngân sách chu kỳ Tuần để kiểm soát dòng tiền sát hơn",
            "Gom nhóm các khoản chi nhỏ để dễ nhận diện khoản nào thực sự cần"
        ),
        defaultImprovementsEn = listOf(
            "Switch volatile categories to Weekly budgets for tighter feedback",
            "Consolidate small expenses to spot what truly brings value"
        )
    ),
    IFCT(
        code = "IFCT",
        emoji = "☕",
        accentColorHex = 0xFFBF360C,
        titleVi = "Tín đồ Thói quen",
        titleEn = "The Ritual Enthusiast",
        taglineVi = "Gắn bó trung thành với vài sở thích quen thuộc hàng ngày đầy thoải mái.",
        taglineEn = "Loyal to favorite daily rituals with an effortless, steady groove.",
        defaultStrengthsVi = listOf(
            "Chi tiêu rất dễ dự đoán nhờ lặp lại ổn định ở vài danh mục quen",
            "Dễ dàng cải thiện chỉ cần tối ưu đúng 1–2 thói quen cốt lõi"
        ),
        defaultStrengthsEn = listOf(
            "Highly predictable habits concentrated in a few familiar areas",
            "Small tweaks to just 1–2 daily rituals yield big savings"
        ),
        defaultImprovementsVi = listOf(
            "Thử tạo ngân sách riêng cho danh mục top 1 để hãm nhẹ 15%",
            "Dùng tính năng Gợi ý ngân sách AI để đặt mốc hợp lý đầu tiên"
        ),
        defaultImprovementsEn = listOf(
            "Create a dedicated budget for your #1 category to trim 15%",
            "Use AI Predictive Budgets to establish realistic starter caps"
        )
    ),
    IFCD(
        code = "IFCD",
        emoji = "🎈",
        accentColorHex = 0xFFEF6C00,
        titleVi = "Người Hưởng thụ Nhịp nhàng",
        titleEn = "The Easygoing Enjoyer",
        taglineVi = "Sống thoải mái, chi tiêu rải đều mỗi ngày theo cảm hứng nhẹ nhàng.",
        taglineEn = "Relaxed, steady everyday enjoyment across a mix of small pleasures.",
        defaultStrengthsVi = listOf(
            "Nhịp chi tiêu đều, hiếm khi phát sinh cú sốc tài chính quá lớn",
            "Lối sống cân bằng, thoải mái"
        ),
        defaultStrengthsEn = listOf(
            "Even spending pace with few extreme financial shocks",
            "Comfortable, well-distributed everyday routine"
        ),
        defaultImprovementsVi = listOf(
            "Cẩn thận hiệu ứng 'cà phê latte' từ nhiều khoản nhỏ cộng dồn",
            "Đặt một hạn mức chi tiêu an toàn mỗi ngày để tăng dần tiền tiết kiệm"
        ),
        defaultImprovementsEn = listOf(
            "Watch out for small daily leaks adding up across categories",
            "Follow a simple daily allowance to steadily grow savings"
        )
    ),
    IFVT(
        code = "IFVT",
        emoji = "🔥",
        accentColorHex = 0xFFD32F2F,
        titleVi = "Tâm hồn Đam mê",
        titleEn = "The Passionate Impulse",
        taglineVi = "Khi đã thích điều gì sẽ chi mạnh tay ngay lập tức đầy nhiệt huyết.",
        taglineEn = "Follows passion wholeheartedly with decisive, high-energy bursts.",
        defaultStrengthsVi = listOf(
            "Quyết đoán, sống hết mình cho sở thích nổi bật nhất",
            "Chỉ cần kiểm soát tốt 1 danh mục trọng điểm là dòng tiền đổi khác ngay"
        ),
        defaultStrengthsEn = listOf(
            "Decisive and passionate about what excites you most",
            "Focusing discipline on a single top category creates huge wins"
        ),
        defaultImprovementsVi = listOf(
            "Thiết lập hạn mức cứng cho danh mục chiếm tỷ trọng cao nhất",
            "Chờ 24h trước khi chốt đơn các khoản chi vượt 500k"
        ),
        defaultImprovementsEn = listOf(
            "Place a firm budget cap on your single largest expense category",
            "Pause 24 hours before confirming large impulse purchases"
        )
    ),
    IFVD(
        code = "IFVD",
        emoji = "🦋",
        accentColorHex = 0xFF8E24AA,
        titleVi = "Nhà Thám hiểm Tự do",
        titleEn = "The Free Explorer",
        taglineVi = "Yêu tự do tuyệt đối, thích trải nghiệm muôn màu và sống trọn khoảnh khắc.",
        taglineEn = "Embraces total spontaneity, curiosity, and living in the moment.",
        defaultStrengthsVi = listOf(
            "Cuộc sống tràn đầy năng lượng, đa dạng trải nghiệm mới mẻ",
            "Việc bắt đầu theo dõi giao dịch đã là bước ngoặt cực kỳ đáng khen"
        ),
        defaultStrengthsEn = listOf(
            "Vibrant, curiosity-driven lifestyle rich in experiences",
            "Tracking your cashflow is already a powerful first step"
        ),
        defaultImprovementsVi = listOf(
            "Áp dụng quy tắc 50/30/20 và tự động trích 10% ngay khi có thu nhập",
            "Bật ngân sách Tuần để có phản hồi nhanh và tránh 'cháy ví' cuối tháng"
        ),
        defaultImprovementsEn = listOf(
            "Start with a 10% auto-save right when income arrives",
            "Try Weekly budgets for fast feedback and calmer month-ends"
        )
    );

    fun title(languageCode: String): String = if (languageCode == "vi") titleVi else titleEn
    fun tagline(languageCode: String): String = if (languageCode == "vi") taglineVi else taglineEn
    fun defaultStrengths(languageCode: String): List<String> =
        if (languageCode == "vi") defaultStrengthsVi else defaultStrengthsEn
    fun defaultImprovements(languageCode: String): List<String> =
        if (languageCode == "vi") defaultImprovementsVi else defaultImprovementsEn

    companion object {
        fun fromCode(code: String): FinancialArchetype =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SPCT
    }
}

/**
 * The 4 behavioral axes that define a user's Financial DNA.
 */
enum class DnaAxis {
    SAVINGS_VS_INDULGENCE,
    PLANNER_VS_SPONTANEOUS,
    CONSISTENT_VS_VOLATILE,
    TARGETED_VS_DIVERSE
}

/**
 * AI-generated personalized narrative & coaching bullets for a Financial DNA profile.
 */
@Immutable
data class FinancialDnaAiNarrative(
    val narrative: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val dnaCode: String,
    val languageCode: String,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Complete Financial DNA profile for a user — combining deterministic 4-axis
 * behavioral scores with optional AI-crafted storytelling.
 */
@Immutable
data class FinancialDNA(
    val code: String,
    val archetype: FinancialArchetype,
    val savingsScore: Int,      // 0..100 (>=50 Saver 'S', <50 Indulger 'I')
    val plannerScore: Int,      // 0..100 (>=50 Planner 'P', <50 Free-spirit 'F')
    val stabilityScore: Int,    // 0..100 (>=50 Consistent 'C', <50 Volatile 'V')
    val focusScore: Int,        // 0..100 (>=50 Targeted 'T', <50 Diverse 'D')
    val savingsRatePercent: Double,
    val weeklyVariancePercent: Double,
    val topCategoriesSharePercent: Double,
    val topCategories: List<String>,
    val analyzedTransactionsCount: Int,
    val analyzedDays: Int,
    val aiNarrative: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val isAiEnhanced: Boolean = false,
    val generatedAt: Long = System.currentTimeMillis(),
    val languageCode: String = "vi"
) {
    fun scoreFor(axis: DnaAxis): Int = when (axis) {
        DnaAxis.SAVINGS_VS_INDULGENCE -> savingsScore
        DnaAxis.PLANNER_VS_SPONTANEOUS -> plannerScore
        DnaAxis.CONSISTENT_VS_VOLATILE -> stabilityScore
        DnaAxis.TARGETED_VS_DIVERSE -> focusScore
    }
}
