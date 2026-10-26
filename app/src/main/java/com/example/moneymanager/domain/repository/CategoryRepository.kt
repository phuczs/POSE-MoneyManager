package com.example.moneymanager.domain.repository

import com.example.moneymanager.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for category data access.
 *
 * Clean Architecture note:
 * - Zero framework dependencies.
 * - Implementations enforce user-scoped queries using the authenticated UID from FirebaseAuth.
 */
interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: String): Flow<List<Category>>
    fun getTopLevelCategories(): Flow<List<Category>>
    fun getTopLevelCategoriesByType(type: String): Flow<List<Category>>
    fun getSubcategories(parentId: String): Flow<List<Category>>
    suspend fun addCategory(category: Category): Result<Category>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    suspend fun getCategoryById(categoryId: String): Result<Category>
}
