package com.example.moneymanager.domain.usecase.category

import com.example.moneymanager.domain.repository.CategoryRepository
import javax.inject.Inject

/** Deletes a category and all its subcategories. */
class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(categoryId: String): Result<Unit> {
        if (categoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("Category ID cannot be empty"))
        }
        return categoryRepository.deleteCategory(categoryId)
    }
}
