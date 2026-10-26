package com.example.moneymanager.domain.usecase.category

import com.example.moneymanager.domain.model.Category
import com.example.moneymanager.domain.repository.CategoryRepository
import javax.inject.Inject

/** Adds a new category, validating that name and type are non-blank. */
class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Category> {
        if (category.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Category name cannot be blank"))
        }
        if (category.type !in setOf("income", "expense")) {
            return Result.failure(IllegalArgumentException("Category type must be 'income' or 'expense'"))
        }
        return categoryRepository.addCategory(category)
    }
}
