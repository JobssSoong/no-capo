package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.CardBackground
import com.example.myapplication.ui.theme.CardText
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.guitar.GuitarFretboardScreen
import com.example.myapplication.ui.guitar.audio.AudioSettings
import com.example.myapplication.ui.guitar.checkin.CheckInScreen
import com.example.myapplication.ui.guitar.chord.Tuning
import com.example.myapplication.ui.guitar.profile.ProfileScreen
import com.example.myapplication.ui.guitar.training.TrainingHubScreen
import com.example.myapplication.ui.guitar.training.model.TrainingSettings
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

sealed class MainRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object CheckIn : MainRoute("checkin", "打卡", Icons.Default.CheckCircle)
    data object Fretboard : MainRoute("fretboard", "指板", Icons.Default.MusicNote)
    data object Training : MainRoute("training", "训练", Icons.Default.FitnessCenter)
    data object Profile : MainRoute("profile", "我的", Icons.Default.Person)
}

private val mainRoutes = listOf(
    MainRoute.CheckIn,
    MainRoute.Fretboard,
    MainRoute.Training,
    MainRoute.Profile
)

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var tuning by remember { mutableStateOf(Tuning.Standard) }
    var audioSettings by remember { mutableStateOf(AudioSettings()) }
    var trainingSettings by remember { mutableStateOf(TrainingSettings()) }

    val navController = rememberNavController()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = CardBackground,
                shadowElevation = 6.dp
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    contentColor = CardText
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    mainRoutes.forEach { route ->
                        val selected = currentDestination?.hierarchy?.any { it.route == route.route } == true

                        NavigationBarItem(
                            icon = { Icon(route.icon, contentDescription = route.label) },
                            label = { Text(route.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = CardText,
                                unselectedTextColor = CardText
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainRoute.Fretboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainRoute.CheckIn.route) {
                CheckInScreen(
                    tuning = tuning,
                    audioSettings = audioSettings,
                    settings = trainingSettings,
                    onSettingsChange = { trainingSettings = it }
                )
            }
            composable(MainRoute.Fretboard.route) {
                GuitarFretboardScreen(
                    tuning = tuning,
                    audioSettings = audioSettings
                )
            }
            composable(MainRoute.Training.route) {
                TrainingHubScreen(
                    tuning = tuning,
                    audioSettings = audioSettings,
                    settings = trainingSettings,
                    onSettingsChange = { trainingSettings = it }
                )
            }
            composable(MainRoute.Profile.route) {
                ProfileScreen(
                    tuning = tuning,
                    onTuningChange = { tuning = it },
                    audioSettings = audioSettings,
                    onAudioSettingsChange = { audioSettings = it },
                    trainingSettings = trainingSettings,
                    onTrainingSettingsChange = { trainingSettings = it }
                )
            }
        }
    }
}
