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
    @Serializable data object Home       : Destination
    @Serializable data object Chat       : Destination
    @Serializable data object Topics     : Destination
    @Serializable data object Sources    : Destination
    @Serializable data object Settings   : Destination
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
        class HomeChild(val component: ComponentContext)       : Child()
        class ChatChild(val component: ComponentContext)       : Child()
        class TopicsChild(val component: ComponentContext)     : Child()
        class SourcesChild(val component: ComponentContext)    : Child()
        class SettingsChild(val component: ComponentContext)   : Child()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Default implementation
// ─────────────────────────────────────────────────────────────────────────────

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val isFirstLaunch: Boolean = true,
    private val onOnboardingComplete: () -> Unit = {},
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Destination>()

    private val startDestination: Destination =
        if (isFirstLaunch) Destination.Onboarding else Destination.Home

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
        Destination.Home       -> RootComponent.Child.HomeChild(componentContext)
        Destination.Chat       -> RootComponent.Child.ChatChild(componentContext)
        Destination.Topics     -> RootComponent.Child.TopicsChild(componentContext)
        Destination.Sources    -> RootComponent.Child.SourcesChild(componentContext)
        Destination.Settings   -> RootComponent.Child.SettingsChild(componentContext)
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
