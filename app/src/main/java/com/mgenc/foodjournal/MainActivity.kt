@file:OptIn(ExperimentalMaterial3Api::class)

package com.mgenc.foodjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.mgenc.foodjournal.ui.theme.FoodJournalTheme
import kotlinx.serialization.Serializable

@Serializable
data object First : NavKey

@Serializable
data object Second : NavKey

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FoodJournalTheme {
                val backStack = rememberNavBackStack(First)
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Food Journal") })
                    }
                ) { innerPadding ->
                    NavDisplay(
                        backStack = backStack,
                        modifier = Modifier.padding(innerPadding),
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            entry<First> {
                                FirstScreen(
                                    onNext = { backStack.add(Second) }
                                )
                            }
                            entry<Second> {
                                SecondScreen(
                                    onPrevious = { backStack.removeLastOrNull() }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
