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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.trishit.egloo.data.repositories.SettingsRepository
import com.trishit.egloo.domain.viewmodels.IngestViewModel
import com.trishit.egloo.domain.viewmodels.SettingsViewModel
import com.trishit.egloo.ui.screens.*
import com.trishit.egloo.ui.theme.EglooTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import egloo.shared.generated.resources.*
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

// ── Bottom nav items ──────────────────────────────────────────────────────────

private data class NavItem(
    val label: String,
    val iconRes: DrawableResource,
    val destination: Destination,
)

@Composable
private fun getNavItems() = listOf(
    NavItem(
        "Home",
        Res.drawable.house_chimney_solid_full,
        Destination.Home
    ),
    NavItem(
        "Pingo",
        Res.drawable.pingo_tab_icon,
        Destination.Pingo
    ),
    NavItem(
        "Egloos",
        Res.drawable.egloo_tab_icon,
        Destination.Egloos
    ),
    NavItem(
        "Saved",
        Res.drawable.bookmark_svgrepo_com,
        Destination.Saved
    ),
    NavItem(
        "Settings",
        Res.drawable.gear_svgrepo_com,
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
        
        val ingestViewModel = koinInject<IngestViewModel>()
        val ingestState by ingestViewModel.uiState.collectAsState()

        EglooTheme(darkTheme = isDarkTheme) {
            val stack by component.stack.subscribeAsState()
            val activeChild = stack.active.instance

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isDesktopLayout = maxWidth >= 600.dp
                val isAuthOrOnboarding = activeChild is RootComponent.Child.OnboardingChild || 
                                        activeChild is RootComponent.Child.LoginChild || 
                                        activeChild is RootComponent.Child.SignUpChild

                Column(modifier = Modifier.fillMaxSize()) {
                    // Global Ingest Progress Bar
                    if (ingestState.activeJobs.isNotEmpty() && !isAuthOrOnboarding) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (isAuthOrOnboarding) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background,
                            ) {
                                when (activeChild) {
                                    is RootComponent.Child.OnboardingChild -> {
                                        OnboardingScreen(
                                            onComplete = { component.navigateTo(Destination.Login) }
                                        )
                                    }
                                    is RootComponent.Child.LoginChild -> {
                                        LoginScreen(
                                            onLoginSuccess = { component.navigateTo(Destination.Home) },
                                            onNavigateToSignUp = { component.navigateTo(Destination.SignUp) }
                                        )
                                    }
                                    is RootComponent.Child.SignUpChild -> {
                                        SignUpScreen(
                                            onSignUpSuccess = { component.navigateTo(Destination.Login) },
                                            onNavigateToLogin = { component.navigateTo(Destination.Login) }
                                        )
                                    }
                                    else -> {}
                                }
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
                                                    when (val instance = child.instance) {
                                                        is RootComponent.Child.HomeChild -> HomeScreen()
                                                        is RootComponent.Child.PingoChild -> PingoScreen()
                                                        is RootComponent.Child.EgloosChild -> EgloosScreen(
                                                            onNavigateToPdfUpload = { component.navigateTo(Destination.PdfUpload) }
                                                        )
                                                        is RootComponent.Child.SavedChild -> SavedItemsScreen()
                                                        is RootComponent.Child.PdfUploadChild -> PdfScreen(onBack = { component.onBackPressed() })
                                                        is RootComponent.Child.SettingsChild -> SettingsScreen(
                                                            onRestartOnboarding = { component.navigateTo(Destination.Onboarding) }
                                                        )
                                                        else -> {}
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
                                                is RootComponent.Child.PingoChild -> PingoScreen()
                                                is RootComponent.Child.EgloosChild -> EgloosScreen(
                                                    onNavigateToPdfUpload = { component.navigateTo(Destination.PdfUpload) }
                                                )
                                                is RootComponent.Child.SavedChild -> SavedItemsScreen()
                                                is RootComponent.Child.PdfUploadChild -> PdfScreen(onBack = { component.onBackPressed() })
                                                is RootComponent.Child.SettingsChild -> SettingsScreen(
                                                    onRestartOnboarding = { component.navigateTo(Destination.Onboarding) }
                                                )
                                                else -> {}
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
        getNavItems().forEach { item ->
            val isSelected = activeDestination == item.destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
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
        getNavItems().forEach { item ->
            val isSelected = activeDestination == item.destination
            NavigationRailItem(
                selected = isSelected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
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

// ── Helper extension ──────────────────────────────────────────────────────────

private fun RootComponent.Child.toDestination(): Destination? =
    when (this) {
        is RootComponent.Child.HomeChild -> Destination.Home
        is RootComponent.Child.PingoChild -> Destination.Pingo
        is RootComponent.Child.EgloosChild -> Destination.Egloos
        is RootComponent.Child.SavedChild -> Destination.Saved
        is RootComponent.Child.SettingsChild -> Destination.Settings
        else -> null
    }
