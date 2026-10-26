package com.example.moneymanager.ui.screens.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.res.stringResource
import com.example.moneymanager.R
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.data.model.Category
import com.example.moneymanager.ui.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val groupedCategoriesState by categoryViewModel.groupedCategoriesState.collectAsState()
    var selectedType by remember { mutableStateOf("expense") } // all, income, expense
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedParentCategory by remember { mutableStateOf<Category?>(null) }

    // Apply filter when it changes
    LaunchedEffect(selectedType) {
        categoryViewModel.loadGroupedCategoriesByType(selectedType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {Text(stringResource(R.string.categories))},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedParentCategory = null //reset parent
                    showAddCategoryDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == "expense",
                    onClick = { selectedType = "expense" },
                    label = { Text(stringResource(R.string.expenses))},
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedType == "income",
                    onClick = { selectedType = "income" },
                    label = {Text(stringResource(R.string.incomes))},
                    modifier = Modifier.weight(1f)
                )
            }

            when(groupedCategoriesState) {
                is CategoryViewModel.GroupedCategoriesState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is CategoryViewModel.GroupedCategoriesState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (groupedCategoriesState as CategoryViewModel.GroupedCategoriesState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { categoryViewModel.loadGroupedCategoriesByType(selectedType)}
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
                is CategoryViewModel.GroupedCategoriesState.Success -> {
                    val groups = (groupedCategoriesState as CategoryViewModel.GroupedCategoriesState.Success).groups
                        .distinctBy { it.parent.id }
                    if (groups.isEmpty()) {
                        EmptyCategoriesView(
                            onCreateDefaults = { categoryViewModel.createDefaultCategories() },
                            onAddNew = {
                                selectedParentCategory = null
                                showAddCategoryDialog = true
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(groups, key = {it.parent.id}) { group ->
                                CategoryGroupCard(
                                    group = group,
                                    onAddSubCategory = {parent ->
                                        selectedParentCategory = parent
                                        showAddCategoryDialog = true
                                    },
                                    onDeleteCategory = {categoryId ->
                                        categoryViewModel.deleteCategory(categoryId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            parentCategory = selectedParentCategory,
            onDismiss = {
                showAddCategoryDialog = false
                selectedParentCategory = null
            },
            onAddCategory = { name, type, icon, colorHex, parentId ->
                categoryViewModel.addCategory(
                    Category(
                        name = name,
                        type = type,
                        iconName = icon.takeIf { it.isNotBlank() },
                        colorHex = colorHex.takeIf { it.isNotBlank() },
                        parentId = parentId
                    )
                )
                showAddCategoryDialog = false
                selectedParentCategory = null
            }
        )
    }
}

@Composable
fun CategoryGroupCard(
    group: CategoryViewModel.CategoryGroup,
    onAddSubCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Pair<String, String>?>(null) } //id, name

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            //parent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val groupColorHex = group.parent.colorHex ?: ""
                    val groupColor = if (groupColorHex.isNotBlank()) {
                        try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(groupColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                    } else MaterialTheme.colorScheme.primary

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = groupColor.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = group.parent.iconName ?: group.parent.name.first().toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = groupColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = group.parent.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.subcategories_count, group.subcategories.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    //add subcate
                    IconButton(
                        onClick = {onAddSubCategory(group.parent)}
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add subcategory",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    //delete parent
                    IconButton(
                        onClick = {
                            categoryToDelete = group.parent.id to group.parent.name
                            showDeleteDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete category",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            //subcategories
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (group.subcategories.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.no_subcategories_yet),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        group.subcategories.forEach { subcategory ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val subColorHex = subcategory.colorHex ?: ""
                                            val subColor = if (subColorHex.isNotBlank()) {
                                                try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(subColorHex)) } catch(e: Exception) { MaterialTheme.colorScheme.primary }
                                            } else MaterialTheme.colorScheme.primary
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(
                                                        color = subColor.copy(alpha = 0.15f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (subcategory.iconName != null) {
                                                    Text(text = subcategory.iconName, fontSize = 12.sp)
                                                } else {
                                                    Box(modifier = Modifier.size(8.dp).background(subColor, CircleShape))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = subcategory.name,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                categoryToDelete = subcategory.id to subcategory.name
                                                showDeleteDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete subcategory",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showDeleteDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                categoryToDelete = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {Text(stringResource(R.string.delete_category))},
            text = {
                Text(stringResource(R.string.delete_category_confirm, categoryToDelete?.second ?: ""))
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.first?.let { onDeleteCategory(it) }
                        showDeleteDialog = false
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    categoryToDelete = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun EmptyCategoriesView(
    onCreateDefaults: () -> Unit,
    onAddNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    )
    {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_categories_yet),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_categories_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateDefaults,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.create_default_categories))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onAddNew,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.add_custom_category))
        }
    }
}


@Composable
fun AddCategoryDialog(
    parentCategory: Category? = null,
    onDismiss: () -> Unit,
    onAddCategory: (name: String, type: String, icon: String, colorHex: String, parentId: String?) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var categoryType by remember { mutableStateOf(parentCategory?.type ?: "expense") }
    var categoryIcon by remember { mutableStateOf("") }
    var categoryColor by remember { mutableStateOf("") }

    val predefinedColors = listOf(
        "#00796B", "#7F3DFF", "#2196F3", "#E91E63", "#FF9800", "#4CAF50"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (parentCategory != null)
                    stringResource(R.string.add_subcategory)
                    else stringResource(R.string.add_new_category),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (parentCategory != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.parent_category),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = parentCategory.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = categoryIcon,
                        onValueChange = { if (it.length <= 2) categoryIcon = it },
                        label = { Text(stringResource(R.string.icon)) },
                        placeholder = { Text("🍔") },
                        modifier = Modifier.weight(0.3f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        placeholder = {
                            Text(
                                if (parentCategory != null) stringResource(R.string.eg_electricity)
                                else stringResource(R.string.eg_utilities)
                            )
                        },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.pick_a_color), style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    predefinedColors.forEach { hex ->
                        val color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { categoryColor = hex }
                                .let { if (categoryColor == hex) it.background(color.copy(alpha = 0.5f), CircleShape).padding(4.dp) else it }
                        ) {
                            if (categoryColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.Center).size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (parentCategory == null) {
                    Text(
                        text = stringResource(R.string.category_type),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { categoryType = "expense" },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = categoryType == "expense",
                                onClick = {categoryType = "expense"}
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.expense),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { categoryType = "income" },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = categoryType == "income",
                                onClick = {categoryType = "income"}
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.income),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.this_will_be_subcategory, parentCategory.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (categoryName.isNotBlank()) {
                                onAddCategory(categoryName, categoryType, categoryIcon, categoryColor, parentCategory?.id)
                            }
                        },
                        enabled = categoryName.isNotBlank()
                    ) {
                        Text(stringResource(R.string.add))
                    }
                }
            }
        }
    }
}