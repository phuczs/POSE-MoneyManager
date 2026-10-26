package com.example.moneymanager.domain.model

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class RecurringTransaction(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "expense", // "income" or "expense"
    val category: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val startDate: Long = System.currentTimeMillis(),
    val lastExecutedDate: Long? = null,
    val nextExecutionDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val description: String = ""
)
