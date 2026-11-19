package com.example.moneymanager.util

import com.example.moneymanager.util.toCurrencyString

object PromptUtils {

    fun getFinancialAdvisorPrompt(
        totalIncome: Double,
        totalExpense: Double,
        balance: Double,
        topExpenses: String,
        budgetAnalysis: String,
        languageCode: String = "vi"
    ): String {
        val currencyFormat = if (languageCode == "vi") "VND" else "USD"
        return if (languageCode == "vi") {
            """
            Bạn là trợ lý tài chính thông minh (AI Financial Advisor).
            HỒ SƠ TÀI CHÍNH THÁNG NÀY:
            - Tổng thu: ${totalIncome.toCurrencyString(currencyFormat)}
            - Tổng chi: ${totalExpense.toCurrencyString(currencyFormat)}
            - Số dư: ${balance.toCurrencyString(currencyFormat)}
            
            TOP CHI TIÊU:
            $topExpenses
            $budgetAnalysis
            
            NHIỆM VỤ:
            - Tư vấn ngắn gọn, hữu ích dựa trên số liệu thực tế.
            - Cảnh báo nếu chi tiêu vượt mức.
            - Trả lời thân thiện bằng Tiếng Việt.
            """.trimIndent()
        } else {
            """
            You are a smart AI Financial Advisor.
            CURRENT MONTH FINANCIAL PROFILE:
            - Total Income: ${totalIncome.toCurrencyString("USD")}
            - Total Expense: ${totalExpense.toCurrencyString("USD")}
            - Balance: ${balance.toCurrencyString("USD")}
            
            TOP EXPENSES:
            $topExpenses
            $budgetAnalysis
            
            TASK:
            - Provide concise, useful advice based on actual data.
            - Warn if spending is excessive.
            - Reply in English.
            """.trimIndent()
            }
    }

    fun getQuickAddPrompt(input: String, categoryNames: String): String {
        return """
            Bạn là công cụ trích xuất dữ liệu giao dịch tài chính từ văn bản tiếng Việt.
                    
                    INPUT: "$input"
                    DANH MỤC HỆ THỐNG: [$categoryNames, General, Salary, Bonus, Food & Drinks, Shopping]
                    
                    NHIỆM VỤ: Trả về JSON với các trường: "amount", "type", "category", "description".
                    
                    1. QUY TẮC PHÂN LOẠI "type" (Rất quan trọng):
                       - INCOME (Thu nhập): Nếu câu chứa từ khóa "nhận", "lương", "thưởng", "lãi", "bán", "được cho", "biếu", "hoàn tiền", "thu".
                       - EXPENSE (Chi tiêu): Nếu câu chứa "mua", "trả", "đóng", "nạp", "đi" (chợ/xe), "ăn", "uống", "sắm", "tốn", "chi".
                       - MẶC ĐỊNH (Nếu không rõ động từ):
                         + Nếu nội dung liên quan tiền vào (vd: lương, thưởng) -> INCOME.
                         + Nếu nội dung liên quan tiêu dùng (vd: cafe, xăng, điện, nước) -> EXPENSE.
                    
                    2. QUY TẮC SỐ TIỀN:
                       - "k", "nghìn", "ng" -> 000
                       - "m", "tr", "triệu", "củ" -> 000000
                       - "lít" -> 00000 (trăm nghìn)
                       - "tỷ" -> 000000000
                    
                    3. DANH MỤC: Chọn tên trong danh sách khớp nhất. Nếu không, chọn "General".
                    
                    VÍ DỤ MẪU (HỌC THEO LOGIC NÀY):
                    User: "nhận lương 15tr" -> JSON: {"amount": 15000000, "type": "income", "category": "Lương", "description": "Lương tháng"}
                    User: "bán đồ cũ 500k" -> JSON: {"amount": 500000, "type": "income", "category": "Thu nhập khác", "description": "Bán đồ cũ"}
                    User: "cafe 30k" -> JSON: {"amount": 30000, "type": "expense", "category": "Ăn uống", "description": "Cafe"} (Mặc định Expense vì là đồ uống)
                    User: "đóng tiền điện 1 củ" -> JSON: {"amount": 1000000, "type": "expense", "category": "Hóa đơn & Tiện ích", "description": "Tiền điện"}
                    User: "đổ xăng 50" -> JSON: {"amount": 50000, "type": "expense", "category": "Di chuyển", "description": "Đổ xăng"} (Hiểu ngầm 50 là 50k)
                    
                    CHỈ TRẢ VỀ JSON:
        """.trimIndent()
    }

    fun getChatAdvisorPrompt(context: String, userQuestion: String, languageCode: String="vi"): String {
        return if (languageCode == "vi") {
            """
            Bạn là Trợ lý Tài chính (AI Financial Advisor).
            DỮ LIỆU TÀI CHÍNH: $context
            CÂU HỎI: "$userQuestion"
            NHIỆM VỤ: Trả lời bằng Tiếng Việt, văn phong lịch sự, ngắn gọn.
            """.trimIndent()
        } else {
            """
            You are an AI Financial Advisor.
            FINANCIAL DATA: $context
            QUESTION: "$userQuestion"
            TASK: Reply in English, polite and concise tone.
            """.trimIndent()
        }
    }

    /**
     * Prompt for Smart Auto-Categorization.
     * Includes the user's recent transaction history as examples to personalize suggestions.
     * Security: description is sanitized by caller (max 200 chars, trimmed).
     */
    fun getAutoCategoryPrompt(
        sanitizedDescription: String,
        amount: Double,
        recentHistoryExamples: String,   // "coffee -> Ăn uống, grab -> Di chuyển"
        availableCategories: String,      // Comma-separated list of user's category names
        languageCode: String = "vi"
    ): String {
        return """
            You are a financial transaction categorizer. Given a description and amount, return ONLY a JSON object.
            
            USER'S RECENT HABITS (learn from these):
            $recentHistoryExamples
            
            AVAILABLE CATEGORIES: [$availableCategories]
            
            TRANSACTION TO CATEGORIZE:
            - Description: "$sanitizedDescription"
            - Amount: $amount
            
            RULES:
            1. Match the description to one of the AVAILABLE CATEGORIES only.
            2. Prioritize the user's recent habits over generic rules.
            3. If unsure, return "General".
            4. ONLY return valid JSON, nothing else.
            
            RETURN FORMAT: {"category": "CategoryName", "confidence": "high|medium|low"}
        """.trimIndent()
    }

    /**
     * Prompt for Personalized Financial Insights report card.
     * Uses aggregated numbers only — no raw descriptions are sent.
     */
    fun getInsightReportPrompt(
        currentMonthSummary: String,   // e.g. "Income: 15M, Expense: 12M, Top: Food 4M, Transport 2M"
        previousMonthSummary: String,  // e.g. "Income: 14M, Expense: 11M, Top: Food 3M, Transport 3M"
        languageCode: String = "vi"
    ): String {
        return if (languageCode == "vi") {
            """
            Bạn là chuyên gia phân tích tài chính. Hãy tạo báo cáo tài chính cá nhân ngắn gọn dựa trên dữ liệu sau.
            
            THÁNG NÀY: $currentMonthSummary
            THÁNG TRƯỚC: $previousMonthSummary
            
            Trả về JSON hợp lệ với đúng 3 trường sau (không thêm gì khác):
            {
              "summary": "2-3 câu phân tích xu hướng chi tiêu so với tháng trước, bằng Tiếng Việt, giọng thân thiện",
              "topChangeCategory": "tên danh mục có sự thay đổi lớn nhất",
              "actionableTip": "1 lời khuyên cụ thể, ngắn gọn để cải thiện tài chính"
            }
            CHỈ TRẢ VỀ JSON:
            """.trimIndent()
        } else {
            """
            You are a personal finance analyst. Generate a concise financial report based on the data below.
            
            THIS MONTH: $currentMonthSummary
            LAST MONTH: $previousMonthSummary
            
            Return valid JSON with exactly these 3 fields (nothing else):
            {
              "summary": "2-3 sentences analyzing spending trends vs last month, friendly English tone",
              "topChangeCategory": "name of category with biggest change",
              "actionableTip": "1 specific, concise tip to improve finances"
            }
            ONLY RETURN JSON:
            """.trimIndent()
        }
    }

    /**
     * Prompt for Predictive Budget suggestions.
     * Uses only category-level aggregates — no individual transaction descriptions.
     */
    fun getPredictiveBudgetPrompt(
        categoryAverages: String,   // "Food & Drinks: avg 3.2M/month (3 months), Transport: avg 1.5M/month (3 months)"
        languageCode: String = "vi"
    ): String {
        return if (languageCode == "vi") {
            """
            Bạn là chuyên gia lập kế hoạch ngân sách. Dựa trên lịch sử chi tiêu trung bình sau, hãy đề xuất ngân sách hàng tháng hợp lý.
            
            LỊCH SỬ CHI TIÊU TRUNG BÌNH:
            $categoryAverages
            
            Trả về JSON array hợp lệ. Mỗi phần tử có đúng 3 trường:
            [
              {
                "category": "tên danh mục",
                "suggestedAmount": số tiền đề xuất (số, không có đơn vị),
                "rationale": "giải thích ngắn gọn 1 câu tại sao đề xuất mức này"
              }
            ]
            QUY TẮC: suggestedAmount nên = trung bình * 1.1 (thêm 10% dự phòng), làm tròn đến 50,000.
            CHỈ TRẢ VỀ JSON ARRAY:
            """.trimIndent()
        } else {
            """
            You are a budget planning expert. Based on the average spending history below, suggest reasonable monthly budgets.
            
            AVERAGE SPENDING HISTORY:
            $categoryAverages
            
            Return a valid JSON array. Each element has exactly 3 fields:
            [
              {
                "category": "category name",
                "suggestedAmount": suggested amount (number, no units),
                "rationale": "1 concise sentence explaining why this amount is suggested"
              }
            ]
            RULE: suggestedAmount = average * 1.1 (add 10% buffer), rounded to nearest 50,000.
            ONLY RETURN JSON ARRAY:
            """.trimIndent()
        }
    }

    /**
     * Prompt for Financial DNA personalized storytelling & coaching.
     * Security: Only receives aggregated scores and top category names — zero raw PII.
     */
    fun getFinancialDnaPrompt(
        dnaCode: String,
        archetypeTitle: String,
        savingsScore: Int,
        plannerScore: Int,
        stabilityScore: Int,
        focusScore: Int,
        savingsRatePercent: Double,
        weeklyVariancePercent: Double,
        topCategoriesSharePercent: Double,
        topCategories: String,
        languageCode: String = "vi"
    ): String {
        val sr = String.format(java.util.Locale.US, "%.1f", savingsRatePercent)
        val wv = String.format(java.util.Locale.US, "%.0f", weeklyVariancePercent)
        val tc = String.format(java.util.Locale.US, "%.0f", topCategoriesSharePercent)

        return if (languageCode == "vi") {
            """
            Bạn là chuyên gia tâm lý học tài chính cá nhân. Dựa trên hồ sơ Financial DNA dưới đây, hãy viết phân tích cá nhân hóa thật sâu sắc, ấm áp và gần gũi như MBTI tài chính.

            HỒ SƠ FINANCIAL DNA:
            - Mã DNA: $dnaCode ($archetypeTitle)
            - Trục Tiết kiệm vs Hưởng thụ: $savingsScore/100 (Tỷ lệ tiết kiệm: $sr%)
            - Trục Kế hoạch vs Ngẫu hứng: $plannerScore/100
            - Trục Ổn định vs Biến động: $stabilityScore/100 (Dao động tuần: ±$wv%)
            - Trục Tập trung vs Phân tán: $focusScore/100 (Top danh mục chiếm $tc%: $topCategories)

            Trả về JSON hợp lệ với đúng 3 trường sau (không thêm markdown hay văn bản thừa):
            {
              "narrative": "2-3 câu kể câu chuyện cá tính tài chính của người dùng một cách tự nhiên, sắc sảo dựa vào số liệu thật",
              "strengths": ["Điểm mạnh cụ thể 1", "Điểm mạnh cụ thể 2"],
              "improvements": ["Gợi ý cải thiện thiết thực 1", "Gợi ý cải thiện thiết thực 2"]
            }
            CHỈ TRẢ VỀ JSON:
            """.trimIndent()
        } else {
            """
            You are a personal finance behavioral psychologist. Based on the Financial DNA profile below, write an insightful, warm, MBTI-style personalized portrait.

            FINANCIAL DNA PROFILE:
            - DNA Code: $dnaCode ($archetypeTitle)
            - Saver vs Indulger: $savingsScore/100 (Savings rate: $sr%)
            - Planner vs Spontaneous: $plannerScore/100
            - Consistent vs Volatile: $stabilityScore/100 (Weekly swing: ±$wv%)
            - Targeted vs Diverse: $focusScore/100 (Top categories share $tc%: $topCategories)

            Return valid JSON with exactly these 3 fields (no markdown or extra text):
            {
              "narrative": "2-3 crisp, engaging sentences portraying their financial personality grounded in their real numbers",
              "strengths": ["Concrete strength 1", "Concrete strength 2"],
              "improvements": ["Practical growth tip 1", "Practical growth tip 2"]
            }
            ONLY RETURN JSON:
            """.trimIndent()
        }
    }

    /**
     * Prompt for Weekly AI Digest commentary & actionable tip.
     * Security: Only aggregated 7-day totals and top category names are transmitted.
     */
    fun getWeeklyDigestPrompt(
        totalSpentFormatted: String,
        previousWeekSpentFormatted: String,
        deltaPercent: Double,
        topCategoriesSummary: String,
        budgetsOnTrackText: String,
        highestDayText: String,
        languageCode: String = "vi"
    ): String {
        val deltaSign = if (deltaPercent >= 0) "+" else ""
        val deltaFormatted = "$deltaSign${String.format(java.util.Locale.US, "%.0f", deltaPercent)}%"
        return if (languageCode == "vi") {
            """
            Bạn là huấn luyện viên tài chính cá nhân (Proactive Financial Coach). Hãy viết bản tin tổng kết 7 ngày qua ngắn gọn, sắc bén và khích lệ.

            DỮ LIỆU TUẦN QUA:
            - Tổng chi tuần này: $totalSpentFormatted (so với tuần trước $previousWeekSpentFormatted: $deltaFormatted)
            - Top danh mục: $topCategoriesSummary
            - Ngày chi cao nhất: $highestDayText
            - Trạng thái ngân sách: $budgetsOnTrackText

            Trả về JSON hợp lệ gồm đúng 2 trường (không markdown, không văn bản thừa):
            {
              "summary": "2 câu nhận xét sát thực tế về xu hướng chi tiêu 7 ngày qua, giọng gần gũi, khích lệ",
              "tip": "1 mẹo hành động cụ thể, dễ làm ngay trong tuần tới"
            }
            CHỈ TRẢ VỀ JSON:
            """.trimIndent()
        } else {
            """
            You are a Proactive Financial Coach. Write a crisp, warm 7-day personal finance digest commentary.

            PAST 7 DAYS SUMMARY:
            - Total spent: $totalSpentFormatted (vs prior week $previousWeekSpentFormatted: $deltaFormatted)
            - Top categories: $topCategoriesSummary
            - Peak spending day: $highestDayText
            - Budgets status: $budgetsOnTrackText

            Return valid JSON with exactly 2 fields (no markdown or extra text):
            {
              "summary": "2 warm, data-grounded sentences analyzing the past 7 days' spending trend",
              "tip": "1 concrete, easy-to-apply tip for the week ahead"
            }
            ONLY RETURN JSON:
            """.trimIndent()
        }
    }
}