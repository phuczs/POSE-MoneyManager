package com.example.moneymanager.domain.usecase.category

import com.example.moneymanager.domain.model.Category
import com.example.moneymanager.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Returns categories filtered by type ("income" or "expense"). */
class GetCategoriesByTypeUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(type: String): Flow<List<Category>> =
        categoryRepository.getCategoriesByType(type)
}
