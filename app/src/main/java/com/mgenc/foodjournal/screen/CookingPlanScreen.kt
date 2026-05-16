@file:OptIn(ExperimentalMaterial3Api::class)

package com.mgenc.foodjournal.screen

import android.app.Application
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.data.CookingPlan
import com.mgenc.foodjournal.data.Recipe
import com.mgenc.foodjournal.viewmodel.AddEditCookingPlanViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private class PlanRecipeDropdownViewModel(app: Application) : AndroidViewModel(app) {
    val recipes: StateFlow<List<Recipe>> =
        (app as FoodJournalApp).database.recipeDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

@Composable
fun CookingPlanScreen(
    viewModel: AddEditCookingPlanViewModel,
    epochDay: Long,
    editingPlanId: String?,
    onBack: () -> Unit,
    onEditPlan: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name by viewModel.name.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val plans by viewModel.plansForDay(epochDay).collectAsState()
    val isEdit = editingPlanId != null

    val context = LocalContext.current
    val recipeVm = remember { PlanRecipeDropdownViewModel(context.applicationContext as Application) }
    val recipes by recipeVm.recipes.collectAsState()
    var recipeExpanded by remember { mutableStateOf(false) }

    if (editingPlanId != null) {
        LaunchedEffect(editingPlanId) { viewModel.loadPlan(editingPlanId) }
    } else {
        LaunchedEffect(Unit) { viewModel.resetForm() }
    }

    val date = LocalDate.ofEpochDay(epochDay)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(date.format(dateFormatter)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEdit) {
                        IconButton(onClick = {
                            viewModel.deletePlan(editingPlanId)
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    TextButton(
                        onClick = {
                            if (viewModel.savePlan(epochDay, editingPlanId)) {
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
                .verticalScroll(rememberScrollState()),
        ) {
            if (plans.isNotEmpty() && !isEdit) {
                Text(
                    text = "Plans for this day",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
                for (plan in plans) {
                    PlanRow(
                        plan = plan,
                        onClick = { onEditPlan(plan.id) },
                        onDelete = { viewModel.deletePlan(plan.id) },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (recipes.isNotEmpty() && !isEdit) {
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
                                        viewModel.setName(recipe.name)
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
                    value = name,
                    onValueChange = { viewModel.setName(it) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
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
}

@Composable
private fun PlanRow(
    plan: CookingPlan,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!plan.notes.isNullOrBlank()) {
                    Text(
                        text = plan.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
