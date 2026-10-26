package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.Budget
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class FirebaseBudgetRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BudgetRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    override fun getBudgets(date: Date): Flow<List<Budget>> {
        return firestore.collection("budgets")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("endDate", date)
            .whereLessThanOrEqualTo("startDate", date)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Budget::class.java)
            }
    }

    override suspend fun saveBudget(budget: Budget): Result<Unit> = try {
        firestore.collection("budgets")
            .add(budget.copy(userId = userId))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateBudget(budget: Budget): Result<Unit> = try {
        val doc = firestore.collection("budgets").document(budget.id).get().await()
        if (!doc.exists() || doc.getString("userId") != userId) {
            Result.failure(Exception("Budget not found or unauthorized"))
        } else {
            firestore.collection("budgets")
                .document(budget.id)
                .set(budget)
                .await()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteBudget(budgetId: String): Result<Unit> = try {
        val doc = firestore.collection("budgets").document(budgetId).get().await()
        if (!doc.exists() || doc.getString("userId") != userId) {
            Result.failure(Exception("Budget not found or unauthorized"))
        } else {
            firestore.collection("budgets")
                .document(budgetId)
                .delete()
                .await()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
