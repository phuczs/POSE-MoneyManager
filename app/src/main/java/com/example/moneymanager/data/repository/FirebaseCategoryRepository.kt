package com.example.moneymanager.data.repository

import com.example.moneymanager.data.model.Category
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

@Singleton
class FirebaseCategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CategoryRepository {

    private val categoriesCollection = firestore.collection("categories")
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    }

    override fun getAllCategories(): Flow<List<Category>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = categoriesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, err ->
                if (err != null ) {
                    close(err)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { listenerRegistration.remove() }
    }

    override fun getCategoriesByType(type: String): Flow<List<Category>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = categoriesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", type)
            .addSnapshotListener { snapshot, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { listenerRegistration.remove() }
    }

    override fun getTopLevelCategories(): Flow<List<Category>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = categoriesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("parentId", null)
            .addSnapshotListener { snapshot, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { listenerRegistration.remove() }
    }

    override fun getTopLevelCategoriesByType(type: String): Flow<List<Category>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = categoriesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("parentId", null)
            .whereEqualTo("type", type)
            .addSnapshotListener { snapshot, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { listenerRegistration.remove() }
    }

    override fun getSubcategories(parentId: String): Flow<List<Category>> = callbackFlow{
        val userId = getCurrentUserId()
        val listenerRegistration = categoriesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("parentId", parentId)
            .addSnapshotListener { snapshot, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun addCategory(category: Category): Result<Category> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            val categoryWithUser = category.copy(userId = userId)
            val docRef = categoriesCollection.document()
            val categoryWithId = categoryWithUser.copy(id = docRef.id)
            docRef.set(categoryWithId).await()
            Result.success(categoryWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            val categoryDoc = categoriesCollection.document(categoryId).get().await()
            if (!categoryDoc.exists() || categoryDoc.getString("userId") != userId) {
                return@withContext Result.failure(Exception("Category not found or unauthorized"))
            }
            val subcategoriesSnapshot = categoriesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("parentId", categoryId)
                .get()
                .await()
            subcategoriesSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            categoriesCollection.document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategoryById(categoryId: String): Result<Category> = withContext(
        Dispatchers.IO){
        try {
            val userId = getCurrentUserId()
            val document = categoriesCollection.document(categoryId).get().await()
            val category = document.toObject(Category::class.java)

            if (category != null && category.userId == userId) {
                Result.success(category)
            } else {
                Result.failure(Exception("Category not found or unauthorized"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}