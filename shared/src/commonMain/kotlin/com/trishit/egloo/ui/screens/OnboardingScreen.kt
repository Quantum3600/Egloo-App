package com.trishit.egloo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.trishit.egloo.ui.theme.EglooColors
import org.jetbrains.compose.resources.painterResource
import egloo.shared.generated.resources.*

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val totalPages = 4

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 800.dp

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left pane: Illustration
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "onboarding_image",
                    ) { currentPage ->
                        val painter = when (currentPage) {
                            0 -> painterResource(Res.drawable.pingo_egg)
                            1 -> painterResource(Res.drawable.pingo_walk)
                            2 -> painterResource(Res.drawable.egloo)
                            3 -> painterResource(Res.drawable.pingo_hi)
                            else -> painterResource(Res.drawable.pingo_egg)
                        }
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.6f)
                        )
                    }
                }

                // Right pane: Content + Navigation
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 64.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            slideInVertically { it / 2 } + fadeIn() togetherWith
                                    slideOutVertically { -it / 2 } + fadeOut()
                        },
                        label = "onboarding_text",
                    ) { currentPage ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            val title = when (currentPage) {
                                0 -> "Meet Pingo"
                                1 -> "Your igloo of knowledge"
                                2 -> "Your data stays yours"
                                3 -> "Pingo is ready!"
                                else -> ""
                            }
                            val body = when (currentPage) {
                                0 -> "Your personal AI assistant who lives in an igloo and keeps your knowledge safe, organised, and always at hand."
                                1 -> "Connect Gmail, Slack, and Drive. Pingo reads everything and stores it safely in the igloo — then answers your questions in plain English."
                                2 -> "Everything lives in your igloo. Your data is encrypted and never used to train AI models. Pingo works for you, not for us."
                                3 -> "The igloo is built. Connect your first source and let Pingo get to work."
                                else -> ""
                            }

                            Text(
                                text = title,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Start
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Start
                            )

                            if (currentPage == 1) {
                                Spacer(Modifier.height(32.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HowItWorksStep("1", "Connect your tools")
                                    HowItWorksStep("2", "Pingo reads & understands")
                                    HowItWorksStep("3", "Ask anything, get answers")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(64.dp))

                    OnboardingNavigation(
                        page = page,
                        totalPages = totalPages,
                        onComplete = onComplete,
                        onNext = { page++ },
                        isWide = true
                    )
                }
            }
        } else {
            // Original layout for narrow screens
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "onboarding_mobile",
                ) { currentPage ->
                    when (currentPage) {
                        0 -> OnboardingWelcome()
                        1 -> OnboardingHowItWorks()
                        2 -> OnboardingPrivacy()
                        3 -> OnboardingReady()
                        else -> OnboardingWelcome()
                    }
                }

                OnboardingNavigation(
                    page = page,
                    totalPages = totalPages,
                    onComplete = onComplete,
                    onNext = { page++ },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 32.dp, vertical = 40.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingNavigation(
    page: Int,
    totalPages: Int,
    onComplete: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    isWide: Boolean = false
) {
    Column(
        modifier = modifier.then(if (isWide) Modifier.width(400.dp) else Modifier.fillMaxWidth()),
        horizontalAlignment = if (isWide) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Page dots
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalPages) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == page) 20.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i == page) EglooColors.TealPrimary
                            else EglooColors.TealPrimary.copy(alpha = 0.25f)
                        )
                )
            }
        }

        // CTA button
        Button(
            onClick = { if (page < totalPages - 1) onNext() else onComplete() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = if (page < totalPages - 1) "Continue" else "Let's go ❄",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        if (page == 0) {
            TextButton(
                onClick = onComplete,
                modifier = if (isWide) Modifier.padding(start = 0.dp) else Modifier
            ) {
                Text(
                    "Skip onboarding",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    painter: Painter,
    title: String,
    body: String,
    extraContent: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 40.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Pingo illustration
        Box(
            modifier = Modifier
                .size(360.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(24.dp))
        extraContent()
    }
}

@Composable
private fun OnboardingWelcome() = OnboardingPage(
    painter = painterResource(Res.drawable.pingo_egg),
    title = "Meet Pingo",
    body = "Your personal AI assistant who lives in an igloo and keeps your knowledge safe, organised, and always at hand.",
)

@Composable
private fun OnboardingHowItWorks() = OnboardingPage(
    painter = painterResource(Res.drawable.pingo_walk),
    title = "Your igloo of knowledge",
    body = "Connect Gmail, Slack, and Drive. Pingo reads everything and stores it safely in the igloo — then answers your questions in plain English.",
    extraContent = {
        // How it works steps
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HowItWorksStep("1", "Connect your tools")
            HowItWorksStep("2", "Pingo reads & understands")
            HowItWorksStep("3", "Ask anything, get answers")
        }
    }
)

@Composable
private fun OnboardingPrivacy() = OnboardingPage(
    painter = painterResource(Res.drawable.egloo),
    title = "Your data stays yours",
    body = "Everything lives in your igloo. Your data is encrypted and never used to train AI models. Pingo works for you, not for us.",
)

@Composable
private fun OnboardingReady() = OnboardingPage(
    painter = painterResource(Res.drawable.pingo_hi),
    title = "Pingo is ready!",
    body = "The igloo is built. Connect your first source and let Pingo get to work.",
)

@Composable
private fun HowItWorksStep(number: String, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(EglooColors.TealPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
        }
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}
