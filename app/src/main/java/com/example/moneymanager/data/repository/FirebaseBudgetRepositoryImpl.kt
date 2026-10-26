package com.example.moneymanager.data.repository

import android.util.Log
import com.example.moneymanager.data.mapper.toBudgetDomain
import com.example.moneymanager.data.mapper.toFirestoreMap
import com.example.moneymanager.domain.model.Budget
import com.example.moneymanager.domain.repository.BudgetRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Firestore implementation of [BudgetRepository].
 *
 * Clean Architecture note:
 * - [getBudgets] now takes a [Long] epoch ms instead of [java.util.Date],
 *   aligned with the domain interface.
 * - Firestore Timestamp conversion is handled by [BudgetMapper].
 *
 * Security note:
 * - All queries are user-scoped via [FirebaseAuth.currentUser?.uid].
 * - The [Budget.userId] is always set from the authenticated UID in write operations.
 */
@Singleton
class FirebaseBudgetRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BudgetRepository {

    private val budgetsCollection = firestore.collection("budgets")

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    override fun getBudgets(date: Long): Flow<List<Budget>> = callbackFlow {
        val uid = userId
        val firebaseDate = com.google.firebase.Timestamp(date / 1000L, ((date % 1000L) * 1_000_000L).toInt())
        val registration = budgetsCollection
            .whereEqualTo("userId", uid)
            .whereGreaterThanOrEqualTo("endDate", firebaseDate)
            .whereLessThanOrEqualTo("startDate", firebaseDate)
            .addSnapshotListener { snapshot, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val budgets = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toBudgetDomain(doc.id)
                } ?: emptyList()
                trySend(budgets)
            }
        awaitClose { registration.remove() }
    }

    override fun getAllBudgets(): Flow<List<Budget>> = callbackFlow {
        val uid = userId
        val registration = budgetsCollection
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val budgets = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toBudgetDomain(doc.id)
                } ?: emptyList()
                trySend(budgets)
            }
        awaitClose { registration.remove() }
    }

    override fun getBudgetHistory(category: String, limit: Int): Flow<List<Budget>> = callbackFlow {
        val uid = userId
        val registration = budgetsCollection
            .whereEqualTo("userId", uid)
            .whereEqualTo("category", category)
            .addSnapshotListener { snapshot, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val budgets = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toBudgetDomain(doc.id)
                }?.sortedByDescending { it.startDate }?.take(limit) ?: emptyList()
                trySend(budgets)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun saveBudget(budget: Budget): Result<Unit> = try {
        budgetsCollection.add(budget.copy(userId = userId).toFirestoreMap()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "saveBudget failed", e)
        Result.failure(e)
    }

    override suspend fun updateBudget(budget: Budget): Result<Unit> = try {
        val doc = budgetsCollection.document(budget.id).get().await()
        if (!doc.exists() || doc.getString("userId") != userId) {
            Result.failure(Exception("Budget not found or unauthorized"))
        } else {
            budgetsCollection.document(budget.id).set(budget.toFirestoreMap()).await()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Log.e(TAG, "updateBudget failed", e)
        Result.failure(e)
    }

    override suspend fun deleteBudget(budgetId: String): Result<Unit> = try {
        val doc = budgetsCollection.document(budgetId).get().await()
        if (!doc.exists() || doc.getString("userId") != userId) {
            Result.failure(Exception("Budget not found or unauthorized"))
        } else {
            budgetsCollection.document(budgetId).delete().await()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Log.e(TAG, "deleteBudget failed", e)
        Result.failure(e)
    }

    companion object {
        private const val TAG = "FirebaseBudgetRepoImpl"
    }
}
