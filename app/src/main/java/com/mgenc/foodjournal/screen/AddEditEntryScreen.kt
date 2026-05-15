@file:OptIn(ExperimentalMaterial3Api::class)

package com.mgenc.foodjournal.screen

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.data.MealType
import com.mgenc.foodjournal.data.Recipe
import com.mgenc.foodjournal.viewmodel.AddEditEntryViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

private class RecipeDropdownViewModel(app: Application) : AndroidViewModel(app) {
    val recipes: StateFlow<List<Recipe>> =
        (app as FoodJournalApp).database.recipeDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun AddEditEntryScreen(
    viewModel: AddEditEntryViewModel,
    epochDay: Long,
    entryId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val foodName by viewModel.foodName.collectAsState()
    val mealType by viewModel.mealType.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val isEdit = entryId != null

    val context = LocalContext.current
    val recipeVm = remember { RecipeDropdownViewModel(context.applicationContext as Application) }
    val recipes by recipeVm.recipes.collectAsState()
    var recipeExpanded by remember { mutableStateOf(false) }

    if (entryId != null) {
        LaunchedEffect(entryId) { viewModel.loadEntry(entryId) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Entry" else "Add Entry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (entryId != null) {
                        IconButton(onClick = {
                            viewModel.deleteEntry(entryId)
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    TextButton(
                        onClick = {
                            if (viewModel.saveEntry(epochDay, entryId)) {
                                onBack()
                            }
                        },
                    ) {
                        Text("Save")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (recipes.isNotEmpty() && entryId == null) {
                ExposedDropdownMenuBox(
                    expanded = recipeExpanded,
                    onExpandedChange = { recipeExpanded = it },
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Use recipe (optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recipeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = recipeExpanded,
                        onDismissRequest = { recipeExpanded = false },
                    ) {
                        for (recipe in recipes) {
                            DropdownMenuItem(
                                text = { Text(recipe.name) },
                                onClick = {
                                    viewModel.setFoodName(recipe.name)
                                    if (!recipe.notes.isNullOrBlank()) {
                                        viewModel.setNotes(recipe.notes)
                                    }
                                    recipeExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            OutlinedTextField(
                value = foodName,
                onValueChange = { viewModel.setFoodName(it) },
                label = { Text("Food name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Meal", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (type in MealType.entries) {
                    FilterChip(
                        selected = mealType == type.name,
                        onClick = { viewModel.setMealType(type.name) },
                        label = {
                            Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text("Notes (optional)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
