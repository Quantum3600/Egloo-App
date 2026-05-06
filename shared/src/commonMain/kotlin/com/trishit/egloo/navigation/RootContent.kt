package com.trishit.egloo.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.trishit.egloo.data.repositories.SettingsRepository
import com.trishit.egloo.ui.screens.*
import com.trishit.egloo.ui.theme.EglooTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

// ── Bottom nav items ──────────────────────────────────────────────────────────

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val destination: Destination,
)

private val navItems = listOf(
    NavItem(
        "Home",
        Icons.Default.Home,
        Icons.Default.Home,
        Destination.Home
    ),
    NavItem(
        "Chat",
        Icons.Default.Search,
        Icons.Default.Search,
        Destination.Chat
    ),
    NavItem(
        "Topics",
        Icons.AutoMirrored.Filled.List,
        Icons.AutoMirrored.Filled.List,
        Destination.Topics
    ),
    NavItem(
        "Sources",
        Icons.Default.AccountBox,
        Icons.Default.AccountBox,
        Destination.Sources
    ),
    NavItem(
        "Settings",
        Icons.Default.Settings,
        Icons.Default.Settings,
        Destination.Settings
    ),
)

// ── Root content — shared across Android, iOS, Desktop ───────────────────────

@Composable
fun RootContent(component: RootComponent) {
    AdaptiveRootContent(component)
}

@Composable
fun AdaptiveRootContent(component: RootComponent) {
    KoinContext {
        val settingsRepo = koinInject<SettingsRepository>()
        val settings by settingsRepo.getSettings().collectAsState(initial = null)
        val isDarkTheme = settings?.darkTheme ?: isSystemInDarkTheme()

        EglooTheme(darkTheme = isDarkTheme) {
            val stack by component.stack.subscribeAsState()
            val activeChild = stack.active.instance

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isDesktopLayout = maxWidth >= 600.dp

                if (activeChild is RootComponent.Child.OnboardingChild) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        OnboardingScreen(
                            onComplete = { component.navigateTo(Destination.Home) }
                        )
                    }
                } else {
                    if (isDesktopLayout) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                EglooNavRail(
                                    activeDestination = activeChild.toDestination(),
                                    onNavigate = component::navigateTo,
                                )

                                VerticalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.fillMaxHeight(),
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    Children(
                                        stack = stack,
                                        animation = stackAnimation(fade()),
                                    ) { child ->
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.background
                                        ) {
                                            when (child.instance) {
                                                is RootComponent.Child.HomeChild -> HomeScreen()
                                                is RootComponent.Child.ChatChild -> ChatScreen()
                                                is RootComponent.Child.TopicsChild -> TopicsScreen()
                                                is RootComponent.Child.SourcesChild -> SourcesScreen()
                                                is RootComponent.Child.SettingsChild -> SettingsScreen(
                                                    onRestartOnboarding = { component.navigateTo(Destination.Onboarding) }
                                                )
                                                is RootComponent.Child.OnboardingChild -> {}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Main app shell for mobile
                        Scaffold(
                            bottomBar = {
                                EglooBottomBar(
                                    activeDestination = activeChild.toDestination(),
                                    onNavigate = component::navigateTo,
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.background,
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                Children(
                                    stack = stack,
                                    animation = stackAnimation(fade() + scale()),
                                ) { child ->
                                    when (val instance = child.instance) {
                                        is RootComponent.Child.HomeChild -> HomeScreen()
                                        is RootComponent.Child.ChatChild -> ChatScreen()
                                        is RootComponent.Child.TopicsChild -> TopicsScreen()
                                        is RootComponent.Child.SourcesChild -> SourcesScreen()
                                        is RootComponent.Child.SettingsChild -> SettingsScreen(
                                            onRestartOnboarding = { component.navigateTo(Destination.Onboarding) }
                                        )
                                        is RootComponent.Child.OnboardingChild -> {} // handled above
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Bottom navigation bar ─────────────────────────────────────────────────────

@Composable
private fun EglooBottomBar(
    activeDestination: Destination?,
    onNavigate: (Destination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        navItems.forEach { item ->
            val isSelected = activeDestination == item.destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

// ── Desktop nav rail (used by desktopApp instead of bottom bar) ───────────────

@Composable
fun EglooNavRail(
    activeDestination: Destination?,
    onNavigate: (Destination) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Spacer(Modifier.height(16.dp))
            // Pingo wordmark
            Text(
                "eg",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
        }
    ) {
        Spacer(Modifier.weight(1f))
        navItems.forEach { item ->
            val isSelected = activeDestination == item.destination
            NavigationRailItem(
                selected = isSelected,
                onClick = { onNavigate(item.destination) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

// ── Removed DesktopRootContent redundant implementation ──────────────────────────

// ── Helper extension ──────────────────────────────────────────────────────────

private fun RootComponent.Child.toDestination(): Destination? =
    when (this) {
        is RootComponent.Child.HomeChild -> Destination.Home
        is RootComponent.Child.ChatChild -> Destination.Chat
        is RootComponent.Child.TopicsChild -> Destination.Topics
        is RootComponent.Child.SourcesChild -> Destination.Sources
        is RootComponent.Child.SettingsChild -> Destination.Settings
        is RootComponent.Child.OnboardingChild -> null
    }
