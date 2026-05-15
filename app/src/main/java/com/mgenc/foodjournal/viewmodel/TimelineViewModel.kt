package com.mgenc.foodjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.data.FoodEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TimelineViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as FoodJournalApp).database.foodEntryDao()

    val entries: StateFlow<List<FoodEntry>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
