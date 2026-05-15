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
import com.mgenc.foodjournal.screen.DailyJournalScreen
import com.mgenc.foodjournal.screen.EditRecipeScreen
import com.mgenc.foodjournal.screen.RecipeListScreen
import com.mgenc.foodjournal.screen.TimelineScreen
import com.mgenc.foodjournal.ui.theme.FoodJournalTheme
import com.mgenc.foodjournal.viewmodel.AddEditEntryViewModel
import com.mgenc.foodjournal.viewmodel.DailyJournalViewModel
import com.mgenc.foodjournal.viewmodel.EditRecipeViewModel
import com.mgenc.foodjournal.viewmodel.RecipeListViewModel
import com.mgenc.foodjournal.viewmodel.TimelineViewModel
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class DailyJournal(val epochDay: Long = LocalDate.now().toEpochDay()) : NavKey

@Serializable
data class AddEntry(val epochDay: Long) : NavKey

@Serializable
data class EditEntry(val entryId: Long) : NavKey

@Serializable
data object Timeline : NavKey

@Serializable
data object Recipes : NavKey

@Serializable
data class EditRecipe(val recipeId: Long? = null) : NavKey

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
                    },
                )
            }
        }
    }
}
