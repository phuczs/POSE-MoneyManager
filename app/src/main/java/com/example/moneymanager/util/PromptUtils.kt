package com.example.moneymanager.util

import com.example.moneymanager.util.toCurrencyString

object PromptUtils {

    fun getFinancialAdvisorPrompt(
        totalIncome: Double,
        totalExpense: Double,
        balance: Double,
        topExpenses: String,
        budgetAnalysis: String
    ): String {
        return """
            Bạn là trợ lý tài chính thông minh (AI Financial Advisor).
            HỒ SƠ TÀI CHÍNH THÁNG NÀY:
            - Tổng thu: ${totalIncome.toCurrencyString()}
            - Tổng chi: ${totalExpense.toCurrencyString()}
            - Số dư: ${balance.toCurrencyString()}
            
            TOP CHI TIÊU:
            $topExpenses
            $budgetAnalysis
            
            NHIỆM VỤ:
            - Tư vấn ngắn gọn, hữu ích dựa trên số liệu thực tế.
            - Cảnh báo nếu chi tiêu vượt mức.
            - Trả lời thân thiện bằng tiếng Việt.
        """.trimIndent()
    }

    fun getQuickAddPrompt(input: String, categoryNames: String): String {
        return """
            Bạn là công cụ trích xuất thông tin CHI TIÊU (Expense) từ văn bản tiếng Việt.
                    
                    INPUT: "$input"
                    
                    DANH MỤC HỆ THỐNG: [$categoryNames, General, Food & Drinks, Transportation, Shopping]
                    
                    NHIỆM VỤ: Trả về JSON với các trường sau:
                    1. "amount": Số tiền (Double). Tự động đổi: "k/nghìn"->000, "tr/m/củ"->000000, "lít"->00000.
                    2. "category": Chọn 1 tên trong danh sách trên khớp nhất với nội dung. Nếu không rõ, chọn "General".
                    3. "description": Viết lại nội dung ngắn gọn, viết hoa chữ cái đầu.
                    4. "type": Luôn là "expense".
                    
                    VÍ DỤ MẪU (HỌC THEO LOGIC NÀY):
                    User: "cafe 25k" 
                    JSON: {"amount": 25000, "category": "Food & Drinks", "description": "Cafe", "type": "expense"}
                    
                    User: "đổ xăng 50" (Hiểu là 50k)
                    JSON: {"amount": 50000, "category": "Transportation", "description": "Đổ xăng", "type": "expense"}
                    
                    User: "mua áo khoác 1 củ 2" (1.2 triệu)
                    JSON: {"amount": 1200000, "category": "Shopping", "description": "Mua áo khoác", "type": "expense"}
                    
                    User: "trả tiền điện 500 ngàn"
                    JSON: {"amount": 500000, "category": "Bills & Utilities", "description": "Tiền điện", "type": "expense"}
                    
                    CHỈ TRẢ VỀ ĐÚNG 1 CHUỖI JSON DUY NHẤT:
        """.trimIndent()
    }

    fun getChatAdvisorPrompt(context: String, userQuestion: String): String {
        return """
            Bạn là Trợ lý Tài chính (AI Financial Advisor).
            
            DỮ LIỆU TÀI CHÍNH CỦA NGƯỜI DÙNG:
            $context
            
            CÂU HỎI CỦA NGƯỜI DÙNG: "$userQuestion"
            
            NHIỆM VỤ:
            - Trả lời câu hỏi, đưa ra lời khuyên dựa trên số liệu trên.
            - Văn phong lịch sự, chuyên nghiệp, tiếng Việt tự nhiên.
            - KHÔNG trả về JSON. Trả về văn bản thường (Markdown) để dễ đọc.
            - Ngắn gọn, súc tích.
        """.trimIndent()
    }
}