package com.mgenc.foodjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.data.FoodEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DailyJournalViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FoodJournalApp).database.foodEntryDao()

    private val _currentDay = MutableStateFlow(LocalDate.now())
    val currentDay: StateFlow<LocalDate> = _currentDay

    val entries: StateFlow<List<FoodEntry>> = _currentDay
        .combine(dao.getAll()) { day, all -> all.filter { it.epochDay == day.toEpochDay() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun prevDay() {
        _currentDay.value = _currentDay.value.minusDays(1)
    }

    fun nextDay() {
        _currentDay.value = _currentDay.value.plusDays(1)
    }

    fun goToDay(day: LocalDate) {
        _currentDay.value = day
    }

    fun deleteEntry(entry: FoodEntry) {
        viewModelScope.launch { dao.delete(entry) }
    }
}
