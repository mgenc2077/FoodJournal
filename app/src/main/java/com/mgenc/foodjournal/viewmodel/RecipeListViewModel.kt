package com.mgenc.foodjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.data.Recipe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class RecipeListViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FoodJournalApp).database.recipeDao()

    val recipes: StateFlow<List<Recipe>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(recipe: Recipe) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.update(recipe.copy(deletedAt = now, updatedAt = now))
        }
    }
}
