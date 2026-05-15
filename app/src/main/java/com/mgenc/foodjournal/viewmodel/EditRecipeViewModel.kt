package com.mgenc.foodjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mgenc.foodjournal.FoodJournalApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditRecipeViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FoodJournalApp).database.recipeDao()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    fun setName(name: String) { _name.value = name }
    fun setNotes(notes: String) { _notes.value = notes }

    fun loadRecipe(recipeId: Long) {
        viewModelScope.launch {
            val recipe = dao.getById(recipeId) ?: return@launch
            _name.value = recipe.name
            _notes.value = recipe.notes.orEmpty()
        }
    }

    fun save(existingId: Long? = null): Boolean {
        val name = _name.value.trim()
        if (name.isBlank()) return false
        viewModelScope.launch {
            if (existingId != null) {
                val existing = dao.getById(existingId) ?: return@launch
                dao.update(existing.copy(name = name, notes = _notes.value.ifBlank { null }))
            } else {
                dao.insert(com.mgenc.foodjournal.data.Recipe(name = name, notes = _notes.value.ifBlank { null }))
            }
        }
        return true
    }

    fun delete(recipeId: Long) {
        viewModelScope.launch {
            val recipe = dao.getById(recipeId) ?: return@launch
            dao.delete(recipe)
        }
    }
}
