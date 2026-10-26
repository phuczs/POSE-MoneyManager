package com.example.moneymanager.data.repository

import android.util.Log
import com.example.moneymanager.data.mapper.toCategoryDomain
import com.example.moneymanager.data.mapper.toFirestoreMap
import com.example.moneymanager.domain.model.Category
import com.example.moneymanager.domain.repository.CategoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Firestore implementation of [CategoryRepository].
 *
 * Security note:
 * - All Firestore queries are scoped to the authenticated user's UID, obtained
 *   from [FirebaseAuth] — never from caller-provided parameters.
 * - [deleteCategory] also deletes all subcategories to maintain referential integrity.
 */
@Singleton
class FirebaseCategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CategoryRepository {

    private val categoriesCollection = firestore.collection("categories")

    private fun getCurrentUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    override fun getAllCategories(): Flow<List<Category>> = callbackFlow {
        val userId = getCurrentUserId()
        val registration = categoriesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toCategoryDomain(doc.id)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { registration.remove() }
    }

    override fun getCategoriesByType(type: String): Flow<List<Category>> = callbackFlow {
        val userId = getCurrentUserId()
        val registration = categoriesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", type)
            .addSnapshotListener { snapshot, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toCategoryDomain(doc.id)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { registration.remove() }
    }

    override fun getTopLevelCategories(): Flow<List<Category>> = callbackFlow {
        val userId = getCurrentUserId()
        val registration = categoriesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("parentId", null)
            .addSnapshotListener { snapshot, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toCategoryDomain(doc.id)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { registration.remove() }
    }

    override fun getTopLevelCategoriesByType(type: String): Flow<List<Category>> = callbackFlow {
        val userId = getCurrentUserId()
        val registration = categoriesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("parentId", null)
            .whereEqualTo("type", type)
            .addSnapshotListener { snapshot, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toCategoryDomain(doc.id)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { registration.remove() }
    }

    override fun getSubcategories(parentId: String): Flow<List<Category>> = callbackFlow {
        val userId = getCurrentUserId()
        val registration = categoriesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("parentId", parentId)
            .addSnapshotListener { snapshot, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toCategoryDomain(doc.id)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun addCategory(category: Category): Result<Category> =
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val docRef = categoriesCollection.document()
                val enriched = category.copy(userId = userId, id = docRef.id)
                docRef.set(enriched.toFirestoreMap()).await()
                Result.success(enriched)
            } catch (e: Exception) {
                Log.e(TAG, "addCategory failed", e)
                Result.failure(e)
            }
        }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val categoryDoc = categoriesCollection.document(categoryId).get().await()
                if (!categoryDoc.exists() || categoryDoc.getString("userId") != userId) {
                    return@withContext Result.failure(Exception("Category not found or unauthorized"))
                }
                // Delete subcategories first to maintain referential integrity
                val subcategories = categoriesCollection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("parentId", categoryId)
                    .get().await()
                subcategories.documents.forEach { it.reference.delete().await() }
                categoriesCollection.document(categoryId).delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "deleteCategory failed", e)
                Result.failure(e)
            }
        }

    override suspend fun getCategoryById(categoryId: String): Result<Category> =
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val doc = categoriesCollection.document(categoryId).get().await()
                val category = doc.data?.toCategoryDomain(doc.id)
                if (category == null || category.userId != userId) {
                    return@withContext Result.failure(Exception("Category not found or unauthorized"))
                }
                Result.success(category)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        private const val TAG = "FirebaseCategoryRepoImpl"
    }
}
