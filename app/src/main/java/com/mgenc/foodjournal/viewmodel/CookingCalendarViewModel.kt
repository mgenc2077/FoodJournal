package com.mgenc.foodjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.data.CookingPlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CookingCalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FoodJournalApp).database.cookingPlanDao()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    val plansByDay: StateFlow<Map<Long, List<CookingPlan>>> = _currentMonth
        .flatMapLatest { month ->
            val start = month.atDay(1).with(DayOfWeek.MONDAY)
            val end = month.atEndOfMonth().with(DayOfWeek.SUNDAY)
            dao.getAllActive().combine(MutableStateFlow(Unit)) { plans, _ ->
                val startDay = start.toEpochDay()
                val endDay = end.toEpochDay()
                plans.filter { it.epochDay in startDay..endDay }
                    .groupBy { it.epochDay }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun prevMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun goToToday() {
        _currentMonth.value = YearMonth.now()
    }
}
