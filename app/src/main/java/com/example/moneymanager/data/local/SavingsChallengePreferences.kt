package com.example.moneymanager.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.moneymanager.domain.model.ChallengeStatus
import com.example.moneymanager.domain.model.SavingsChallenge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists active and historical Savings Challenges into SharedPreferences using JSON.
 */
@Singleton
class SavingsChallengePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("savings_challenge_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CHALLENGES_JSON = "saved_challenges_json"
    }

    private val _challenges = MutableStateFlow(loadChallenges())
    val challenges: StateFlow<List<SavingsChallenge>> = _challenges.asStateFlow()

    private fun loadChallenges(): List<SavingsChallenge> {
        val raw = prefs.getString(KEY_CHALLENGES_JSON, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<SavingsChallenge>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SavingsChallenge(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        description = obj.optString("description"),
                        category = obj.optString("category"),
                        keywordFilter = obj.optString("keywordFilter"),
                        targetDays = obj.optInt("targetDays", 7),
                        currentStreak = obj.optInt("currentStreak", 0),
                        dailyBudgetLimit = obj.optDouble("dailyBudgetLimit", 0.0),
                        baselineDailyAverage = obj.optDouble("baselineDailyAverage", 0.0),
                        savedAmount = obj.optDouble("savedAmount", 0.0),
                        targetSavings = obj.optDouble("targetSavings", 0.0),
                        startDate = obj.optLong("startDate", System.currentTimeMillis()),
                        endDate = obj.optLong("endDate", System.currentTimeMillis()),
                        status = try {
                            ChallengeStatus.valueOf(obj.optString("status", ChallengeStatus.ACTIVE.name))
                        } catch (e: Exception) {
                            ChallengeStatus.ACTIVE
                        },
                        badgeName = obj.optString("badgeName", "Tiết Kiệm Thông Thái"),
                        badgeIcon = obj.optString("badgeIcon", "🏆"),
                        daysCompleted = obj.optInt("daysCompleted", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveChallenge(challenge: SavingsChallenge) {
        val current = _challenges.value.toMutableList()
        val index = current.indexOfFirst { it.id == challenge.id }
        if (index >= 0) {
            current[index] = challenge
        } else {
            current.add(challenge)
        }
        persistList(current)
    }

    fun removeChallenge(challengeId: String) {
        val updated = _challenges.value.filter { it.id != challengeId }
        persistList(updated)
    }

    fun saveAll(newList: List<SavingsChallenge>) {
        persistList(newList)
    }

    private fun persistList(list: List<SavingsChallenge>) {
        _challenges.value = list
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("description", item.description)
                    put("category", item.category)
                    put("keywordFilter", item.keywordFilter)
                    put("targetDays", item.targetDays)
                    put("currentStreak", item.currentStreak)
                    put("dailyBudgetLimit", item.dailyBudgetLimit)
                    put("baselineDailyAverage", item.baselineDailyAverage)
                    put("savedAmount", item.savedAmount)
                    put("targetSavings", item.targetSavings)
                    put("startDate", item.startDate)
                    put("endDate", item.endDate)
                    put("status", item.status.name)
                    put("badgeName", item.badgeName)
                    put("badgeIcon", item.badgeIcon)
                    put("daysCompleted", item.daysCompleted)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_CHALLENGES_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Log or ignore
        }
    }
}
