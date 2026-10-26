package com.example.moneymanager.domain.model

/**
 * Domain model representing the user's daily spending velocity compared to their
 * monthly daily average.
 *
 * Feature F-06: Daily Spending Velocity Alerts.
 *
 * @property todaySpent Total amount spent today (00:00 to now) in user's currency.
 * @property dailyAverage Average daily expenditure in the current month up to yesterday.
 * @property percentOverAverage Percentage relative to average (e.g. 150 means 1.5x average).
 * @property projectedMonthTotal Estimated total expenditure by end of month if current rate continues.
 * @property isHighVelocity True when today's spending significantly exceeds normal pace.
 */
data class DailyVelocityReport(
    val todaySpent: Double,
    val dailyAverage: Double,
    val percentOverAverage: Double,
    val projectedMonthTotal: Double,
    val isHighVelocity: Boolean
)
