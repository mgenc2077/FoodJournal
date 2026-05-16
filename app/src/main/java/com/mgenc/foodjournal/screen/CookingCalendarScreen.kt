@file:OptIn(ExperimentalMaterial3Api::class)

package com.mgenc.foodjournal.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mgenc.foodjournal.data.CookingPlan
import com.mgenc.foodjournal.viewmodel.CookingCalendarViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun CookingCalendarScreen(
    viewModel: CookingCalendarViewModel,
    onBack: () -> Unit,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val plansByDay by viewModel.plansByDay.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cooking Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            MonthNavigation(
                currentMonth = currentMonth,
                onPrev = { viewModel.prevMonth() },
                onNext = { viewModel.nextMonth() },
            )
            DayOfWeekHeader()
            CalendarGrid(
                month = currentMonth,
                plansByDay = plansByDay,
                onDayClick = onDayClick,
            )
        }
    }
}

@Composable
private fun MonthNavigation(
    currentMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = currentMonth.format(monthFormatter),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        for (day in days) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    plansByDay: Map<Long, List<CookingPlan>>,
    onDayClick: (Long) -> Unit,
) {
    val firstDay = month.atDay(1)
    val startOfWeek = WeekFields.of(DayOfWeek.MONDAY, 1).dayOfWeek()
    var gridDate = firstDay.with(startOfWeek, 1)
    val lastDay = month.atEndOfMonth()
    val endOfWeek = WeekFields.of(DayOfWeek.SUNDAY, 7).dayOfWeek()
    val gridEnd = lastDay.with(endOfWeek, 7)

    val weeks = mutableListOf<List<LocalDate>>()
    var week = mutableListOf<LocalDate>()
    while (!gridDate.isAfter(gridEnd)) {
        week.add(gridDate)
        if (week.size == 7) {
            weeks.add(week)
            week = mutableListOf()
        }
        gridDate = gridDate.plusDays(1)
    }
    if (week.isNotEmpty()) {
        while (week.size < 7) {
            week.add(gridDate)
            gridDate = gridDate.plusDays(1)
        }
        weeks.add(week)
    }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        for (weekDates in weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (date in weekDates) {
                    val epochDay = date.toEpochDay()
                    val plans = plansByDay[epochDay]
                    val isCurrentMonth = date.month == month.month

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .background(
                                color = if (date == LocalDate.now()) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = MaterialTheme.shapes.small,
                            )
                            .clickable { onDayClick(epochDay) }
                            .padding(4.dp),
                    ) {
                        Column {
                            Text(
                                text = if (isCurrentMonth) date.dayOfMonth.toString() else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (date == LocalDate.now()) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            if (plans != null && plans.isNotEmpty()) {
                                Text(
                                    text = plans.first().name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (date == LocalDate.now()) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
