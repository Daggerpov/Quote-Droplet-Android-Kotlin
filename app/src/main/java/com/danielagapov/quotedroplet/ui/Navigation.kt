package com.danielagapov.quotedroplet.ui

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.danielagapov.quotedroplet.AppContainer
import com.danielagapov.quotedroplet.ui.author.AuthorScreen
import com.danielagapov.quotedroplet.ui.community.CommunityScreen
import com.danielagapov.quotedroplet.ui.community.SubmissionsScreen
import com.danielagapov.quotedroplet.ui.components.ScreenBackground
import com.danielagapov.quotedroplet.ui.droplets.DropletsScreen
import com.danielagapov.quotedroplet.ui.info.InfoScreen
import com.danielagapov.quotedroplet.ui.search.SearchScreen
import com.danielagapov.quotedroplet.ui.settings.SettingsScreen
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import com.danielagapov.quotedroplet.ui.top.TopScreen

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

@Composable
fun QuoteDropletNavHost() {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() },
    ) {
        composable("home") {
            HomeScreen(
                onOpenInfo = { nav.navigate("info") },
                onOpenAuthor = { author -> nav.navigate("author/${Uri.encode(author)}") },
                onOpenSubmissions = { nav.navigate("submissions") },
            )
        }
        composable("info") { InfoScreen(onBack = { nav.navigateUp() }) }
        composable("submissions") { SubmissionsScreen(onBack = { nav.navigateUp() }) }
        composable(
            route = "author/{name}",
            arguments = listOf(navArgument("name") { type = NavType.StringType }),
        ) { entry ->
            AuthorScreen(author = entry.arguments?.getString("name").orEmpty(), onBack = { nav.navigateUp() })
        }
    }
}

private enum class HomeTab(val label: String, val icon: ImageVector) {
    Droplets("Droplets", Icons.Filled.WaterDrop),
    Search("Search", Icons.Filled.Search),
    Top("Top", Icons.Filled.EmojiEvents),
    Community("Community", Icons.Filled.Groups),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
private fun HomeScreen(
    onOpenInfo: () -> Unit,
    onOpenAuthor: (String) -> Unit,
    onOpenSubmissions: () -> Unit,
) {
    val container = LocalAppContainer.current
    val palette = LocalPalette.current
    var tab by rememberSaveable { mutableStateOf(HomeTab.Droplets) }
    val openRecent by container.openRecentRequests.collectAsStateWithLifecycle()
    LaunchedEffect(openRecent) {
        if (openRecent) tab = HomeTab.Droplets
    }
    val tabStates = rememberSaveableStateHolder()

    ScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = palette.card) {
                    HomeTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = palette.accent,
                                selectedTextColor = palette.accent,
                                indicatorColor = palette.accent.copy(alpha = 0.18f),
                                unselectedIconColor = palette.text.copy(alpha = 0.6f),
                                unselectedTextColor = palette.text.copy(alpha = 0.6f),
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                tabStates.SaveableStateProvider(tab.name) {
                    when (tab) {
                        HomeTab.Droplets -> DropletsScreen(onOpenInfo = onOpenInfo, onOpenAuthor = onOpenAuthor)
                        HomeTab.Search -> SearchScreen(onOpenAuthor = onOpenAuthor)
                        HomeTab.Top -> TopScreen(onOpenInfo = onOpenInfo, onOpenAuthor = onOpenAuthor)
                        HomeTab.Community -> CommunityScreen(onOpenInfo = onOpenInfo, onOpenSubmissions = onOpenSubmissions)
                        HomeTab.Settings -> SettingsScreen(onOpenInfo = onOpenInfo)
                    }
                }
            }
        }
    }
}
