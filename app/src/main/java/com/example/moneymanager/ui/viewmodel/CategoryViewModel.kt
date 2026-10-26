package com.example.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.domain.model.Category
import com.example.moneymanager.domain.usecase.category.AddCategoryUseCase
import com.example.moneymanager.domain.usecase.category.DeleteCategoryUseCase
import com.example.moneymanager.domain.usecase.category.GetAllCategoriesUseCase
import com.example.moneymanager.domain.usecase.category.GetCategoriesByTypeUseCase
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
    private val getAllCategoriesUseCase: GetAllCategoriesUseCase,
    private val getCategoriesByTypeUseCase: GetCategoriesByTypeUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _categoriesState = MutableStateFlow<CategoriesState>(CategoriesState.Loading)
    val categoriesState: StateFlow<CategoriesState> = _categoriesState.asStateFlow()

    private val _groupedCategoriesState = MutableStateFlow<GroupedCategoriesState>(GroupedCategoriesState.Loading)
    val groupedCategoriesState: StateFlow<GroupedCategoriesState> = _groupedCategoriesState.asStateFlow()

    init {
        loadAllCategories()
        loadGroupedCategories()
    }

    fun loadAllCategories() {
        viewModelScope.launch {
            _categoriesState.value = CategoriesState.Loading
            getAllCategoriesUseCase()
                .catch { e ->
                    _categoriesState.value = CategoriesState.Error(e.message ?: "Failed to load categories")
                }
                .collectLatest { categories ->
                    _categoriesState.value = CategoriesState.Success(categories)
                }
        }
    }

    /** Load only categories of the given type (used by Add/Edit Transaction screen). */
    fun loadCategoriesByType(type: String) {
        viewModelScope.launch {
            getCategoriesByTypeUseCase(type)
                .catch { e ->
                    _categoriesState.value = CategoriesState.Error(e.message ?: "Failed to load categories")
                }
                .collectLatest { categories ->
                    _categoriesState.value = CategoriesState.Success(categories)
                }
        }
    }

    fun loadGroupedCategories() {
        viewModelScope.launch {
            _groupedCategoriesState.value = GroupedCategoriesState.Loading
            getAllCategoriesUseCase()
                .catch { e ->
                    _groupedCategoriesState.value = GroupedCategoriesState.Error(e.message ?: "Failed to load categories")
                }
                .collectLatest { categories ->
                    val topLevel = categories.filter { it.parentId == null }
                    val grouped = topLevel.map { parent ->
                        CategoryGroup(parent, categories.filter { it.parentId == parent.id })
                    }
                    _groupedCategoriesState.value = GroupedCategoriesState.Success(grouped)
                }
        }
    }

    fun loadGroupedCategoriesByType(type: String) {
        viewModelScope.launch {
            _groupedCategoriesState.value = GroupedCategoriesState.Loading
            getCategoriesByTypeUseCase(type)
                .catch { e ->
                    _groupedCategoriesState.value = GroupedCategoriesState.Error(e.message ?: "Failed to load categories")
                }
                .collectLatest { categories ->
                    val topLevel = categories.filter { it.parentId == null }.distinctBy { it.name.lowercase().trim() }
                    val grouped = topLevel.map { parent ->
                        CategoryGroup(parent, categories.filter { it.parentId == parent.id })
                    }
                    _groupedCategoriesState.value = GroupedCategoriesState.Success(grouped)
                }
        }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            addCategoryUseCase(category).fold(
                onSuccess = {
                    loadAllCategories()
                    loadGroupedCategories()
                },
                onFailure = { error ->
                    _categoriesState.value = CategoriesState.Error(error.message ?: "Failed to add category")
                }
            )
        }
    }

    fun createDefaultCategories() {
        viewModelScope.launch {
            val topLevelCategories = listOf(
                Category(name = "Ăn uống", type = "expense"),
                Category(name = "Di chuyển", type = "expense"),
                Category(name = "Mua sắm", type = "expense"),
                Category(name = "Hóa đơn & Tiện ích", type = "expense"),
                Category(name = "Giải trí", type = "expense"),
                Category(name = "Sức khỏe", type = "expense"),
                Category(name = "Lương", type = "income"),
                Category(name = "Làm thêm", type = "income"),
                Category(name = "Đầu tư", type = "income"),
                Category(name = "Thu nhập khác", type = "income")
            )

            val categoryIds = mutableMapOf<String, String>()
            topLevelCategories.forEach { category ->
                addCategoryUseCase(category).fold(
                    onSuccess = { added -> categoryIds[category.name] = added.id },
                    onFailure = { /* Non-fatal */ }
                )
            }

            kotlinx.coroutines.delay(500)

            val subcategories = listOf(
                Category(name = "Nhà hàng", type = "expense", parentId = categoryIds["Ăn uống"]),
                Category(name = "Đi chợ", type = "expense", parentId = categoryIds["Ăn uống"]),
                Category(name = "Cà phê & Trà", type = "expense", parentId = categoryIds["Ăn uống"]),
                Category(name = "Đồ ăn nhanh", type = "expense", parentId = categoryIds["Ăn uống"]),
                Category(name = "Đổ xăng", type = "expense", parentId = categoryIds["Di chuyển"]),
                Category(name = "Phương tiện công cộng", type = "expense", parentId = categoryIds["Di chuyển"]),
                Category(name = "Taxi/Xe ôm", type = "expense", parentId = categoryIds["Di chuyển"]),
                Category(name = "Quần áo", type = "expense", parentId = categoryIds["Mua sắm"]),
                Category(name = "Đồ điện tử", type = "expense", parentId = categoryIds["Mua sắm"]),
                Category(name = "Tiền điện", type = "expense", parentId = categoryIds["Hóa đơn & Tiện ích"]),
                Category(name = "Tiền nước", type = "expense", parentId = categoryIds["Hóa đơn & Tiện ích"]),
                Category(name = "Internet", type = "expense", parentId = categoryIds["Hóa đơn & Tiện ích"]),
                Category(name = "Phim ảnh & TV", type = "expense", parentId = categoryIds["Giải trí"]),
                Category(name = "Trò chơi", type = "expense", parentId = categoryIds["Giải trí"]),
                Category(name = "Khám bệnh", type = "expense", parentId = categoryIds["Sức khỏe"]),
                Category(name = "Hiệu thuốc", type = "expense", parentId = categoryIds["Sức khỏe"]),
                Category(name = "Lương cơ bản", type = "income", parentId = categoryIds["Lương"]),
                Category(name = "Thưởng", type = "income", parentId = categoryIds["Lương"]),
                Category(name = "Cổ phiếu", type = "income", parentId = categoryIds["Đầu tư"]),
                Category(name = "Cổ tức", type = "income", parentId = categoryIds["Đầu tư"])
            )

            subcategories.forEach { addCategoryUseCase(it) }
            loadAllCategories()
            loadGroupedCategories()
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            deleteCategoryUseCase(categoryId).fold(
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