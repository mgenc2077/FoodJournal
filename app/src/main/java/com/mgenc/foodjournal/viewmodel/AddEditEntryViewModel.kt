package com.mgenc.foodjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.data.FoodEntry
import com.mgenc.foodjournal.data.MealType
import com.mgenc.foodjournal.util.uuidV7
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddEditEntryViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FoodJournalApp).database.foodEntryDao()

    private val _foodName = MutableStateFlow("")
    val foodName: StateFlow<String> = _foodName

    private val _mealType = MutableStateFlow(MealType.LUNCH.name)
    val mealType: StateFlow<String> = _mealType

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    fun setFoodName(name: String) { _foodName.value = name }
    fun setMealType(type: String) { _mealType.value = type }
    fun setNotes(notes: String) { _notes.value = notes }

    fun loadEntry(entryId: String) {
        viewModelScope.launch {
            val entry = dao.getById(entryId) ?: return@launch
            _foodName.value = entry.foodName
            _mealType.value = entry.mealType
            _notes.value = entry.notes.orEmpty()
        }
    }

    fun saveEntry(epochDay: Long, existingId: String? = null): Boolean {
        val name = _foodName.value.trim()
        if (name.isBlank()) return false
        viewModelScope.launch {
            if (existingId != null) {
                val existing = dao.getById(existingId) ?: return@launch
                dao.update(
                    existing.copy(
                        foodName = name,
                        mealType = _mealType.value,
                        notes = _notes.value.ifBlank { null },
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            } else {
                dao.insert(
                    FoodEntry(
                        id = uuidV7(),
                        epochDay = epochDay,
                        foodName = name,
                        mealType = _mealType.value,
                        notes = _notes.value.ifBlank { null },
                    )
                )
            }
        }
        return true
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            val entry = dao.getById(entryId) ?: return@launch
            dao.delete(entry)
        }
    }
}
