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
}