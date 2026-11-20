package com.example.moneymanager.domain.model

enum class ChallengeStatus {
    SUGGESTED,  // Đề xuất từ AI, người dùng có thể bấm "Tham gia thử thách"
    ACTIVE,     // Đang tham gia thử thách
    COMPLETED,  // Đã hoàn thành (đạt targetDays)
    FAILED      // Không hoàn thành hoặc đã quá hạn
}

/**
 * Domain model for Gamified Savings Challenges (Thử thách Tiết kiệm Game hóa).
 *
 * Tracks user progress, streaks, badges, and real-time saved amount calculated
 * by comparing actual transactions against historical spending baselines.
 */
data class SavingsChallenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",              // "Food & Beverage", "Shopping", "Entertainment", hoặc "" (tất cả)
    val keywordFilter: String = "",         // Regex pattern (vd: "cafe|coffee|trà sữa|highlands|starbucks")
    val targetDays: Int = 7,                // 5, 7, 14, 30 ngày
    val currentStreak: Int = 0,             // Số ngày liên tiếp giữ vững không vi phạm
    val dailyBudgetLimit: Double = 0.0,     // Ngân sách tối đa mỗi ngày trong category (0.0 = kiêng tuyệt đối)
    val baselineDailyAverage: Double = 0.0, // Mức chi trung bình trước đây/ngày cho mục này
    val savedAmount: Double = 0.0,          // Tiền thực tế giữ lại được = baseline * streak - actualSpend
    val targetSavings: Double = 0.0,        // Kỳ vọng tiết kiệm = baselineDailyAverage * targetDays
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
    val status: ChallengeStatus = ChallengeStatus.ACTIVE,
    val badgeName: String = "Tiết Kiệm Thông Thái",
    val badgeIcon: String = "🏆",           // Emoji icon (vd: "☕", "🍳", "🛍️", "🛡️", "🔥")
    val daysCompleted: Int = 0
) {
    val progress: Float
        get() = if (targetDays > 0) (currentStreak.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f) else 0f

    val isFinished: Boolean
        get() = status == ChallengeStatus.COMPLETED || status == ChallengeStatus.FAILED
}
