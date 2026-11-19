package com.example.moneymanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.moneymanager.data.local.CoachPreferences
import com.example.moneymanager.domain.model.NudgeActionTarget
import com.example.moneymanager.domain.model.NudgeTone
import com.example.moneymanager.domain.repository.SettingsRepository
import com.example.moneymanager.domain.usecase.ai.EvaluateSpendingNudgeUseCase
import com.example.moneymanager.domain.usecase.ai.GenerateWeeklyDigestUseCase
import com.example.moneymanager.domain.usecase.budget.GetAllBudgetsUseCase
import com.example.moneymanager.domain.usecase.transaction.GetAllTransactionsUseCase
import com.example.moneymanager.util.NotificationHelper
import com.example.moneymanager.widget.MoneyManagerWidgetProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager job powering Phase 5.3 automation:
 * - On Mondays (or once every 7 days): generates & delivers the Weekly AI Financial Digest notification.
 * - On other days (at most once per 20h): checks Proactive AI Coach triggers and pushes high-value nudges.
 *
 * Battery-friendly: single periodic check every 24 hours, honors user notification preferences.
 */
class WeeklyDigestWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun getAllTransactionsUseCase(): GetAllTransactionsUseCase
        fun getAllBudgetsUseCase(): GetAllBudgetsUseCase
        fun generateWeeklyDigestUseCase(): GenerateWeeklyDigestUseCase
        fun evaluateSpendingNudgeUseCase(): EvaluateSpendingNudgeUseCase
        fun settingsRepository(): SettingsRepository
        fun coachPreferences(): CoachPreferences
        fun notificationHelper(): NotificationHelper
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                WorkerEntryPoint::class.java
            )
            val settingsRepo = entryPoint.settingsRepository()
            if (!settingsRepo.isNotificationsEnabled.value) {
                return Result.success()
            }

            val getAllTransactions = entryPoint.getAllTransactionsUseCase()
            val getAllBudgets = entryPoint.getAllBudgetsUseCase()
            val generateWeeklyDigest = entryPoint.generateWeeklyDigestUseCase()
            val evaluateSpendingNudge = entryPoint.evaluateSpendingNudgeUseCase()

            val transactions = withTimeoutOrNull(5_000L) {
                getAllTransactions().first()
            } ?: return Result.success()
            if (transactions.isEmpty()) return Result.success()

            val budgets = withTimeoutOrNull(5_000L) {
                getAllBudgets().first()
            } ?: emptyList()

            val languageCode = settingsRepo.currentLanguage.value
            val coachPrefs = entryPoint.coachPreferences()
            val notificationHelper = entryPoint.notificationHelper()
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val isMonday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
            val daysSinceLastDigest = (now - coachPrefs.getLastDigestPushMillis()) / DAY_MS

            if ((isMonday && daysSinceLastDigest >= 6L) || daysSinceLastDigest >= 7L) {
                val digest = generateWeeklyDigest(
                    allTransactions = transactions,
                    activeBudgets = budgets,
                    languageCode = languageCode,
                    nowMillis = now
                )
                if (digest != null) {
                    val title = if (languageCode == "vi") {
                        "📅 Bản tin Tài chính Tuần qua"
                    } else {
                        "📅 Your Weekly Financial Digest"
                    }
                    notificationHelper.showWeeklyDigestNotification(
                        title = title,
                        summary = digest.aiSummary
                    )
                    coachPrefs.recordDigestPush(now)
                    return Result.success()
                }
            }

            // Otherwise check if an urgent/warning/gentle proactive nudge should be delivered
            val hoursSinceLastCoachPush = (now - coachPrefs.getLastCoachPushMillis()) / HOUR_MS
            if (hoursSinceLastCoachPush >= 20L) {
                val nudge = evaluateSpendingNudge(
                    allTransactions = transactions,
                    activeBudgets = budgets,
                    dismissedNudgeIds = coachPrefs.dismissedNudgeIds.value,
                    languageCode = languageCode,
                    nowMillis = now
                )
                if (nudge != null && nudge.tone != NudgeTone.CELEBRATION) {
                    val navTarget = when (nudge.actionTarget) {
                        NudgeActionTarget.OPEN_CHAT -> MoneyManagerWidgetProvider.NAV_CHAT
                        NudgeActionTarget.ADD_TRANSACTION -> MoneyManagerWidgetProvider.NAV_ADD_TRANSACTION
                        NudgeActionTarget.OPEN_BUDGETS -> NotificationHelper.NAV_BUDGETS
                        NudgeActionTarget.OPEN_WEEKLY_DIGEST -> NotificationHelper.NAV_WEEKLY_DIGEST
                    }
                    notificationHelper.showCoachNudgeNotification(
                        nudgeId = nudge.id,
                        title = "${nudge.emoji} ${nudge.title}",
                        body = nudge.message,
                        navTarget = navTarget
                    )
                    coachPrefs.recordCoachPush(now)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "weekly_ai_digest_and_coach_worker"
        private const val HOUR_MS = 60L * 60 * 1000
        private const val DAY_MS = 24L * HOUR_MS

        fun enqueuePeriodicWork(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<WeeklyDigestWorker>(
                    24, TimeUnit.HOURS
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
