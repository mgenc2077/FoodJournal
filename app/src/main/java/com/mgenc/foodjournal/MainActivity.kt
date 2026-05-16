@file:OptIn(ExperimentalMaterial3Api::class)

package com.mgenc.foodjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.mgenc.foodjournal.screen.AddEditEntryScreen
import com.mgenc.foodjournal.screen.CookingCalendarScreen
import com.mgenc.foodjournal.screen.CookingPlanScreen
import com.mgenc.foodjournal.screen.DailyJournalScreen
import com.mgenc.foodjournal.screen.EditRecipeScreen
import com.mgenc.foodjournal.screen.RecipeListScreen
import com.mgenc.foodjournal.screen.ReminderScreen
import com.mgenc.foodjournal.screen.SettingsScreen
import com.mgenc.foodjournal.screen.TimelineScreen
import com.mgenc.foodjournal.ui.theme.FoodJournalTheme
import com.mgenc.foodjournal.viewmodel.AddEditCookingPlanViewModel
import com.mgenc.foodjournal.viewmodel.AddEditEntryViewModel
import com.mgenc.foodjournal.viewmodel.CookingCalendarViewModel
import com.mgenc.foodjournal.viewmodel.DailyJournalViewModel
import com.mgenc.foodjournal.viewmodel.EditRecipeViewModel
import com.mgenc.foodjournal.viewmodel.RecipeListViewModel
import com.mgenc.foodjournal.viewmodel.ReminderViewModel
import com.mgenc.foodjournal.viewmodel.SettingsViewModel
import com.mgenc.foodjournal.viewmodel.TimelineViewModel
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class DailyJournal(val epochDay: Long = LocalDate.now().toEpochDay()) : NavKey

@Serializable
data class AddEntry(val epochDay: Long) : NavKey

@Serializable
data class EditEntry(val entryId: String) : NavKey

@Serializable
data object Timeline : NavKey

@Serializable
data object Recipes : NavKey

@Serializable
data class EditRecipe(val recipeId: String? = null) : NavKey

@Serializable
data object Reminders : NavKey

@Serializable
data object CookingCalendar : NavKey

@Serializable
data class CookingPlanDay(val epochDay: Long) : NavKey

@Serializable
data class EditCookingPlan(val planId: String, val epochDay: Long) : NavKey

@Serializable
data object Settings : NavKey

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FoodJournalTheme {
                val backStack = rememberNavBackStack(DailyJournal())
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    entryProvider = entryProvider {
                        entry<DailyJournal> { key ->
                            val vm: DailyJournalViewModel = viewModel()
                            DailyJournalScreen(
                                viewModel = vm,
                                onAddEntry = { backStack.add(AddEntry(it)) },
                                onEditEntry = { backStack.add(EditEntry(it)) },
                                onTimeline = { backStack.add(Timeline) },
                                onRecipes = { backStack.add(Recipes) },
                                onReminders = { backStack.add(Reminders) },
                                onCookingCalendar = { backStack.add(CookingCalendar) },
                                onSettings = { backStack.add(Settings) },
                            )
                        }
                        entry<AddEntry> { key ->
                            val vm: AddEditEntryViewModel = viewModel()
                            AddEditEntryScreen(
                                viewModel = vm,
                                epochDay = key.epochDay,
                                entryId = null,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<EditEntry> { key ->
                            val vm: AddEditEntryViewModel = viewModel()
                            AddEditEntryScreen(
                                viewModel = vm,
                                epochDay = 0L,
                                entryId = key.entryId,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Timeline> {
                            val vm: TimelineViewModel = viewModel()
                            TimelineScreen(
                                viewModel = vm,
                                onBack = { backStack.removeLastOrNull() },
                                onEditEntry = { backStack.add(EditEntry(it)) },
                            )
                        }
                        entry<Recipes> {
                            val vm: RecipeListViewModel = viewModel()
                            RecipeListScreen(
                                viewModel = vm,
                                onBack = { backStack.removeLastOrNull() },
                                onAddRecipe = { backStack.add(EditRecipe()) },
                                onEditRecipe = { backStack.add(EditRecipe(it)) },
                            )
                        }
                        entry<EditRecipe> { key ->
                            val vm: EditRecipeViewModel = viewModel()
                            EditRecipeScreen(
                                viewModel = vm,
                                recipeId = key.recipeId,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Settings> {
                            val vm: SettingsViewModel = viewModel()
                            SettingsScreen(
                                viewModel = vm,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Reminders> {
                            val vm: ReminderViewModel = viewModel()
                            ReminderScreen(
                                viewModel = vm,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<CookingCalendar> {
                            val vm: CookingCalendarViewModel = viewModel()
                            CookingCalendarScreen(
                                viewModel = vm,
                                onBack = { backStack.removeLastOrNull() },
                                onDayClick = { backStack.add(CookingPlanDay(it)) },
                            )
                        }
                        entry<CookingPlanDay> { key ->
                            val vm: AddEditCookingPlanViewModel = viewModel()
                            CookingPlanScreen(
                                viewModel = vm,
                                epochDay = key.epochDay,
                                editingPlanId = null,
                                onBack = { backStack.removeLastOrNull() },
                                onEditPlan = { backStack.add(EditCookingPlan(it, key.epochDay)) },
                            )
                        }
                        entry<EditCookingPlan> { key ->
                            val vm: AddEditCookingPlanViewModel = viewModel()
                            CookingPlanScreen(
                                viewModel = vm,
                                epochDay = key.epochDay,
                                editingPlanId = key.planId,
                                onBack = { backStack.removeLastOrNull() },
                                onEditPlan = { },
                            )
                        }
                    },
                )
            }
        }
    }
}
