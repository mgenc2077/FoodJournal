@file:OptIn(ExperimentalMaterial3Api::class)

package com.mgenc.foodjournal.screen

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mgenc.foodjournal.viewmodel.EditRecipeViewModel

@Composable
fun EditRecipeScreen(
    viewModel: EditRecipeViewModel,
    recipeId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name by viewModel.name.collectAsState()
    val notes by viewModel.notes.collectAsState()

    if (recipeId != null) {
        LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recipeId != null) "Edit Recipe" else "Add Recipe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (recipeId != null) {
                        IconButton(onClick = {
                            viewModel.delete(recipeId)
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    TextButton(
                        onClick = {
                            if (viewModel.save(recipeId)) {
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
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.setName(it) },
                label = { Text("Recipe name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text("Ingredients / Notes") },
                minLines = 3,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
