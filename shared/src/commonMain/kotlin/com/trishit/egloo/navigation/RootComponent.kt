package com.trishit.egloo.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// Destinations
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
sealed interface Destination {
    @Serializable data object Onboarding : Destination
    @Serializable data object Login      : Destination
    @Serializable data object SignUp     : Destination
    @Serializable data object Home       : Destination
    @Serializable data object Pingo      : Destination
    @Serializable data object Egloos     : Destination
    @Serializable data object Settings   : Destination
    @Serializable data object Saved      : Destination
}

// ─────────────────────────────────────────────────────────────────────────────
// Root component
// ─────────────────────────────────────────────────────────────────────────────

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun navigateTo(destination: Destination)
    fun onBackPressed()

    sealed class Child {
        class OnboardingChild(val component: ComponentContext) : Child()
        class LoginChild(val component: ComponentContext)      : Child()
        class SignUpChild(val component: ComponentContext)     : Child()
        class HomeChild(val component: ComponentContext)       : Child()
        class PingoChild(val component: ComponentContext)      : Child()
        class EgloosChild(val component: ComponentContext)     : Child()
        class SettingsChild(val component: ComponentContext)   : Child()
        class SavedChild(val component: ComponentContext)      : Child()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Default implementation
// ─────────────────────────────────────────────────────────────────────────────

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val isFirstLaunch: Boolean = true,
    private val isAuthenticated: Boolean = false,
    private val onOnboardingComplete: () -> Unit = {},
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Destination>()

    private val startDestination: Destination = when {
        isFirstLaunch -> Destination.Onboarding
        !isAuthenticated -> Destination.Login
        else -> Destination.Home
    }

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source         = navigation,
            serializer     = Destination.serializer(),
            initialStack   = { listOf(startDestination) },
            handleBackButton = true,
            childFactory   = ::createChild,
        )

    private fun createChild(
        destination: Destination,
        componentContext: ComponentContext,
    ): RootComponent.Child = when (destination) {
        Destination.Onboarding -> RootComponent.Child.OnboardingChild(componentContext)
        Destination.Login      -> RootComponent.Child.LoginChild(componentContext)
        Destination.SignUp     -> RootComponent.Child.SignUpChild(componentContext)
        Destination.Home       -> RootComponent.Child.HomeChild(componentContext)
        Destination.Pingo      -> RootComponent.Child.PingoChild(componentContext)
        Destination.Egloos     -> RootComponent.Child.EgloosChild(componentContext)
        Destination.Settings   -> RootComponent.Child.SettingsChild(componentContext)
        Destination.Saved      -> RootComponent.Child.SavedChild(componentContext)
    }

    override fun navigateTo(destination: Destination) {
        if (destination == Destination.Home && stack.value.active.instance is RootComponent.Child.OnboardingChild) {
            onOnboardingComplete()
        }
        navigation.bringToFront(destination)
    }

    override fun onBackPressed() {
        navigation.pop()
    }
}
