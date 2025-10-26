package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.model.Category
import com.example.moneymanager.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _categoriesState = MutableStateFlow<CategoriesState>(CategoriesState.Loading)
    val categoriesState: StateFlow<CategoriesState> = _categoriesState.asStateFlow()
    private val _groupedCategoriesState = MutableStateFlow<GroupedCategoriesState>(
        GroupedCategoriesState.Loading
    )
    val groupedCategoriesState: StateFlow<GroupedCategoriesState> =
        _groupedCategoriesState.asStateFlow()

    init {
        loadAllCategories()
        loadGroupedCategories()
    }

    fun loadAllCategories() {
        viewModelScope.launch {
            _categoriesState.value = CategoriesState.Loading
            categoryRepository.getAllCategories()
                .catch { e ->
                    _categoriesState.value =
                        CategoriesState.Error(e.message ?: "Failed to load categories")
                }
                .collectLatest { categories ->
                    _categoriesState.value = CategoriesState.Success(categories)
                }
        }
    }

    fun loadGroupedCategories() {
        viewModelScope.launch {
            _groupedCategoriesState.value = GroupedCategoriesState.Loading
            categoryRepository.getAllCategories()
                .catch { e ->
                    _groupedCategoriesState.value =
                        GroupedCategoriesState.Error(e.message ?: "Failed to load categories")
                }
                .collectLatest { categories ->
                    val topLevel = categories.filter { it.parentId == null }
                    val grouped = topLevel.map { parent ->
                        val children = categories.filter { it.parentId == parent.id }
                        CategoryGroup(parent, children)
                    }
                    _groupedCategoriesState.value = GroupedCategoriesState.Success(grouped)
                }
        }
    }

    fun loadCategoriesByType(type: String) {
        viewModelScope.launch {
            _categoriesState.value = CategoriesState.Loading
            categoryRepository.getCategoriesByType(type)
                .catch { e ->
                    _categoriesState.value =
                        CategoriesState.Error(e.message ?: "Failed to load categories")
                }
                .collectLatest { categories ->
                    _categoriesState.value = CategoriesState.Success(categories)
                }
        }
    }

    fun loadGroupedCategoriesByType(type: String) {
        viewModelScope.launch {
            _groupedCategoriesState.value = GroupedCategoriesState.Loading
            categoryRepository.getCategoriesByType(type)
                .catch { e ->
                    _groupedCategoriesState.value =
                        GroupedCategoriesState.Error(e.message ?: "Failed to load categories")
                }
                .collectLatest { categories ->
                    val topLevel = categories
                        .filter { it.parentId == null }
                        .distinctBy { it.name.lowercase().trim() } // Safety check

                    val grouped = topLevel.map { parent ->
                        val children = categories.filter { it.parentId == parent.id }
                        CategoryGroup(parent, children)
                    }
                    _groupedCategoriesState.value = GroupedCategoriesState.Success(grouped)
                }
        }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.addCategory(category)
                .fold(
                    onSuccess = {
                        loadAllCategories()
                        loadGroupedCategories()
                    },
                    onFailure = { error ->
                        _categoriesState.value = CategoriesState.Error(
                            error.message ?: "Failed to add category"
                        )
                    }
                )
        }
    }

    fun createDefaultCategories() {
        viewModelScope.launch {
            val userId = "current_user_id" // Replace with actual user ID

            // Top-level categories
            val topLevelCategories = listOf(
                Category(name = "Food & Drinks", type = "expense", userId = userId),
                Category(name = "Transportation", type = "expense", userId = userId),
                Category(name = "Shopping", type = "expense", userId = userId),
                Category(name = "Bills & Utilities", type = "expense", userId = userId),
                Category(name = "Entertainment", type = "expense", userId = userId),
                Category(name = "Healthcare", type = "expense", userId = userId),
                Category(name = "Salary", type = "income", userId = userId),
                Category(name = "Freelance", type = "income", userId = userId),
                Category(name = "Investment", type = "income", userId = userId),
                Category(name = "Other Income", type = "income", userId = userId)
            )

            // Add top-level categories first and store their IDs
            val categoryIds = mutableMapOf<String, String>()
            topLevelCategories.forEach { category ->
                categoryRepository.addCategory(category).fold(
                    onSuccess = { addedCategory ->
                        categoryIds[category.name] = addedCategory.id
                    },
                    onFailure = { /* Handle error */ }
                )
            }

            // Wait a bit for Firebase to process
            kotlinx.coroutines.delay(500)

            // Now add subcategories
            val subcategories = listOf(
                // Food & Drinks subcategories
                Category(
                    name = "Restaurants",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Food & Drinks"]
                ),
                Category(
                    name = "Groceries",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Food & Drinks"]
                ),
                Category(
                    name = "Coffee & Tea",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Food & Drinks"]
                ),
                Category(
                    name = "Fast Food",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Food & Drinks"]
                ),

                // Transportation subcategories
                Category(
                    name = "Fuel",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Transportation"]
                ),
                Category(
                    name = "Public Transit",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Transportation"]
                ),
                Category(
                    name = "Taxi/Ride Share",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Transportation"]
                ),
                Category(
                    name = "Parking",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Transportation"]
                ),

                // Shopping subcategories
                Category(
                    name = "Clothing",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Shopping"]
                ),
                Category(
                    name = "Electronics",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Shopping"]
                ),
                Category(
                    name = "Home & Garden",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Shopping"]
                ),
                Category(
                    name = "Personal Care",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Shopping"]
                ),

                // Bills & Utilities subcategories
                Category(
                    name = "Electricity",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Bills & Utilities"]
                ),
                Category(
                    name = "Water",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Bills & Utilities"]
                ),
                Category(
                    name = "Gas",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Bills & Utilities"]
                ),
                Category(
                    name = "Internet",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Bills & Utilities"]
                ),
                Category(
                    name = "Phone",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Bills & Utilities"]
                ),
                Category(
                    name = "Rent/Mortgage",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Bills & Utilities"]
                ),

                // Entertainment subcategories
                Category(
                    name = "Movies & TV",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Entertainment"]
                ),
                Category(
                    name = "Games",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Entertainment"]
                ),
                Category(
                    name = "Sports",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Entertainment"]
                ),
                Category(
                    name = "Hobbies",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Entertainment"]
                ),

                // Healthcare subcategories
                Category(
                    name = "Doctor",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Healthcare"]
                ),
                Category(
                    name = "Pharmacy",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Healthcare"]
                ),
                Category(
                    name = "Insurance",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Healthcare"]
                ),
                Category(
                    name = "Dental",
                    type = "expense",
                    userId = userId,
                    parentId = categoryIds["Healthcare"]
                ),

                // Salary subcategories
                Category(
                    name = "Base Salary",
                    type = "income",
                    userId = userId,
                    parentId = categoryIds["Salary"]
                ),
                Category(
                    name = "Bonus",
                    type = "income",
                    userId = userId,
                    parentId = categoryIds["Salary"]
                ),
                Category(
                    name = "Overtime",
                    type = "income",
                    userId = userId,
                    parentId = categoryIds["Salary"]
                ),

                // Investment subcategories
                Category(
                    name = "Stocks",
                    type = "income",
                    userId = userId,
                    parentId = categoryIds["Investment"]
                ),
                Category(
                    name = "Dividends",
                    type = "income",
                    userId = userId,
                    parentId = categoryIds["Investment"]
                ),
                Category(
                    name = "Real Estate",
                    type = "income",
                    userId = userId,
                    parentId = categoryIds["Investment"]
                ),
                Category(
                    name = "Interest",
                    type = "income",
                    userId = userId,
                    parentId = categoryIds["Investment"]
                )
            )

            subcategories.forEach { category ->
                categoryRepository.addCategory(category)
            }

            loadAllCategories()
            loadGroupedCategories()
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
                .fold(
                    onSuccess = {
                        loadAllCategories()
                        loadGroupedCategories()
                    },
                    onFailure = { /* Handle error */ }
                )
        }
    }

    data class CategoryGroup(
        val parent: Category,
        val subcategories: List<Category>
    )

    sealed class CategoriesState {
        object Loading : CategoriesState()
        data class Success(val categories: List<Category>) : CategoriesState()
        data class Error(val message: String) : CategoriesState()
    }

    sealed class GroupedCategoriesState {
        object Loading : GroupedCategoriesState()
        data class Success(val groups: List<CategoryGroup>) : GroupedCategoriesState()
        data class Error(val message: String) : GroupedCategoriesState()
    }
}