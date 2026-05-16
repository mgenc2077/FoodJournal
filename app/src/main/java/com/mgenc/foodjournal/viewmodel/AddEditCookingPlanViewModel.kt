package com.mgenc.foodjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.data.CookingPlan
import com.mgenc.foodjournal.util.uuidV7
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddEditCookingPlanViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FoodJournalApp).database.cookingPlanDao()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes

    fun setName(name: String) { _name.value = name }
    fun setNotes(notes: String) { _notes.value = notes }

    fun loadPlan(planId: String) {
        viewModelScope.launch {
            val plan = dao.getById(planId) ?: return@launch
            _name.value = plan.name
            _notes.value = plan.notes.orEmpty()
        }
    }

    fun plansForDay(epochDay: Long): StateFlow<List<CookingPlan>> {
        return dao.getByDate(epochDay)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun savePlan(epochDay: Long, existingId: String? = null): Boolean {
        val name = _name.value.trim()
        if (name.isBlank()) return false
        viewModelScope.launch {
            if (existingId != null) {
                val existing = dao.getById(existingId) ?: return@launch
                dao.update(
                    existing.copy(
                        name = name,
                        notes = _notes.value.ifBlank { null },
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            } else {
                dao.insert(
                    CookingPlan(
                        id = uuidV7(),
                        epochDay = epochDay,
                        name = name,
                        notes = _notes.value.ifBlank { null },
                    )
                )
            }
        }
        return true
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            val plan = dao.getById(planId) ?: return@launch
            val now = System.currentTimeMillis()
            dao.update(plan.copy(deletedAt = now, updatedAt = now))
        }
    }

    fun resetForm() {
        _name.value = ""
        _notes.value = ""
    }
}
